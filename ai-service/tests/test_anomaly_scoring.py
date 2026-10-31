"""Anomaly scoring contract, determinism and reason-code tests."""

from __future__ import annotations

import pytest

REQUIRED_REASON_CODES = {
    "EXCESS_INTAKE",
    "CATEGORY_CAPACITY_EXCEEDED",
    "APAAR_FORMAT_INVALID",
    "APAAR_UNVERIFIED",
    "DUPLICATE_ENROLMENT_SIGNAL",
    "LOW_CREDIT_PROGRESS",
    "CGPA_OUTLIER",
    "SUBMISSION_DELAY",
}


def _score(client, records):
    response = client.post("/api/v1/anomalies/score", json={"records": records})
    assert response.status_code == 200, response.text
    return response.json()


def test_normal_record_is_not_flagged(client, normal_record):
    body = _score(client, [normal_record])
    result = body["results"][0]

    assert body["model_name"] == "apaar-regulatory-anomaly"
    assert body["model_version"] == "0.1.0"
    assert body["trained_on_real_data"] is False
    assert body["summary"] == {"evaluated": 1, "flagged": 0}

    assert 0.0 <= result["anomaly_score"] <= 1.0
    assert result["is_anomaly"] is False
    assert result["severity"] == "LOW"
    assert result["requires_human_review"] is False
    assert result["reason_codes"] == []
    assert result["explanations"] == []


def test_anomaly_record_is_flagged_with_explanations(client, anomaly_record):
    body = _score(client, [anomaly_record])
    result = body["results"][0]

    assert body["summary"] == {"evaluated": 1, "flagged": 1}
    assert result["record_id"] == anomaly_record["record_id"]
    assert 0.0 <= result["anomaly_score"] <= 1.0
    assert result["anomaly_score"] > 0.55
    assert result["is_anomaly"] is True
    assert result["severity"] in {"HIGH", "CRITICAL"}
    assert result["requires_human_review"] is True
    assert REQUIRED_REASON_CODES.issubset(set(result["reason_codes"]))
    assert "STATISTICAL_OUTLIER" in result["reason_codes"]

    explanation_codes = {item["code"] for item in result["explanations"]}
    assert explanation_codes == set(result["reason_codes"])
    assert result["feature_contributions"]
    assert all(0.0 <= value <= 1.0 for value in result["feature_contributions"].values())
    total_contribution = sum(result["feature_contributions"].values())
    assert total_contribution <= result["anomaly_score"] + 0.02


def test_score_is_deterministic(client, anomaly_record):
    first = _score(client, [anomaly_record])
    second = _score(client, [anomaly_record])
    assert first["results"] == second["results"]


def test_batch_endpoint_matches_score_endpoint(client, normal_record, anomaly_record):
    score = _score(client, [normal_record, anomaly_record])
    batch = client.post(
        "/api/v1/anomalies/batch",
        json={"records": [normal_record, anomaly_record]},
    )
    assert batch.status_code == 200
    assert batch.json()["results"] == score["results"]
    assert batch.json()["summary"] == {"evaluated": 2, "flagged": 1}


def test_batch_summary_and_order_are_preserved(client, record_factory):
    records = [
        record_factory(record_id="a"),
        record_factory(record_id="b", actual_enrolment=180),
        record_factory(record_id="c", apaar_verified=False),
    ]
    body = _score(client, records)
    assert [result["record_id"] for result in body["results"]] == ["a", "b", "c"]
    assert body["summary"]["evaluated"] == 3
    assert body["summary"]["flagged"] == 1


def test_extreme_statistical_values_trigger_statistical_outlier(
    client, record_factory
):
    record = record_factory(
        actual_enrolment=120_000,
        category_enrolment=25,
        category_capacity=50,
        institution_submission_delay_days=3_000,
    )
    result = _score(client, [record])["results"][0]
    assert "STATISTICAL_OUTLIER" in result["reason_codes"]
    assert result["is_anomaly"] is True


@pytest.mark.parametrize(
    "overrides,expected_code",
    [
        ({"actual_enrolment": 130}, "EXCESS_INTAKE"),
        ({"category_enrolment": 60}, "CATEGORY_CAPACITY_EXCEEDED"),
        ({"apaar_format_valid": False}, "APAAR_FORMAT_INVALID"),
        ({"apaar_verified": False}, "APAAR_UNVERIFIED"),
        ({"duplicate_enrolment_count": 2}, "DUPLICATE_ENROLMENT_SIGNAL"),
        ({"credits_earned_ratio": 0.2}, "LOW_CREDIT_PROGRESS"),
        ({"cgpa": 2.0}, "CGPA_OUTLIER"),
        ({"institution_submission_delay_days": 60}, "SUBMISSION_DELAY"),
    ],
)
def test_each_required_reason_code_can_be_produced(
    client, record_factory, overrides, expected_code
):
    body = _score(client, [record_factory(**overrides)])
    assert expected_code in body["results"][0]["reason_codes"]
