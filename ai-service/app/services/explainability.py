"""Human-readable explanations and reason-code ordering.

Reason codes are investigation triggers. Messages must never state or imply
that fraud or non-compliance has been confirmed.
"""

from __future__ import annotations

import pandas as pd

from app.schemas.anomaly import AnomalyRecord, Explanation
from app.services.features import (
    CGPA_LOWER_BOUND,
    CGPA_UPPER_BOUND,
    CREDIT_PROGRESS_FLOOR,
    SUBMISSION_DELAY_REVIEW_DAYS,
)

REASON_ORDER: tuple[str, ...] = (
    "EXCESS_INTAKE",
    "CATEGORY_CAPACITY_EXCEEDED",
    "APAAR_FORMAT_INVALID",
    "APAAR_UNVERIFIED",
    "DUPLICATE_ENROLMENT_SIGNAL",
    "LOW_CREDIT_PROGRESS",
    "CGPA_OUTLIER",
    "SUBMISSION_DELAY",
    "STATISTICAL_OUTLIER",
    "MULTIVARIATE_OUTLIER",
)

STATISTICAL_OUTLIER_Z = 3.5
MULTIVARIATE_OUTLIER_SCORE = 0.75

# A reason is associated with the engineered feature that drove its contribution.
REASON_FEATURE: dict[str, str] = {
    "EXCESS_INTAKE": "excess_intake_ratio",
    "CATEGORY_CAPACITY_EXCEEDED": "category_excess_ratio",
    "APAAR_FORMAT_INVALID": "apaar_format_invalid",
    "APAAR_UNVERIFIED": "apaar_unverified",
    "DUPLICATE_ENROLMENT_SIGNAL": "duplicate_enrolment_count",
    "LOW_CREDIT_PROGRESS": "credit_shortfall",
    "CGPA_OUTLIER": "cgpa_abs_deviation",
    "SUBMISSION_DELAY": "submission_delay_scaled",
    "STATISTICAL_OUTLIER": "robust_z_max",
    "MULTIVARIATE_OUTLIER": "isolation_forest_score",
}


def _number(value: float) -> int | float:
    return int(value) if float(value).is_integer() else round(float(value), 4)


def explain_reason(
    code: str,
    record: AnomalyRecord,
    features: pd.Series,
    contribution: float,
) -> Explanation:
    """Build one explanation with observed/expected context."""

    contribution = round(max(0.0, min(1.0, contribution)), 4)

    if code == "EXCESS_INTAKE":
        gap = record.actual_enrolment - record.approved_intake
        return Explanation(
            code=code,
            message=(
                f"Reported enrolment ({record.actual_enrolment}) exceeds approved "
                f"intake ({record.approved_intake}) by {gap}. Review the admission "
                "and approval records; this is an investigation trigger only."
            ),
            observed=record.actual_enrolment,
            expected=record.approved_intake,
            contribution=contribution,
        )

    if code == "CATEGORY_CAPACITY_EXCEEDED":
        gap = record.category_enrolment - record.category_capacity
        return Explanation(
            code=code,
            message=(
                f"{record.admission_category.value} category enrolment "
                f"({record.category_enrolment}) exceeds category capacity "
                f"({record.category_capacity}) by {gap}. Verify category-wise "
                "seat allocation before drawing any conclusion."
            ),
            observed=record.category_enrolment,
            expected=record.category_capacity,
            contribution=contribution,
        )

    if code == "APAAR_FORMAT_INVALID":
        return Explanation(
            code=code,
            message=(
                "The submitted APAAR identifier failed the service's format check. "
                "Format validation is not authoritative APAAR verification; resolve "
                "the record through the official verification process."
            ),
            observed=1,
            expected=0,
            contribution=contribution,
        )

    if code == "APAAR_UNVERIFIED":
        return Explanation(
            code=code,
            message=(
                "The APAAR identifier is marked unverified. Only the authoritative "
                "APAAR system can confirm identity and enrolment; this service cannot."
            ),
            observed=1,
            expected=0,
            contribution=contribution,
        )

    if code == "DUPLICATE_ENROLMENT_SIGNAL":
        return Explanation(
            code=code,
            message=(
                f"{record.duplicate_enrolment_count} possible duplicate enrolment "
                "signal(s) were reported. Confirm whether these are data-entry "
                "artefacts or genuine duplicate records."
            ),
            observed=record.duplicate_enrolment_count,
            expected=0,
            contribution=contribution,
        )

    if code == "LOW_CREDIT_PROGRESS":
        return Explanation(
            code=code,
            message=(
                f"Credit completion ratio ({record.credits_earned_ratio:.3f}) is below "
                f"the review floor ({CREDIT_PROGRESS_FLOOR:.2f}). Check academic "
                "records and any approved exceptions."
            ),
            observed=record.credits_earned_ratio,
            expected=CREDIT_PROGRESS_FLOOR,
            contribution=contribution,
        )

    if code == "CGPA_OUTLIER":
        expected = CGPA_LOWER_BOUND if record.cgpa < CGPA_LOWER_BOUND else CGPA_UPPER_BOUND
        return Explanation(
            code=code,
            message=(
                f"CGPA ({record.cgpa:.2f}) falls outside the synthetic baseline's "
                f"plausible band ({CGPA_LOWER_BOUND:.1f}-{CGPA_UPPER_BOUND:.1f}). "
                "This is a statistical flag, not evidence of wrongdoing."
            ),
            observed=record.cgpa,
            expected=expected,
            contribution=contribution,
        )

    if code == "SUBMISSION_DELAY":
        return Explanation(
            code=code,
            message=(
                f"Institution submission delay ({record.institution_submission_delay_days} "
                f"days) exceeds the review threshold ({SUBMISSION_DELAY_REVIEW_DAYS} days). "
                "Check reporting timelines and data-freshness before escalation."
            ),
            observed=record.institution_submission_delay_days,
            expected=SUBMISSION_DELAY_REVIEW_DAYS,
            contribution=contribution,
        )

    if code == "STATISTICAL_OUTLIER":
        robust_z = float(features.get("robust_z_max", 0.0))
        return Explanation(
            code=code,
            message=(
                f"A numeric feature is a robust statistical outlier (max robust "
                f"z-score {robust_z:.2f}, review threshold {STATISTICAL_OUTLIER_Z:.1f}) "
                "relative to the synthetic baseline. Review the source values."
            ),
            observed=round(robust_z, 4),
            expected=STATISTICAL_OUTLIER_Z,
            contribution=contribution,
        )

    if code == "MULTIVARIATE_OUTLIER":
        iso_score = float(features.get("isolation_forest_score", 0.0))
        return Explanation(
            code=code,
            message=(
                "The combination of features is unusual relative to the synthetic "
                "baseline (IsolationForest signal). A multivariate flag is a review "
                "prompt, not a determination."
            ),
            observed=round(iso_score, 4),
            expected=MULTIVARIATE_OUTLIER_SCORE,
            contribution=contribution,
        )

    # Defensive fallback: reason codes are internal constants, but never fail a
    # whole batch because one explanation is missing.
    return Explanation(
        code=code,
        message="An anomaly signal requires review.",
        observed=None,
        expected=None,
        contribution=contribution,
    )


def build_explanations(
    record: AnomalyRecord,
    features: pd.Series,
    contributions: dict[str, float],
) -> list[Explanation]:
    """Return explanations in canonical reason order."""

    ordered_codes = [code for code in REASON_ORDER if code in contributions]
    return [
        explain_reason(code, record, features, contributions[code])
        for code in ordered_codes
    ]
