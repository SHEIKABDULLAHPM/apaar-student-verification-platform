"""Shared pytest fixtures.

The app is created once per test session with an isolated test environment.
Synthetic data only; no external services are contacted.
"""

from __future__ import annotations

import os
from collections.abc import Callable, Iterator
from typing import Any

import pytest
from fastapi.testclient import TestClient

os.environ.setdefault("APP_ENV", "test")
os.environ.setdefault("LOG_LEVEL", "WARNING")

from app.core.config import Settings  # noqa: E402
from app.main import create_app  # noqa: E402


@pytest.fixture(scope="session")
def app():
    return create_app(
        Settings(
            app_env="test",
            model_version="0.1.0",
            anomaly_threshold=0.55,
            log_level="WARNING",
        )
    )


@pytest.fixture(scope="session")
def client(app) -> Iterator[TestClient]:
    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture
def record_factory() -> Callable[..., dict[str, Any]]:
    """Return a factory for a plausible, non-anomalous synthetic record."""

    def make_record(**overrides: Any) -> dict[str, Any]:
        record: dict[str, Any] = {
            "record_id": "rec-0001",
            "student_id": None,
            "institution_id": "inst-001",
            "programme_id": "prog-001",
            "academic_year_id": "ay-2025",
            "admission_category": "GENERAL",
            "approved_intake": 120,
            "actual_enrolment": 110,
            "category_enrolment": 25,
            "category_capacity": 50,
            "apaar_format_valid": True,
            "apaar_verified": True,
            "duplicate_enrolment_count": 0,
            "credits_earned_ratio": 0.9,
            "cgpa": 7.4,
            "days_since_admission": 120,
            "institution_submission_delay_days": 3,
        }
        record.update(overrides)
        return record

    return make_record


@pytest.fixture
def normal_record(record_factory: Callable[..., dict[str, Any]]) -> dict[str, Any]:
    return record_factory()


@pytest.fixture
def anomaly_record(record_factory: Callable[..., dict[str, Any]]) -> dict[str, Any]:
    return record_factory(
        record_id="rec-anomaly",
        actual_enrolment=180,
        category_enrolment=60,
        category_capacity=30,
        apaar_format_valid=False,
        apaar_verified=False,
        duplicate_enrolment_count=2,
        credits_earned_ratio=0.2,
        cgpa=2.0,
        institution_submission_delay_days=60,
    )
