"""Stable descriptive constants shared by routes and documentation."""

MODEL_METHODOLOGY = (
    "ensemble of robust z-score and isolation forest on synthetic baseline; "
    "investigation support only"
)

MODEL_LIMITATIONS: tuple[str, ...] = (
    "Trained and calibrated on synthetic data only; never trained on real institutional or student data.",
    "Scores are statistical investigation triggers, not proof of fraud, non-compliance, or intent.",
    "This service does not perform authoritative APAAR ID verification and cannot confirm student identity.",
    "This service does not make deterministic regulatory, eligibility, admission, or enforcement decisions.",
    "False positives and false negatives are expected; every flagged record requires human review.",
    "Robust-score baselines and thresholds are fixed for reproducibility and may not fit every institution.",
    "Not a substitute for authorised institutional review, audit procedures, or official APAAR systems.",
)

PREVIEW_NOTE = (
    "Feature preview only: values are derived deterministically from the submitted "
    "record. No anomaly score, verification result, or regulatory decision is produced."
)
