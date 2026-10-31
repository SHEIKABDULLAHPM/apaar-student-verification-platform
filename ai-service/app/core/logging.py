"""Minimal, privacy-conscious logging helpers.

Application logs deliberately never contain names, APAAR IDs, request bodies,
or raw record identifiers. When correlation is needed, callers use
``hash_identifier`` which produces a short, one-way reference.
"""

from __future__ import annotations

import hashlib
import logging
import sys
from contextvars import ContextVar

request_id_ctx: ContextVar[str] = ContextVar("request_id", default="-")

_LOG_FORMAT = (
    "%(asctime)s level=%(levelname)s logger=%(name)s "
    "request_id=%(request_id)s %(message)s"
)


class RequestIdFilter(logging.Filter):
    """Attach the current request id to every log record."""

    def filter(self, record: logging.LogRecord) -> bool:  # noqa: A003
        record.request_id = request_id_ctx.get()
        return True


def configure_logging(level: str = "INFO") -> None:
    """Configure process logging once, with request correlation and no PII."""

    root = logging.getLogger()
    root.setLevel(level.upper())

    handler_name = "_apaar_ai_handler"
    handler = next(
        (h for h in root.handlers if getattr(h, handler_name, False)), None
    )
    if handler is None:
        handler = logging.StreamHandler(sys.stdout)
        setattr(handler, handler_name, True)
        handler.setFormatter(logging.Formatter(_LOG_FORMAT, datefmt="%Y-%m-%dT%H:%M:%S%z"))
        root.addHandler(handler)

    # Uvicorn installs its own handlers; ensure they also receive the request id.
    for existing in root.handlers:
        if not any(isinstance(f, RequestIdFilter) for f in existing.filters):
            existing.addFilter(RequestIdFilter())


def hash_identifier(value: str | None, length: int = 12) -> str:
    """Return a short, deterministic, non-reversible reference for logging.

    This is not anonymisation for storage; it only reduces accidental PII
    exposure in operational logs.
    """

    if not value:
        return "-"
    digest = hashlib.sha256(f"apaar-ai-service:{value}".encode("utf-8")).hexdigest()
    return digest[:length]


def safe_record_ref(record_id: str | None) -> str:
    """A truncated correlation reference suitable for debug logs."""

    return hash_identifier(record_id, length=10)
