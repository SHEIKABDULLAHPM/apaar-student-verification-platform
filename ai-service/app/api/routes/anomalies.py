"""Anomaly scoring endpoints."""

from __future__ import annotations

import logging

from fastapi import APIRouter, Depends, Request

from app.api.deps import get_detector
from app.core.constants import MODEL_METHODOLOGY
from app.schemas.anomaly import ScoreRequest, ScoreResponse, ScoreSummary
from app.services.detector import AnomalyDetector

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/anomalies", tags=["anomalies"])


def _score(payload: ScoreRequest, request: Request, detector: AnomalyDetector) -> ScoreResponse:
    results = detector.score_records(payload.records)
    flagged = sum(1 for result in results if result.is_anomaly)
    settings = request.app.state.settings

    logger.info(
        "anomaly_scoring evaluated=%d flagged=%d model_version=%s",
        len(results),
        flagged,
        detector.model_version,
    )
    # At debug level only hashed references are emitted, never raw PII.
    if logger.isEnabledFor(logging.DEBUG):
        from app.core.logging import safe_record_ref

        logger.debug(
            "anomaly_record_refs refs=%s",
            ",".join(safe_record_ref(result.record_id) for result in results[:20]),
        )

    return ScoreResponse(
        model_name=settings.model_name,
        model_version=settings.model_version,
        methodology=MODEL_METHODOLOGY,
        trained_on_real_data=False,
        results=results,
        summary=ScoreSummary(evaluated=len(results), flagged=flagged),
    )


@router.post("/score", response_model=ScoreResponse)
def score_records(
    payload: ScoreRequest,
    request: Request,
    detector: AnomalyDetector = Depends(get_detector),
) -> ScoreResponse:
    return _score(payload, request, detector)


@router.post("/batch", response_model=ScoreResponse)
def score_batch(
    payload: ScoreRequest,
    request: Request,
    detector: AnomalyDetector = Depends(get_detector),
) -> ScoreResponse:
    """Batch alias with identical validation and response semantics."""

    return _score(payload, request, detector)
