"""
Base HTTP service for making API calls through the gateway.
"""
from __future__ import annotations

from typing import Dict, Any, Optional
import httpx

from app.config import settings


class BaseAPIService:
    """Base service for making HTTP requests to the API Gateway."""
    
    def __init__(self, base_url: Optional[str] = None) -> None:
        self.base_url = (
            base_url
            or settings.JAVA_BACKEND_BASE_URL
            or "http://127.0.0.1:8080"
        ).rstrip("/")
    
    def _get(self, path: str, token: Optional[str] = None, timeout: float = 10.0) -> Dict[str, Any]:
        """Make a GET request."""
        url = f"{self.base_url}{path}"
        headers = {
            "Accept": "application/json",
        }
        # Add service-to-service authentication headers
        if settings.SERVICE_API_KEY:
            headers["X-API-Key"] = settings.SERVICE_API_KEY
        if settings.SERVICE_NAME:
            headers["X-Service-Name"] = settings.SERVICE_NAME
        # Only add Authorization header if token is provided (for user context)
        if token:
            # Ensure token doesn't already have "Bearer " prefix
            if token.startswith("Bearer "):
                headers["Authorization"] = token
            else:
                headers["Authorization"] = f"Bearer {token}"
            print(f"[base_service] Making GET request to {url} with Authorization header (token length: {len(token)})")
        else:
            print(f"[base_service] Making GET request to {url} without Authorization header")
        try:
            with httpx.Client(timeout=timeout) as client:
                r = client.get(url, headers=headers)
                r.raise_for_status()
                return r.json()
        except httpx.TimeoutException:
            raise Exception(f"Request to {path} timed out after {timeout}s")
        except httpx.HTTPStatusError as e:
            error_text = e.response.text[:200] if e.response.text else "No error text"
            raise Exception(f"HTTP error {e.response.status_code} for {path}: {error_text}")
        except Exception as e:
            raise Exception(f"Error calling {path}: {str(e)}")
    
    def _post(self, path: str, token: Optional[str] = None, data: Dict[str, Any] = None, timeout: float = 10.0) -> Dict[str, Any]:
        """Make a POST request."""
        url = f"{self.base_url}{path}"
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
        }
        # Add service-to-service authentication headers
        if settings.SERVICE_API_KEY:
            headers["X-API-Key"] = settings.SERVICE_API_KEY
        if settings.SERVICE_NAME:
            headers["X-Service-Name"] = settings.SERVICE_NAME
        # Only add Authorization header if token is provided (for user context)
        if token:
            headers["Authorization"] = f"Bearer {token}"
        try:
            with httpx.Client(timeout=timeout) as client:
                r = client.post(url, headers=headers, json=data)
                r.raise_for_status()
                return r.json()
        except httpx.TimeoutException:
            raise Exception(f"Request to {path} timed out after {timeout}s")
        except httpx.HTTPStatusError as e:
            raise Exception(f"HTTP error {e.response.status_code} for {path}: {e.response.text}")
        except Exception as e:
            raise Exception(f"Error calling {path}: {str(e)}")

