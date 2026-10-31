"""FastAPI application factory for the APAAR investigation-support AI service.

Boundaries:
- No CORS middleware: this service is intended for internal network use only.
- No authentication layer is implemented here; deploy behind the backend gateway.
- The service never makes authoritative verification or regulatory decisions.
"""

from __future__ import annotations

import logging
import re
import time
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.api.errors import error_response
from app.api.routes import anomalies, evaluation, features, health
from app.core.config import Settings, get_settings
from app.core.logging import configure_logging, request_id_ctx
from app.services.detector import AnomalyDetector

logger = logging.getLogger(__name__)

_REQUEST_ID_PATTERN = re.compile(r"^[A-Za-z0-9._:-]{1,64}$")

_ERROR_CODE_BY_STATUS = {
    400: "BAD_REQUEST",
    401: "UNAUTHORIZED",
    403: "FORBIDDEN",
    404: "NOT_FOUND",
    405: "METHOD_NOT_ALLOWED",
    409: "CONFLICT",
    413: "PAYLOAD_TOO_LARGE",
    422: "VALIDATION_ERROR",
    429: "TOO_MANY_REQUESTS",
    503: "SERVICE_UNAVAILABLE",
}


def _request_id_for(request: Request) -> str:
    request_id = getattr(request.state, "request_id", None)
    return str(request_id) if request_id else request_id_ctx.get()


def _clean_request_id(value: str | None) -> str:
    candidate = (value or "").strip()
    if _REQUEST_ID_PATTERN.fullmatch(candidate):
        return candidate
    return uuid.uuid4().hex


def _validation_details(exc: RequestValidationError) -> list[dict[str, object]]:
    details: list[dict[str, object]] = []
    for error in exc.errors()[:20]:
        location = [str(part) for part in error.get("loc", ()) if part != "body"]
        message = str(error.get("msg", "Invalid value"))
        if message.startswith("Value error, "):
            message = message[len("Value error, ") :]
        details.append(
            {
                "field": ".".join(location) or "body",
                "message": message,
                "type": str(error.get("type", "value_error")),
            }
        )
    return details


def create_app(settings: Settings | None = None) -> FastAPI:
    """Create the ASGI app; tests pass an isolated Settings instance."""

    app_settings = settings or get_settings()
    configure_logging(app_settings.log_level)

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        app.state.detector = AnomalyDetector(
            model_version=app_settings.model_version,
            threshold=app_settings.anomaly_threshold,
        )
        logger.info(
            "service_start environment=%s model_version=%s",
            app_settings.app_env,
            app_settings.model_version,
        )
        try:
            yield
        finally:
            logger.info("service_stop environment=%s", app_settings.app_env)

    app = FastAPI(
        title="APAAR AI Service",
        description=(
            "Statistical anomaly detection and explainability for AICTE-approved "
            "institutions. Investigation support only; never authoritative "
            "verification or a deterministic regulatory decision."
        ),
        version=app_settings.model_version,
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
        lifespan=lifespan,
    )
    app.state.settings = app_settings

    # Intentionally no CORSMiddleware: the service is internal-only.
    @app.middleware("http")
    async def request_context(request: Request, call_next):
        request_id = _clean_request_id(request.headers.get("X-Request-ID"))
        request.state.request_id = request_id
        token = request_id_ctx.set(request_id)
        started = time.perf_counter()
        try:
            response = await call_next(request)
            elapsed_ms = (time.perf_counter() - started) * 1000.0
            response.headers["X-Request-ID"] = request_id
            logger.info(
                "request_complete method=%s path=%s status=%d duration_ms=%.1f",
                request.method,
                request.url.path,
                response.status_code,
                elapsed_ms,
            )
            return response
        finally:
            request_id_ctx.reset(token)

    @app.exception_handler(RequestValidationError)
    async def validation_error_handler(request: Request, exc: RequestValidationError):
        return error_response(
            status_code=422,
            code="VALIDATION_ERROR",
            message="Request validation failed",
            request_id=_request_id_for(request),
            details=_validation_details(exc),
        )

    @app.exception_handler(StarletteHTTPException)
    async def http_error_handler(request: Request, exc: StarletteHTTPException):
        status_code = int(exc.status_code)
        code = _ERROR_CODE_BY_STATUS.get(status_code, "HTTP_ERROR")
        message = exc.detail if isinstance(exc.detail, str) else "Request failed"
        return error_response(
            status_code=status_code,
            code=code,
            message=message,
            request_id=_request_id_for(request),
        )

    @app.exception_handler(Exception)
    async def unhandled_error_handler(request: Request, exc: Exception):
        logger.exception(
            "unhandled_exception method=%s path=%s type=%s",
            request.method,
            request.url.path,
            type(exc).__name__,
        )
        return error_response(
            status_code=500,
            code="INTERNAL_ERROR",
            message="Internal server error",
            request_id=_request_id_for(request),
        )

    app.include_router(health.router)
    app.include_router(anomalies.router)
    app.include_router(features.router)
    app.include_router(evaluation.router)
    return app


app = create_app()
