"""Explainable anomaly detector.

Design:
1. Deterministic rule signals for explicit regulatory/enrolment conditions.
2. Robust z-scores against a fixed synthetic baseline.
3. Optional IsolationForest ensemble component fitted on the same baseline.

The score is a bounded investigation-support signal (0..1). It is not an
authoritative verification, compliance finding, or fraud determination.
"""

from __future__ import annotations

import logging

import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler

from app.schemas.anomaly import AnomalyRecord, AnomalyResult, Severity
from app.services.explainability import (
    MULTIVARIATE_OUTLIER_SCORE,
    REASON_FEATURE,
    REASON_ORDER,
    STATISTICAL_OUTLIER_Z,
    build_explanations,
)
from app.services.features import (
    CGPA_LOWER_BOUND,
    CGPA_UPPER_BOUND,
    MODEL_FEATURES,
    SUBMISSION_DELAY_REVIEW_DAYS,
    ZSCORE_FEATURES,
    ZSCORE_SCALE_FLOORS,
    engineer_features,
    generate_synthetic_baseline,
)
from app.services.preprocessing import build_feature_frame

logger = logging.getLogger(__name__)

RULE_WEIGHTS: dict[str, float] = {
    "EXCESS_INTAKE": 0.45,
    "CATEGORY_CAPACITY_EXCEEDED": 0.35,
    "APAAR_FORMAT_INVALID": 0.30,
    "APAAR_UNVERIFIED": 0.12,
    "DUPLICATE_ENROLMENT_SIGNAL": 0.35,
    "LOW_CREDIT_PROGRESS": 0.25,
    "CGPA_OUTLIER": 0.20,
    "SUBMISSION_DELAY": 0.20,
}

RULE_ENSEMBLE_WEIGHT = 0.55
ZSCORE_ENSEMBLE_WEIGHT = 0.25
IFOREST_ENSEMBLE_WEIGHT = 0.20


