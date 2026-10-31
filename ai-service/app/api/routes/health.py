"""Health and model-transparency endpoints."""

from __future__ import annotations

from fastapi import APIRouter, Request

from app.core.constants import MODEL_LIMITATIONS, MODEL_METHODOLOGY
from app.schemas.anomaly import HealthResponse, ModelInfoResponse
from app.services.features import FEATURE_COLUMNS

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
@router.get("/api/v1/health", response_model=HealthResponse)
def health(request: Request) -> HealthResponse:
    settings = request.app.state.settings
    return HealthResponse(
        status="ok",
        service=settings.service_name,
        model_version=settings.model_version,
        environment=settings.app_env,
    )


@router.get("/api/v1/model-info", response_model=ModelInfoResponse)
def model_info(request: Request) -> ModelInfoResponse:
    settings = request.app.state.settings
    return ModelInfoResponse(
        model_name=settings.model_name,
        model_version=settings.model_version,
        methodology=MODEL_METHODOLOGY,
        features=list(FEATURE_COLUMNS),
        limitations=list(MODEL_LIMITATIONS),
        threshold=settings.anomaly_threshold,
        trained_on_real_data=False,
    )
