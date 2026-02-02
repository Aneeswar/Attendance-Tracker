# Docker Deployment Guide

## Overview

This project includes Docker support for:
- **Local Development**: `docker-compose.yml` with PostgreSQL
- **Production**: Multi-stage `Dockerfile` optimized for Cloud Run/Render
- **Helper Scripts**: Easy commands for Docker management

## Files Included

| File | Purpose |
|------|---------|
| `Dockerfile` | Production-ready multi-stage Docker image |
| `docker-compose.yml` | Local development with app + PostgreSQL |
| `.dockerignore` | Files excluded from Docker build |
| `.env.example` | Environment variables template |
| `docker-helper.sh` | Linux/Mac helper script |
| `docker-helper.bat` | Windows helper script |

---

## Local Development with Docker Compose

### Prerequisites
- Docker Desktop installed ([download](https://www.docker.com/products/docker-desktop))
- Docker Compose (included with Docker Desktop)

### Quick Start

**Linux/Mac:**
```bash
chmod +x docker-helper.sh
./docker-helper.sh up
```

**Windows:**
```bash
docker-helper.bat up
```

### Access Your App

- **Application**: http://localhost:8080
- **Database**: localhost:5432 (postgres/0000)

### Available Commands

**Linux/Mac:**
```bash
./docker-helper.sh up         # Start containers
./docker-helper.sh down       # Stop containers
./docker-helper.sh logs       # View app logs
./docker-helper.sh logs-db    # View database logs
./docker-helper.sh rebuild    # Rebuild image
./docker-helper.sh clean      # Remove all containers/volumes
./docker-helper.sh shell      # Open database shell
```

**Windows:**
```bash
docker-helper.bat up         # Start containers
docker-helper.bat down       # Stop containers
docker-helper.bat logs       # View app logs
docker-helper.bat logs-db    # View database logs
docker-helper.bat rebuild    # Rebuild image
docker-helper.bat clean      # Remove all containers/volumes
docker-helper.bat shell      # Open database shell
```

### Manual Docker Commands

```bash
# Build and start
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop everything
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Rebuild image
docker-compose build --no-cache

# Access PostgreSQL
docker-compose exec postgres psql -U postgres -d attendance_db
```

---

## Production Build

### Build Locally

```bash
docker build -t attendance-app:latest .
```

### Run Locally

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=postgresql://YOUR_HOST:5432/attendance_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD \
  attendance-app:latest
```

### Push to Registry

**Google Container Registry:**
```bash
docker tag attendance-app:latest gcr.io/YOUR_PROJECT_ID/attendance-app:latest
docker push gcr.io/YOUR_PROJECT_ID/attendance-app:latest
```

**Docker Hub:**
```bash
docker tag attendance-app:latest YOUR_USERNAME/attendance-app:latest
docker push YOUR_USERNAME/attendance-app:latest
```

---

## Environment Configuration

### Local Development (.env)

Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

Edit `.env` with your local values (defaults work out of the box).

### Production (Render/Cloud Run)

Set environment variables in your platform's dashboard:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `PORT`
- `JAVA_TOOL_OPTIONS`

---

## Docker Image Details

### Image Specifications

| Property | Value |
|----------|-------|
| **Base Image** | eclipse-temurin:21-jre-alpine |
| **Size** | ~180MB |
| **Java Version** | 21 LTS |
| **OS** | Alpine Linux 3.18 |
| **Port** | 8080 |

### Multi-Stage Build

```
Stage 1: Builder
├── Maven 3.9
├── Java 21
├── Build with: mvn clean package -DskipTests
└── Output: Attendance-0.0.1-SNAPSHOT.jar

Stage 2: Runtime
├── Alpine Linux (minimal)
├── Java 21 JRE only (no compiler)
├── Copy JAR from builder
└── ~160MB smaller image
```

### Health Check

The Dockerfile includes a health check:
```bash
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/login || exit 1
```

---

## Troubleshooting

### Container won't start

```bash
# Check logs
docker-compose logs app

# Common issues:
# 1. Port 8080 already in use
sudo lsof -i :8080  # Find process
kill -9 <PID>       # Kill process

# 2. Database connection failed
docker-compose logs postgres  # Check DB is running

# 3. Build errors
docker-compose build --no-cache  # Rebuild without cache
```

### Database issues

```bash
# Connect to database shell
docker-compose exec postgres psql -U postgres -d attendance_db

# List tables
\dt

# Exit
\q
```

### View resource usage

```bash
docker stats
```

### Clean everything

```bash
docker-compose down -v
docker system prune -a
```

---

## Performance Optimization

### For Local Development

Current settings in `docker-compose.yml`:
- JVM Memory: 512MB (`-Xmx512m`)
- Container restart: unless-stopped
- Health checks: enabled

### For Production

Adjust in `Dockerfile` or runtime:
- JVM Memory: Set based on available RAM
- `JAVA_TOOL_OPTIONS=-Xmx256m` (Cloud Run: 512MB total)
- `JAVA_TOOL_OPTIONS=-Xmx512m` (Render: 1GB+ RAM)

---

## Security Notes

⚠️ **Important:**

1. **Don't commit `.env` file** - Use `.env.example` as template
2. **Credentials in docker-compose.yml** - Only for local development
3. **Change default passwords** - For production databases
4. **Use secrets management** - Use Render/Cloud Run environment variables for sensitive data

---

## Next Steps

1. **Local Testing**: `./docker-helper.sh up` (or `.bat` on Windows)
2. **Verify App**: http://localhost:8080/login
3. **Deploy to Render**: Push to GitHub, Render auto-detects Dockerfile
4. **Monitor**: Check Logs tab in Render/Cloud Run dashboard

---

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [Render Native Docker Support](https://render.com/docs/deploy-docker)
