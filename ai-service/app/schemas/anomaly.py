"""Pydantic v2 contracts for anomaly scoring.

The backend Java service depends on these field names and response shapes.
Unknown extra fields are ignored for forward compatibility; missing required
fields and out-of-range values are rejected with a 422 validation envelope.
"""

from __future__ import annotations

from enum import StrEnum

from pydantic import BaseModel, ConfigDict, Field, model_validator

MAX_BATCH_SIZE = 500


class AdmissionCategory(StrEnum):
    GENERAL = "GENERAL"
    OBC = "OBC"
    SC = "SC"
    ST = "ST"
    EWS = "EWS"
    NRI = "NRI"
    OCI = "OCI"
    FOREIGN_NATIONAL = "FOREIGN_NATIONAL"
    SUPERNUMERARY = "SUPERNUMERARY"


class Severity(StrEnum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class AnomalyRecord(BaseModel):
    """One regulatory/enrolment record submitted for investigation support."""

    model_config = ConfigDict(extra="ignore", str_strip_whitespace=True)

    record_id: str = Field(min_length=1, max_length=128)
    student_id: str | None = Field(default=None, max_length=128)
    institution_id: str = Field(min_length=1, max_length=128)
    programme_id: str | None = Field(default=None, max_length=128)
    academic_year_id: str | None = Field(default=None, max_length=128)
    admission_category: AdmissionCategory

    approved_intake: int = Field(ge=0, le=1_000_000)
    actual_enrolment: int = Field(ge=0, le=1_000_000)
    category_enrolment: int = Field(ge=0, le=1_000_000)
    category_capacity: int = Field(ge=0, le=1_000_000)

    apaar_format_valid: bool
    apaar_verified: bool
    duplicate_enrolment_count: int = Field(ge=0, le=100)
    credits_earned_ratio: float = Field(ge=0.0, le=1.0)
    cgpa: float = Field(ge=0.0, le=10.0)
    days_since_admission: int = Field(ge=0, le=36_500)
    institution_submission_delay_days: int = Field(ge=0, le=3_650)

    @model_validator(mode="after")
    def _ids_must_not_be_blank(self) -> "AnomalyRecord":
        for name in ("student_id", "programme_id", "academic_year_id"):
            value = getattr(self, name)
            if value is not None and not value.strip():
                setattr(self, name, None)
        return self


class ScoreRequest(BaseModel):
    """Scoring request; the same shape is used by /score and /batch."""

    model_config = ConfigDict(extra="ignore")

    records: list[AnomalyRecord] = Field(
        min_length=1,
        max_length=MAX_BATCH_SIZE,
        description="1-500 records to evaluate. Empty or oversize batches are rejected.",
    )

    @model_validator(mode="after")
    def _record_ids_must_be_unique(self) -> "ScoreRequest":
        ids = [record.record_id for record in self.records]
        if len(ids) != len(set(ids)):
            raise ValueError("record_id values must be unique within a request")
        return self


class Explanation(BaseModel):
    code: str
    message: str
    observed: int | float | None = None
    expected: int | float | None = None
    contribution: float = Field(ge=0.0, le=1.0)


class AnomalyResult(BaseModel):
    record_id: str
    anomaly_score: float = Field(ge=0.0, le=1.0)
    is_anomaly: bool
    severity: Severity
    reason_codes: list[str]
    explanations: list[Explanation]
    feature_contributions: dict[str, float]
    requires_human_review: bool


class ScoreSummary(BaseModel):
    evaluated: int = Field(ge=0)
    flagged: int = Field(ge=0)


class ScoreResponse(BaseModel):
    model_name: str
    model_version: str
    methodology: str
    trained_on_real_data: bool = False
    results: list[AnomalyResult]
    summary: ScoreSummary


class ModelInfoResponse(BaseModel):
    model_name: str
    model_version: str
    methodology: str
    features: list[str]
    limitations: list[str]
    threshold: float = Field(ge=0.0, le=1.0)
    trained_on_real_data: bool = False


class HealthResponse(BaseModel):
    status: str
    service: str
    model_version: str
    environment: str
