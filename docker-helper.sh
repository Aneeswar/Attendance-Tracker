#!/bin/bash

# Docker Helper Scripts for Attendance App

echo "Attendance App - Docker Management"
echo "===================================="
echo ""
echo "Available commands:"
echo "  ./docker-helper.sh up        - Start containers (build if needed)"
echo "  ./docker-helper.sh down      - Stop and remove containers"
echo "  ./docker-helper.sh logs      - View application logs"
echo "  ./docker-helper.sh logs-db   - View database logs"
echo "  ./docker-helper.sh rebuild   - Rebuild Docker image"
echo "  ./docker-helper.sh clean     - Remove containers, volumes, and images"
echo "  ./docker-helper.sh shell     - Open psql shell to database"
echo ""

COMMAND=$1

case $COMMAND in
  up)
    echo "Starting containers..."
    docker-compose up -d
    echo "✓ Containers started"
    echo "App: http://localhost:8080"
    echo "Database: localhost:5432"
    ;;
  down)
    echo "Stopping containers..."
    docker-compose down
    echo "✓ Containers stopped"
    ;;
  logs)
    echo "Application logs:"
    docker-compose logs -f app
    ;;
  logs-db)
    echo "Database logs:"
    docker-compose logs -f postgres
    ;;
  rebuild)
    echo "Rebuilding Docker image..."
    docker-compose build --no-cache
    echo "✓ Image rebuilt"
    ;;
  clean)
    echo "Cleaning up Docker resources..."
    docker-compose down -v
    docker rmi attendance-postgres attendance-app
    echo "✓ Cleanup complete"
    ;;
  shell)
    echo "Opening psql shell..."
    docker-compose exec postgres psql -U postgres -d attendance_db
    ;;
  *)
    echo "Usage: $0 {up|down|logs|logs-db|rebuild|clean|shell}"
    exit 1
    ;;
esac
