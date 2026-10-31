"""Engineered-feature preview endpoint (no scoring)."""

from __future__ import annotations

import logging

from fastapi import APIRouter, Request

from app.core.constants import PREVIEW_NOTE
from app.schemas.features import FeaturePreviewRequest, FeaturePreviewResponse, FeatureRow
from app.services.features import FEATURE_COLUMNS
from app.services.preprocessing import build_feature_frame

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/features", tags=["features"])


@router.post("/preview", response_model=FeaturePreviewResponse)
def preview_features(
    payload: FeaturePreviewRequest, request: Request
) -> FeaturePreviewResponse:
    _, engineered = build_feature_frame(payload.records)
    rows = [
        FeatureRow(
            record_id=record.record_id,
            features={
                column: round(float(engineered.iloc[position][column]), 6)
                for column in FEATURE_COLUMNS
            },
        )
        for position, record in enumerate(payload.records)
    ]
    logger.info("feature_preview records=%d", len(rows))
    return FeaturePreviewResponse(
        model_version=request.app.state.settings.model_version,
        feature_columns=list(FEATURE_COLUMNS),
        rows=rows,
        count=len(rows),
        scored=False,
        note=PREVIEW_NOTE,
    )
