"""Contracts for engineered-feature previews (no scoring is performed)."""

from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.anomaly import MAX_BATCH_SIZE, AnomalyRecord


class FeaturePreviewRequest(BaseModel):
    model_config = ConfigDict(extra="ignore")

    records: list[AnomalyRecord] = Field(min_length=1, max_length=MAX_BATCH_SIZE)


class FeatureRow(BaseModel):
    record_id: str
    features: dict[str, float]


class FeaturePreviewResponse(BaseModel):
    model_version: str
    feature_columns: list[str]
    rows: list[FeatureRow]
    count: int = Field(ge=0)
    scored: bool = False
    note: str
