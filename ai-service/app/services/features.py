"""Deterministic feature engineering for regulatory anomaly signals.

All inputs are synthetic or submitted values; no external data source is used.
The engineered frame is used by the detector, the feature-preview endpoint and
the synthetic evaluation tooling.
"""

from __future__ import annotations

import numpy as np
import pandas as pd

BASELINE_SEED = 20_240_517
BASELINE_ROWS = 600

CREDIT_PROGRESS_FLOOR = 0.60
CGPA_LOWER_BOUND = 4.0
CGPA_UPPER_BOUND = 9.8
SUBMISSION_DELAY_REVIEW_DAYS = 14

# Numeric features consumed by the IsolationForest component.
MODEL_FEATURES: tuple[str, ...] = (
    "excess_intake_ratio",
    "category_excess_ratio",
    "duplicate_enrolment_count",
    "credit_shortfall",
    "cgpa",
    "submission_delay_scaled",
    "apaar_format_invalid",
    "apaar_unverified",
    "days_since_admission_scaled",
)

# Features compared with the synthetic baseline using a robust z-score.
ZSCORE_FEATURES: tuple[str, ...] = (
    "excess_intake_ratio",
    "category_excess_ratio",
    "duplicate_enrolment_count",
    "credit_shortfall",
    "cgpa",
    "submission_delay_scaled",
)

# Floor prevents division by a near-zero MAD on sparse integer features.
ZSCORE_SCALE_FLOORS: dict[str, float] = {
    "excess_intake_ratio": 0.05,
    "category_excess_ratio": 0.05,
    "duplicate_enrolment_count": 0.5,
    "credit_shortfall": 0.05,
    "cgpa": 0.5,
    "submission_delay_scaled": 0.15,
}

# Canonical response order, independent of contribution magnitude.
FEATURE_COLUMNS: tuple[str, ...] = (
    "excess_intake_ratio",
    "intake_utilisation_ratio",
    "excess_intake_flag",
    "category_excess_ratio",
    "category_utilisation_ratio",
    "category_capacity_exceeded_flag",
    "category_share_of_enrolment",
    "apaar_format_invalid",
    "apaar_unverified",
    "duplicate_enrolment_count",
    "duplicate_enrolment_flag",
    "credit_shortfall",
    "low_credit_progress_flag",
    "cgpa",
    "cgpa_abs_deviation",
    "cgpa_outlier_flag",
    "submission_delay_scaled",
    "submission_delay_flag",
    "days_since_admission_scaled",
    "enrolment_gap",
)


def engineer_features(frame: pd.DataFrame) -> pd.DataFrame:
    """Return the engineered numeric feature frame for raw record rows.

    The function is vectorised, deterministic and side-effect free. Division
    denominators are floored at 1 so empty/zero capacities cannot produce NaN
    or infinity.
    """

    approved = frame["approved_intake"].astype(float).clip(lower=1.0)
    actual = frame["actual_enrolment"].astype(float)
    category_enrolment = frame["category_enrolment"].astype(float)
    category_capacity = frame["category_capacity"].astype(float).clip(lower=1.0)
    credits = frame["credits_earned_ratio"].astype(float)
    cgpa = frame["cgpa"].astype(float)
    delay = frame["institution_submission_delay_days"].astype(float)
    days_since_admission = frame["days_since_admission"].astype(float)

    features = pd.DataFrame(index=frame.index)

    features["excess_intake_ratio"] = ((actual - approved) / approved).clip(
        lower=0.0, upper=5.0
    )
    features["intake_utilisation_ratio"] = (actual / approved).clip(lower=0.0, upper=6.0)
    features["excess_intake_flag"] = (actual > approved).astype(float)
    features["category_excess_ratio"] = (
        (category_enrolment - category_capacity) / category_capacity
    ).clip(lower=0.0, upper=5.0)
    features["category_utilisation_ratio"] = (
        category_enrolment / category_capacity
    ).clip(lower=0.0, upper=6.0)
    features["category_capacity_exceeded_flag"] = (
        category_enrolment > category_capacity
    ).astype(float)
    features["category_share_of_enrolment"] = (
        category_enrolment / actual.clip(lower=1.0)
    ).clip(lower=0.0, upper=1.0)
    features["apaar_format_invalid"] = (~frame["apaar_format_valid"].astype(bool)).astype(
        float
    )
    features["apaar_unverified"] = (~frame["apaar_verified"].astype(bool)).astype(float)
    features["duplicate_enrolment_count"] = frame["duplicate_enrolment_count"].astype(
        float
    )
    features["duplicate_enrolment_flag"] = (
        frame["duplicate_enrolment_count"].astype(float) > 0
    ).astype(float)
    features["credit_shortfall"] = (CREDIT_PROGRESS_FLOOR - credits).clip(lower=0.0)
    features["low_credit_progress_flag"] = (
        features["credit_shortfall"] > 0.0
    ).astype(float)
    features["cgpa"] = cgpa
    features["cgpa_abs_deviation"] = (cgpa - 7.0).abs()
    features["cgpa_outlier_flag"] = (
        (cgpa < CGPA_LOWER_BOUND) | (cgpa > CGPA_UPPER_BOUND)
    ).astype(float)
    features["submission_delay_scaled"] = delay / 30.0
    features["submission_delay_flag"] = (
        delay > SUBMISSION_DELAY_REVIEW_DAYS
    ).astype(float)
    features["days_since_admission_scaled"] = (days_since_admission / 365.0).clip(
        lower=0.0, upper=10.0
    )
    features["enrolment_gap"] = actual - approved

    return features.astype(float)


