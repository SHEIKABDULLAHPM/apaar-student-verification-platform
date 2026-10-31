"""Explainability quality tests: messages are non-accusatory and traceable."""

from __future__ import annotations

import pytest

FORBIDDEN_CONFIRMATIONS = (
    "fraud confirmed",
    "confirmed fraud",
    "is fraudulent",
    "guilty",
    "non-compliant confirmed",
)


def _score(client, record):
    response = client.post("/api/v1/anomalies/score", json={"records": [record]})
    assert response.status_code == 200, response.text
    return response.json()["results"][0]


def test_explanations_never_assert_fraud(client, anomaly_record):
    result = _score(client, anomaly_record)
    assert result["explanations"]
    for explanation in result["explanations"]:
        message = explanation["message"].lower()
        assert not any(phrase in message for phrase in FORBIDDEN_CONFIRMATIONS)
        assert explanation["message"]
        assert explanation["code"] == explanation["code"].upper()
        assert 0.0 <= explanation["contribution"] <= 1.0


def test_explanations_include_observed_and_expected(client, anomaly_record):
    result = _score(client, anomaly_record)
    by_code = {item["code"]: item for item in result["explanations"]}

    excess = by_code["EXCESS_INTAKE"]
    assert excess["observed"] == 180
    assert excess["expected"] == 120
    assert "investigation trigger" in excess["message"].lower()

    unverified = by_code["APAAR_UNVERIFIED"]
    assert unverified["observed"] == 1
    assert unverified["expected"] == 0
    assert "cannot" in unverified["message"].lower()

    duplicate = by_code["DUPLICATE_ENROLMENT_SIGNAL"]
    assert duplicate["observed"] == 2
    assert duplicate["expected"] == 0


def test_reason_codes_and_feature_contributions_are_aligned(
    client, anomaly_record
):
    result = _score(client, anomaly_record)
    contributions = result["feature_contributions"]
    assert "excess_intake_ratio" in contributions
    assert "apaar_unverified" in contributions
    assert "robust_z_max" in contributions
    assert all(round(value, 4) == value for value in contributions.values())
    assert len(result["reason_codes"]) == len(set(result["reason_codes"]))


@pytest.mark.parametrize(
    "overrides,code",
    [
        ({"actual_enrolment": 150}, "EXCESS_INTAKE"),
        ({"category_enrolment": 80}, "CATEGORY_CAPACITY_EXCEEDED"),
        ({"apaar_format_valid": False}, "APAAR_FORMAT_INVALID"),
        ({"apaar_verified": False}, "APAAR_UNVERIFIED"),
        ({"duplicate_enrolment_count": 3}, "DUPLICATE_ENROLMENT_SIGNAL"),
        ({"credits_earned_ratio": 0.1}, "LOW_CREDIT_PROGRESS"),
        ({"cgpa": 9.95}, "CGPA_OUTLIER"),
        ({"institution_submission_delay_days": 120}, "SUBMISSION_DELAY"),
    ],
)
def test_reason_explanations_are_present_for_each_signal(
    client, record_factory, overrides, code
):
    result = _score(client, record_factory(**overrides))
    explanatory_codes = {item["code"] for item in result["explanations"]}
    assert code in result["reason_codes"]
    assert code in explanatory_codes
    assert code in " ".join(result["reason_codes"])
