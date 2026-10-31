"""Health, model transparency and docs contract tests."""

from __future__ import annotations


def test_health_contract(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "service": "apaar-ai-service",
        "model_version": "0.1.0",
        "environment": "test",
    }


def test_model_info_contract(client):
    response = client.get("/api/v1/model-info")
    assert response.status_code == 200
    body = response.json()

    assert body["model_name"] == "apaar-regulatory-anomaly"
    assert body["model_version"] == "0.1.0"
    assert body["trained_on_real_data"] is False
    assert body["threshold"] == 0.55
    assert "investigation support only" in body["methodology"]
    assert "excess_intake_ratio" in body["features"]
    assert "category_capacity_exceeded_flag" in body["features"]
    assert len(body["features"]) == len(set(body["features"]))
    assert body["limitations"]
    assert any("synthetic" in limitation.lower() for limitation in body["limitations"])
    assert any("human review" in limitation.lower() for limitation in body["limitations"])


def test_request_id_is_echoed_and_generated(client):
    response = client.get("/health", headers={"X-Request-ID": "test-request-123"})
    assert response.headers["X-Request-ID"] == "test-request-123"

    generated = client.get("/health")
    assert generated.headers["X-Request-ID"]
    assert len(generated.headers["X-Request-ID"]) >= 16

    unsafe = client.get("/health", headers={"X-Request-ID": "bad value\nwith newline"})
    assert unsafe.status_code == 200
    assert "\n" not in unsafe.headers["X-Request-ID"]


def test_swagger_and_openapi_enabled(client):
    docs = client.get("/docs")
    assert docs.status_code == 200
    assert "text/html" in docs.headers["content-type"]

    schema = client.get("/openapi.json")
    assert schema.status_code == 200
    paths = schema.json()["paths"]
    for path in (
        "/health",
        "/api/v1/model-info",
        "/api/v1/anomalies/score",
        "/api/v1/anomalies/batch",
        "/api/v1/features/preview",
        "/api/v1/evaluate",
    ):
        assert path in paths, path
