"""Validation, batch limits and structured error-envelope tests."""

from __future__ import annotations

import pytest


def _assert_structured_error(response, expected_status: int = 422):
    assert response.status_code == expected_status, response.text
    body = response.json()
    assert set(body) == {"error"}
    error = body["error"]
    assert set(error) == {"code", "message", "details", "request_id"}
    assert error["code"] == "VALIDATION_ERROR"
    assert isinstance(error["message"], str) and error["message"]
    assert isinstance(error["details"], list) and error["details"]
    assert isinstance(error["request_id"], str) and error["request_id"]
    return error


def test_empty_records_are_rejected(client):
    for path in ("/api/v1/anomalies/score", "/api/v1/anomalies/batch"):
        error = _assert_structured_error(client.post(path, json={"records": []}))
        assert any("records" in detail["field"] for detail in error["details"])


def test_oversize_batch_is_rejected(client, record_factory):
    records = [
        record_factory(record_id=f"oversize-{index}") for index in range(501)
    ]
    error = _assert_structured_error(
        client.post("/api/v1/anomalies/batch", json={"records": records})
    )
    assert any("records" in detail["field"] for detail in error["details"])


@pytest.mark.parametrize(
    "field,value",
    [
        ("approved_intake", -1),
        ("actual_enrolment", -5),
        ("category_enrolment", -1),
        ("category_capacity", -1),
        ("duplicate_enrolment_count", -1),
        ("credits_earned_ratio", -0.1),
        ("credits_earned_ratio", 1.2),
        ("cgpa", -0.5),
        ("cgpa", 10.5),
        ("days_since_admission", -1),
        ("institution_submission_delay_days", -3),
    ],
)
def test_negative_or_out_of_range_values_are_rejected(
    client, record_factory, field, value
):
    bad = record_factory(**{field: value})
    error = _assert_structured_error(
        client.post("/api/v1/anomalies/score", json={"records": [bad]})
    )
    assert any(field in detail["field"] for detail in error["details"])


def test_missing_required_field_is_rejected(client, record_factory):
    bad = record_factory()
    del bad["cgpa"]
    error = _assert_structured_error(
        client.post("/api/v1/anomalies/score", json={"records": [bad]})
    )
    assert any("cgpa" in detail["field"] for detail in error["details"])


def test_invalid_admission_category_is_rejected(client, record_factory):
    bad = record_factory(admission_category="UNKNOWN")
    error = _assert_structured_error(
        client.post("/api/v1/anomalies/score", json={"records": [bad]})
    )
    assert any("admission_category" in detail["field"] for detail in error["details"])


def test_duplicate_record_ids_are_rejected(client, record_factory):
    records = [record_factory(record_id="same"), record_factory(record_id="same")]
    error = _assert_structured_error(
        client.post("/api/v1/anomalies/score", json={"records": records})
    )
    assert any("record_id" in detail["message"] for detail in error["details"])


def test_unknown_extra_fields_are_ignored_for_forward_compatibility(
    client, record_factory
):
    record = record_factory(future_field="ignored")
    response = client.post("/api/v1/anomalies/score", json={"records": [record]})
    assert response.status_code == 200
    assert response.json()["summary"]["evaluated"] == 1


def test_unknown_route_uses_structured_envelope(client):
    response = client.get("/does-not-exist")
    assert response.status_code == 404
    body = response.json()
    assert body["error"]["code"] == "NOT_FOUND"
    assert body["error"]["details"] == []
    assert body["error"]["request_id"]


def test_malformed_json_is_rejected_with_envelope(client):
    response = client.post(
        "/api/v1/anomalies/score",
        content=b'{"records": [',
        headers={"Content-Type": "application/json"},
    )
    _assert_structured_error(response)
