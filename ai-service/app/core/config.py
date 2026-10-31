"""Environment-driven configuration for the APAAR AI service.

The service is intentionally stateless apart from a deterministically fitted
synthetic baseline detector. No real student data or secrets are required.
"""

from functools import lru_cache

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Runtime settings, readable from environment variables or a local .env."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    service_name: str = "apaar-ai-service"
    model_name: str = "apaar-regulatory-anomaly"
    model_version: str = "0.1.0"
    app_env: str = "local"
    anomaly_threshold: float = Field(default=0.55, ge=0.0, le=1.0)
    log_level: str = "INFO"

    @field_validator("log_level")
    @classmethod
    def _normalise_log_level(cls, value: str) -> str:
        level = value.strip().upper()
        allowed = {"DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"}
        if level not in allowed:
            raise ValueError(f"log_level must be one of {sorted(allowed)}")
        return level

    @field_validator("app_env", "model_version", "model_name", "service_name")
    @classmethod
    def _non_empty(cls, value: str) -> str:
        cleaned = value.strip()
        if not cleaned:
            raise ValueError("value must not be empty")
        return cleaned


@lru_cache
def get_settings() -> Settings:
    """Return a cached settings instance."""

    return Settings()
