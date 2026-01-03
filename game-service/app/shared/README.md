# Shared Utilities

## Overview

The `shared` directory contains common utilities and infrastructure used across all modules.

## Components

### Container (`container.py`)
Dependency Injection container for managing service dependencies.

```python
from app.shared.container import container

# Register service
container.register(Service, instance=Service(), singleton=True)

# Resolve service
service = container.get(Service)
```

### Module Loader (`module_loader.py`)
Auto-discovers and loads modules from the filesystem.

```python
from app.shared.module_loader import module_loader

# Auto-discover modules
modules = module_loader.discover_modules()

# Load all enabled modules
module_loader.load_all_modules(app, api_prefix="/api/v1")
```

### Exceptions (`exceptions.py`)
Centralized exception handling for the application.

```python
from app.shared.exceptions import ValidationError, ServiceError

# Use typed exceptions
raise ValidationError("Invalid input")
raise ServiceError("Service unavailable")
```

## Exception Types

- `AppException` - Base application exception
- `ValidationError` - 400 Bad Request
- `NotFoundError` - 404 Not Found
- `ServiceError` - 500 Internal Server Error

All exceptions are automatically handled by registered exception handlers in the main app.


