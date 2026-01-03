"""
Dependency Injection Container
Simple DI container for managing service dependencies.
"""
from typing import Dict, Type, TypeVar, Callable, Any, Optional
from functools import lru_cache

T = TypeVar('T')


class Container:
    """Simple dependency injection container."""
    
    def __init__(self):
        self._services: Dict[Type, Any] = {}
        self._factories: Dict[Type, Callable] = {}
        self._singletons: Dict[Type, Any] = {}
    
    def register(self, service_type: Type[T], instance: T = None, factory: Callable[[], T] = None, singleton: bool = True):
        """
        Register a service in the container.
        
        Args:
            service_type: The type/interface to register
            instance: Direct instance (for singletons)
            factory: Factory function to create instances
            singleton: Whether to treat as singleton
        """
        if instance is not None:
            self._services[service_type] = instance
            if singleton:
                self._singletons[service_type] = instance
        elif factory is not None:
            self._factories[service_type] = factory
            if not singleton:
                # For non-singletons, store factory only
                pass
        else:
            raise ValueError("Either instance or factory must be provided")
    
    def get(self, service_type: Type[T]) -> T:
        """
        Get a service instance from the container.
        
        Args:
            service_type: The type/interface to retrieve
            
        Returns:
            Instance of the requested type
        """
        # Check for direct instance
        if service_type in self._services:
            return self._services[service_type]
        
        # Check for singleton
        if service_type in self._singletons:
            return self._singletons[service_type]
        
        # Check for factory
        if service_type in self._factories:
            instance = self._factories[service_type]()
            if service_type in self._singletons or service_type not in [t for t in self._singletons.keys()]:
                # Auto-singleton if not explicitly non-singleton
                self._singletons[service_type] = instance
            return instance
        
        raise ValueError(f"Service {service_type} not registered in container")
    
    def has(self, service_type: Type[T]) -> bool:
        """Check if a service is registered."""
        return service_type in self._services or service_type in self._factories
    
    def clear(self):
        """Clear all registered services (useful for testing)."""
        self._services.clear()
        self._factories.clear()
        self._singletons.clear()


# Global container instance
container = Container()