class AnomalyDetector:
    """Stateless-after-fit detector with a deterministic synthetic baseline."""

    def __init__(self, model_version: str, threshold: float) -> None:
        self.model_version = model_version
        self.threshold = float(threshold)

        baseline = generate_synthetic_baseline()
        baseline_features = engineer_features(baseline)

        self._z_center: dict[str, float] = {}
        self._z_scale: dict[str, float] = {}
        for column in ZSCORE_FEATURES:
            values = baseline_features[column].to_numpy(dtype=float)
            median = float(np.median(values))
            mad = float(np.median(np.abs(values - median)))
            robust_scale = 1.4826 * mad
            self._z_center[column] = median
            self._z_scale[column] = max(robust_scale, ZSCORE_SCALE_FLOORS[column])

        model_matrix = baseline_features[list(MODEL_FEATURES)].to_numpy(dtype=float)
        self._scaler = StandardScaler().fit(model_matrix)
        scaled = self._scaler.transform(model_matrix)
        self._iforest = IsolationForest(
            n_estimators=200,
            contamination=0.05,
            max_samples="auto",
            random_state=42,
            n_jobs=1,
        ).fit(scaled)

        train_iforest_scores = -self._iforest.score_samples(scaled)
        self._iforest_center = float(np.median(train_iforest_scores))
        p95 = float(np.quantile(train_iforest_scores, 0.95))
        self._iforest_span = max(p95 - self._iforest_center, 1e-6)

        logger.info(
            "detector fitted baseline_rows=%d model_version=%s threshold=%.3f",
            len(baseline_features),
            self.model_version,
            self.threshold,
        )

    def _robust_z(self, features: pd.DataFrame) -> pd.Series:
        """Directional robust z-scores (positive means more anomalous)."""

        z_frame = pd.DataFrame(index=features.index)
        for column in ZSCORE_FEATURES:
            values = features[column].astype(float)
            center = self._z_center[column]
            scale = self._z_scale[column]
            if column == "cgpa":
                z_frame[column] = (values - center).abs() / scale
            else:
                z_frame[column] = ((values - center) / scale).clip(lower=0.0)
        z_frame = z_frame.clip(lower=0.0, upper=10.0)
        return z_frame.max(axis=1)

    def _isolation_component(self, features: pd.DataFrame) -> pd.Series:
        matrix = features[list(MODEL_FEATURES)].to_numpy(dtype=float)
        raw = -self._iforest.score_samples(self._scaler.transform(matrix))
        component = (raw - self._iforest_center) / self._iforest_span
        return pd.Series(np.clip(component, 0.0, 1.0), index=features.index)

    @staticmethod
    def _rule_contributions(
        raw: pd.Series, features: pd.Series
    ) -> dict[str, float]:
        contributions: dict[str, float] = {}

        excess = float(features["excess_intake_ratio"])
        if excess > 0.0:
            contributions["EXCESS_INTAKE"] = RULE_WEIGHTS["EXCESS_INTAKE"] * min(
                excess / 0.25, 1.0
            )

        category_excess = float(features["category_excess_ratio"])
        if category_excess > 0.0:
            contributions["CATEGORY_CAPACITY_EXCEEDED"] = RULE_WEIGHTS[
                "CATEGORY_CAPACITY_EXCEEDED"
            ] * min(category_excess / 0.25, 1.0)

        if not bool(raw["apaar_format_valid"]):
            contributions["APAAR_FORMAT_INVALID"] = RULE_WEIGHTS[
                "APAAR_FORMAT_INVALID"
            ]

        if not bool(raw["apaar_verified"]):
            contributions["APAAR_UNVERIFIED"] = RULE_WEIGHTS["APAAR_UNVERIFIED"]

        duplicate_count = int(raw["duplicate_enrolment_count"])
        if duplicate_count > 0:
            contributions["DUPLICATE_ENROLMENT_SIGNAL"] = RULE_WEIGHTS[
                "DUPLICATE_ENROLMENT_SIGNAL"
            ] * min(duplicate_count / 3.0, 1.0)

        credit_shortfall = float(features["credit_shortfall"])
        if credit_shortfall > 0.0:
            contributions["LOW_CREDIT_PROGRESS"] = RULE_WEIGHTS[
                "LOW_CREDIT_PROGRESS"
            ] * min(credit_shortfall / 0.35, 1.0)

        cgpa = float(raw["cgpa"])
        if cgpa < CGPA_LOWER_BOUND or cgpa > CGPA_UPPER_BOUND:
            contributions["CGPA_OUTLIER"] = RULE_WEIGHTS["CGPA_OUTLIER"] * min(
                float(features["cgpa_abs_deviation"]) / 3.0, 1.0
            )

        delay = int(raw["institution_submission_delay_days"])
        if delay > SUBMISSION_DELAY_REVIEW_DAYS:
            contributions["SUBMISSION_DELAY"] = RULE_WEIGHTS["SUBMISSION_DELAY"] * min(
                delay / 60.0, 1.0
            )

        return contributions

    @staticmethod
    def _severity(score: float, is_anomaly: bool) -> Severity:
        if not is_anomaly:
            return Severity.LOW
        if score >= 0.85:
            return Severity.CRITICAL
        if score >= 0.70:
            return Severity.HIGH
        return Severity.MEDIUM

    def score_records(self, records: list[AnomalyRecord]) -> list[AnomalyResult]:
        """Score a non-empty batch of validated records."""

        raw_frame, features = build_feature_frame(records)
        max_z_series = self._robust_z(features)
        isolation_series = self._isolation_component(features)

        results: list[AnomalyResult] = []
        for position, record in enumerate(records):
            raw = raw_frame.iloc[position]
            feature_row = features.iloc[position]
            max_z = float(max_z_series.iloc[position])
            isolation_score = float(isolation_series.iloc[position])

            z_component = float(
                np.clip((max_z - 1.5) / 4.0, 0.0, 1.0)
            )

            rule_contributions = self._rule_contributions(raw, feature_row)
            rule_total = sum(rule_contributions.values())

            ensemble_raw = (
                RULE_ENSEMBLE_WEIGHT * rule_total
                + ZSCORE_ENSEMBLE_WEIGHT * z_component
                + IFOREST_ENSEMBLE_WEIGHT * isolation_score
            )
            score = float(min(1.0, max(0.0, ensemble_raw)))
            scale = (score / ensemble_raw) if ensemble_raw > 0.0 else 0.0

            contributions: dict[str, float] = {}
            for code, value in rule_contributions.items():
                contributions[code] = value * RULE_ENSEMBLE_WEIGHT * scale

            if max_z >= STATISTICAL_OUTLIER_Z:
                contributions["STATISTICAL_OUTLIER"] = (
                    ZSCORE_ENSEMBLE_WEIGHT * z_component * scale
                )
            if isolation_score >= MULTIVARIATE_OUTLIER_SCORE:
                contributions["MULTIVARIATE_OUTLIER"] = (
                    IFOREST_ENSEMBLE_WEIGHT * isolation_score * scale
                )

            contributions = {
                code: round(min(1.0, max(0.0, value)), 4)
                for code, value in contributions.items()
            }
            reason_codes = [code for code in REASON_ORDER if code in contributions]
            explanations = build_explanations(record, feature_row, contributions)
            is_anomaly = score >= self.threshold

            feature_contributions = {
                REASON_FEATURE[code]: value
                for code, value in contributions.items()
                if value > 0.0
            }

            results.append(
                AnomalyResult(
                    record_id=record.record_id,
                    anomaly_score=round(score, 4),
                    is_anomaly=is_anomaly,
                    severity=self._severity(score, is_anomaly),
                    reason_codes=reason_codes,
                    explanations=explanations,
                    feature_contributions=feature_contributions,
                    requires_human_review=is_anomaly,
                )
            )

        return results
