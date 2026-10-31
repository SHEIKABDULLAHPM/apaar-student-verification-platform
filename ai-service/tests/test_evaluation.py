"""Synthetic evaluation endpoint tests."""

from __future__ import annotations

import numpy as np


def _synthetic_labelled_records() -> list[dict]:
    rng = np.random.default_rng(11)
    records: list[dict] = []
    for _ in range(80):
        records.append(
            {
                "features": {
                    "excess_intake_ratio": float(abs(rng.normal(0.0, 0.03))),
                    "cgpa": float(rng.normal(7.0, 0.2)),
                    "credit_shortfall": 0.0,
                    "submission_delay_scaled": float(abs(rng.normal(0.0, 0.1))),
                },
                "label": 0,
            }
        )
    for _ in range(20):
        records.append(
            {
                "features": {
                    "excess_intake_ratio": float(rng.normal(1.0, 0.2)),
                    "cgpa": float(rng.normal(2.0, 0.3)),
                    "credit_shortfall": float(rng.normal(0.4, 0.05)),
                    "submission_delay_scaled": float(rng.normal(3.0, 0.3)),
                },
                "label": 1,
            }
        )
    return records


def test_evaluate_returns_metrics_confusion_and_caveat(client):
    records = _synthetic_labelled_records()
    response = client.post("/api/v1/evaluate", json={"records": records})
    assert response.status_code == 200, response.text
    body = response.json()

    assert body["model_version"] == "0.1.0"
    assert body["threshold"] == 0.55
    metrics = body["metrics"]
    for key in ("precision", "recall", "f1", "accuracy"):
        assert 0.0 <= metrics[key] <= 1.0
    assert metrics["support"] == 100
    assert metrics["positives"] == 20
    assert metrics["precision"] > 0.5

    matrix = body["confusion_matrix"]
    assert matrix["matrix"] == [
        [matrix["true_negative"], matrix["false_positive"]],
        [matrix["false_negative"], matrix["true_positive"]],
    ]
    assert (
        matrix["true_negative"]
        + matrix["false_positive"]
        + matrix["false_negative"]
        + matrix["true_positive"]
        == 100
    )
    assert "Synthetic evaluation only" in body["note"]
    assert "not validate real-world" in body["note"]


def test_evaluate_accepts_raw_array_and_threshold_override(client):
    records = _synthetic_labelled_records()
    response = client.post("/api/v1/evaluate", json=records)
    assert response.status_code == 200
    assert response.json()["threshold"] == 0.55

    wrapper = client.post(
        "/api/v1/evaluate", json={"records": records, "threshold": 0.8}
    )
    assert wrapper.status_code == 200
    assert wrapper.json()["threshold"] == 0.8


def test_evaluate_empty_dataset_is_rejected(client):
    response = client.post("/api/v1/evaluate", json={"records": []})
    assert response.status_code == 422
    assert response.json()["error"]["code"] == "VALIDATION_ERROR"


def test_evaluate_invalid_label_is_rejected(client):
    response = client.post(
        "/api/v1/evaluate",
        json=[{"features": {"cgpa": 7.4}, "label": 2}],
    )
    assert response.status_code == 422
    assert response.json()["error"]["code"] == "VALIDATION_ERROR"
