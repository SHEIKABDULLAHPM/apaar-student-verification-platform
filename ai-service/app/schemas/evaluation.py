"""Contracts for offline synthetic evaluation of the anomaly workflow."""

from __future__ import annotations

import math
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator


class LabelledFeatures(BaseModel):
    model_config = ConfigDict(extra="ignore")

    features: dict[str, float] = Field(min_length=1, max_length=200)
    label: Literal[0, 1]

    @field_validator("features")
    @classmethod
    def _feature_names_non_empty(cls, value: dict[str, float]) -> dict[str, float]:
        if any(not key.strip() for key in value):
            raise ValueError("feature names must not be blank")
        if any(not math.isfinite(number) for number in value.values()):
            raise ValueError("feature values must be finite numbers")
        return value


class EvaluateRequest(BaseModel):
    model_config = ConfigDict(extra="ignore")

    records: list[LabelledFeatures] = Field(min_length=1, max_length=5_000)
    threshold: float | None = Field(
        default=None,
        ge=0.0,
        le=1.0,
        description="Optional override. Defaults to the configured anomaly threshold.",
    )


class EvaluationMetrics(BaseModel):
    precision: float = Field(ge=0.0, le=1.0)
    recall: float = Field(ge=0.0, le=1.0)
    f1: float = Field(ge=0.0, le=1.0)
    accuracy: float = Field(ge=0.0, le=1.0)
    support: int = Field(ge=0)
    positives: int = Field(ge=0)
    predicted_positive: int = Field(ge=0)


class ConfusionMatrix(BaseModel):
    true_negative: int = Field(ge=0)
    false_positive: int = Field(ge=0)
    false_negative: int = Field(ge=0)
    true_positive: int = Field(ge=0)
    matrix: list[list[int]]


class EvaluateResponse(BaseModel):
    model_version: str
    threshold: float = Field(ge=0.0, le=1.0)
    metrics: EvaluationMetrics
    confusion_matrix: ConfusionMatrix
    note: str
