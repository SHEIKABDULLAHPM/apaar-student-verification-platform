"""Input normalisation and feature-frame construction."""

from __future__ import annotations

import numpy as np
import pandas as pd

from app.schemas.anomaly import AnomalyRecord
from app.services.features import FEATURE_COLUMNS, engineer_features

RAW_NUMERIC_COLUMNS: tuple[str, ...] = (
    "approved_intake",
    "actual_enrolment",
    "category_enrolment",
    "category_capacity",
    "duplicate_enrolment_count",
    "credits_earned_ratio",
    "cgpa",
    "days_since_admission",
    "institution_submission_delay_days",
)


class FeatureBuildError(ValueError):
    """Raised when a feature frame cannot be produced safely."""


def records_to_frame(records: list[AnomalyRecord]) -> pd.DataFrame:
    """Convert validated records to a normalised raw DataFrame.

    Pydantic has already enforced ranges and required fields. This layer only
    normalises dtypes and identifier whitespace, and performs a final finite
    number check so the detector never receives NaN/inf.
    """

    if not records:
        raise FeatureBuildError("at least one record is required")

    frame = pd.DataFrame(record.model_dump(mode="python") for record in records)
    for column in RAW_NUMERIC_COLUMNS:
        frame[column] = pd.to_numeric(frame[column], errors="raise")

    for column in ("apaar_format_valid", "apaar_verified"):
        frame[column] = frame[column].astype(bool)

    frame["admission_category"] = frame["admission_category"].astype(str)
    for column in ("record_id", "institution_id", "student_id", "programme_id", "academic_year_id"):
        frame[column] = frame[column].apply(
            lambda value: None if value is None else str(value).strip()
        )

    if frame["record_id"].duplicated().any():
        raise FeatureBuildError("record_id values must be unique")

    numeric_values = frame[list(RAW_NUMERIC_COLUMNS)].to_numpy(dtype=float)
    if not np.isfinite(numeric_values).all():
        raise FeatureBuildError("record values must be finite numbers")

    frame.reset_index(drop=True, inplace=True)
    return frame


def build_feature_frame(records: list[AnomalyRecord]) -> tuple[pd.DataFrame, pd.DataFrame]:
    """Return ``(raw_frame, engineered_frame)`` for the supplied records."""

    raw_frame = records_to_frame(records)
    engineered = engineer_features(raw_frame)

    if list(engineered.columns) != list(FEATURE_COLUMNS):
        raise FeatureBuildError("engineered feature schema mismatch")
    if not np.isfinite(engineered.to_numpy(dtype=float)).all():
        raise FeatureBuildError("engineered features must be finite")

    return raw_frame, engineered