def generate_synthetic_baseline(
    rows: int = BASELINE_ROWS, seed: int = BASELINE_SEED
) -> pd.DataFrame:
    """Generate a reproducible, entirely synthetic baseline of raw records."""

    rng = np.random.default_rng(seed)

    approved = rng.integers(60, 241, size=rows)
    actual = np.clip(
        np.rint(approved * rng.normal(0.93, 0.07, size=rows)), 0, None
    ).astype(int)

    category_capacity = np.maximum(
        1, np.rint(approved * rng.uniform(0.12, 0.35, size=rows)).astype(int)
    )
    category_enrolment = np.clip(
        np.rint(category_capacity * rng.normal(0.88, 0.12, size=rows)), 0, None
    ).astype(int)

    apaar_format_valid = rng.random(rows) > 0.02
    apaar_verified = apaar_format_valid & (rng.random(rows) > 0.06)
    duplicate_enrolment_count = rng.choice(
        [0, 0, 0, 0, 1, 2], size=rows, p=[0.45, 0.2, 0.15, 0.1, 0.07, 0.03]
    )
    credits_earned_ratio = np.clip(rng.normal(0.79, 0.12, size=rows), 0.0, 1.0)
    cgpa = np.clip(rng.normal(7.2, 0.8, size=rows), 0.0, 10.0)
    days_since_admission = rng.integers(30, 1_500, size=rows)
    submission_delay = np.clip(np.rint(rng.gamma(2.0, 4.0, size=rows)), 0, None).astype(
        int
    )

    return pd.DataFrame(
        {
            "record_id": [f"SYN-{i:04d}" for i in range(rows)],
            "student_id": [None] * rows,
            "institution_id": [f"SYN-INST-{i % 17:02d}" for i in range(rows)],
            "programme_id": [f"SYN-PROG-{i % 9:02d}" for i in range(rows)],
            "academic_year_id": ["SYN-AY-2025"] * rows,
            "admission_category": [
                ["GENERAL", "OBC", "SC", "ST", "EWS"][i % 5] for i in range(rows)
            ],
            "approved_intake": approved,
            "actual_enrolment": actual,
            "category_enrolment": category_enrolment,
            "category_capacity": category_capacity,
            "apaar_format_valid": apaar_format_valid,
            "apaar_verified": apaar_verified,
            "duplicate_enrolment_count": duplicate_enrolment_count,
            "credits_earned_ratio": credits_earned_ratio,
            "cgpa": cgpa,
            "days_since_admission": days_since_admission,
            "institution_submission_delay_days": submission_delay,
        }
    )
