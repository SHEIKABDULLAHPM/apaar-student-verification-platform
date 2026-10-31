"""Preprocessing, feature engineering and feature-preview tests."""

from __future__ import annotations

import numpy as np
import pandas as pd
import pytest

from app.schemas.anomaly import AnomalyRecord
from app.services.features import FEATURE_COLUMNS, engineer_features
from app.services.preprocessing import build_feature_frame, records_to_frame


def _record_frame(records):
    return records_to_frame([AnomalyRecord.model_validate(record) for record in records])


def test_records_to_frame_and_engineered_columns(normal_record):
    raw, features = build_feature_frame([AnomalyRecord.model_validate(normal_record)])

    assert len(raw) == 1
    assert list(features.columns) == list(FEATURE_COLUMNS)
    assert np.isfinite(features.to_numpy(dtype=float)).all()

    row = features.iloc[0]
    assert row["excess_intake_ratio"] == 0.0
    assert row["intake_utilisation_ratio"] == pytest.approx(110 / 120)
    assert row["category_excess_ratio"] == 0.0
    assert row["category_capacity_exceeded_flag"] == 0.0
    assert row["apaar_format_invalid"] == 0.0
    assert row["apaar_unverified"] == 0.0
    assert row["credit_shortfall"] == 0.0
    assert row["cgpa_abs_deviation"] == pytest.approx(0.4)


def test_zero_denominators_are_safe(normal_record):
    record = dict(
        normal_record,
        approved_intake=0,
        actual_enrolment=0,
        category_enrolment=0,
        category_capacity=0,
    )
    features = engineer_features(_record_frame([record]))
    assert np.isfinite(features.to_numpy(dtype=float)).all()
    assert features.iloc[0]["intake_utilisation_ratio"] == 0.0
    assert features.iloc[0]["category_utilisation_ratio"] == 0.0


def test_feature_preview_returns_features_without_scoring(client, normal_record):
    response = client.post("/api/v1/features/preview", json={"records": [normal_record]})
    assert response.status_code == 200
    body = response.json()

    assert body["count"] == 1
    assert body["scored"] is False
    assert body["feature_columns"] == list(FEATURE_COLUMNS)
    assert body["rows"][0]["record_id"] == normal_record["record_id"]
    assert body["rows"][0]["features"]["excess_intake_ratio"] == 0.0
    assert "anomaly_score" not in body["rows"][0]
    assert "results" not in body
    assert "No anomaly score" in body["note"]


def test_feature_preview_rejects_invalid_record(client, normal_record):
    bad = dict(normal_record, cgpa=-1.0)
    response = client.post("/api/v1/features/preview", json={"records": [bad]})
    assert response.status_code == 422
    body = response.json()
    assert body["error"]["code"] == "VALIDATION_ERROR"
    assert body["error"]["details"]
    assert body["error"]["request_id"]
