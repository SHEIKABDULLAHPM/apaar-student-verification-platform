"""FastAPI dependencies."""

from __future__ import annotations

from fastapi import HTTPException, Request, status

from app.services.detector import AnomalyDetector


def get_detector(request: Request) -> AnomalyDetector:
    """Return the lifespan-initialised detector for this application."""

    detector = getattr(request.app.state, "detector", None)
    if detector is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Anomaly detector is not initialised",
        )
    return detector
