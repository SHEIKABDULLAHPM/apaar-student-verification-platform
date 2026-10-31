"""Synthetic evaluation endpoint.

Accepts either ``{"records": [...]}`` or a raw array of labelled feature rows,
so integration callers can use the concise contract without a wrapper.
"""

from __future__ import annotations

import logging
from typing import Annotated

from fastapi import APIRouter, Body, HTTPException, Request
from pydantic import Field

from app.schemas.evaluation import EvaluateRequest, EvaluateResponse, LabelledFeatures
from app.services.evaluation import evaluate_labelled

logger = logging.getLogger(__name__)

router = APIRouter(tags=["evaluation"])

EvaluateBody = (
    EvaluateRequest
    | Annotated[list[LabelledFeatures], Field(min_length=1, max_length=5_000)]
)


@router.post("/api/v1/evaluate", response_model=EvaluateResponse)
def evaluate(
    request: Request,
    payload: EvaluateBody = Body(...),
) -> EvaluateResponse:
    settings = request.app.state.settings
    if isinstance(payload, list):
        records = payload
        threshold = settings.anomaly_threshold
    else:
        records = payload.records
        threshold = (
            payload.threshold
            if payload.threshold is not None
            else settings.anomaly_threshold
        )

    try:
        response = evaluate_labelled(
            records=records,
            threshold=threshold,
            model_version=settings.model_version,
        )
    except ValueError as exc:
        raise HTTPException(
            status_code=422,
            detail=f"Invalid synthetic evaluation input: {exc}",
        ) from exc

    logger.info(
        "synthetic_evaluation records=%d threshold=%.3f precision=%.4f recall=%.4f",
        len(records),
        threshold,
        response.metrics.precision,
        response.metrics.recall,
    )
    return response
