#!/bin/bash

# BanditGames Platform - Service Startup Script

set -e

echo "🚀 BanditGames Platform - Starting Services"
echo "============================================"

# Check if Docker is running
echo "📋 Checking Docker daemon..."
if ! docker ps &>/dev/null; then
    echo "❌ Docker daemon is not running!"
    echo ""
    echo "Please start Docker Desktop:"
    echo "  - macOS: Open Docker Desktop application"
    echo "  - Linux: sudo systemctl start docker"
    echo ""
    echo "Waiting for Docker to start..."
    
    # Wait for Docker to start (max 60 seconds)
    for i in {1..60}; do
        if docker ps &>/dev/null; then
            echo "✅ Docker is now running!"
            break
        fi
        sleep 1
        echo -n "."
    done
    
    if ! docker ps &>/dev/null; then
        echo ""
        echo "❌ Docker daemon did not start. Please start Docker manually and try again."
        exit 1
    fi
fi

echo "✅ Docker is running"
echo ""

# Navigate to project directory
cd "$(dirname "$0")"

# Check if .env exists
if [ ! -f .env ]; then
    echo "📝 Creating .env file from .env.example..."
    cp .env.example .env
    echo "✅ .env file created"
    echo ""
fi

# Validate docker-compose file
echo "🔍 Validating docker-compose.yml..."
if docker-compose config --quiet; then
    echo "✅ docker-compose.yml is valid"
    echo ""
else
    echo "❌ docker-compose.yml has errors"
    exit 1
fi

# Build and start services
echo "🔨 Building and starting services..."
echo "   This may take several minutes on first run..."
echo ""

docker-compose up -d --build

echo ""
echo "⏳ Waiting for services to start..."
echo "   This typically takes 3-4 minutes..."
echo ""

# Wait for API Gateway to be healthy
echo "🔍 Checking service health..."
for i in {1..60}; do
    if curl -sf http://localhost:8080/actuator/health &>/dev/null; then
        echo "✅ API Gateway is healthy"
        break
    fi
    sleep 5
    echo -n "."
done

echo ""
echo ""
echo "============================================"
echo "✅ Services are starting!"
echo "============================================"
echo ""
echo "📊 Service Status:"
docker-compose ps
echo ""
echo "🌐 Access URLs:"
echo "   API Gateway Swagger UI: http://localhost:8080/api/docs/swagger-ui.html"
echo "   API Gateway Health:     http://localhost:8080/actuator/health"
echo "   Platform Backend:       http://localhost:8081"
echo "   Game Service:           http://localhost:8000"
echo "   Keycloak Admin:         http://localhost:8090 (admin/admin)"
echo "   RabbitMQ Management:    http://localhost:15672 (guest/guest)"
echo ""
echo "📋 View logs:"
echo "   docker-compose logs -f"
echo ""
echo "🛑 Stop services:"
echo "   docker-compose down"
echo ""
