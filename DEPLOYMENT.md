# Weather Alert Backend - Deployment Guide

This guide provides instructions for deploying the Weather Alert Backend in various environments.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Local Development Setup](#local-development-setup)
- [Docker Deployment](#docker-deployment)
- [Production Deployment](#production-deployment)
- [Environment Variables](#environment-variables)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 14+

### Optional Tools
- Docker & Docker Compose
- Kubernetes (for cloud deployment)
- Git

---

## Local Development Setup

### 1. Clone the Repository
```bash
git clone https://github.com/armper/Weather-alert-backend.git
cd Weather-alert-backend
```

### 2. Start PostgreSQL
```bash
# Using Docker
docker run --name postgres-weather \
  -e POSTGRES_DB=weather_alerts \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:14

# Or install locally and create database
createdb weather_alerts
```

### 3. Configure Application
Edit `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/weather_alerts
    username: postgres
    password: postgres
```

### 4. Build and Run
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR
java -jar target/weather-alert-backend-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

### 5. Verify Installation
```bash
# Check health endpoint (if implemented)
curl http://localhost:8080/actuator/health

# Test weather data endpoint
curl http://localhost:8080/api/weather/active
```

---

## Docker Deployment

### Using Docker Compose

Create `docker-compose.yml`:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: weather_alerts
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - weather-net

  app:
    build: .
    depends_on:
      - postgres
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/weather_alerts
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    networks:
      - weather-net

volumes:
  postgres-data:

networks:
  weather-net:
    driver: bridge
```

Create `Dockerfile`:
```dockerfile
FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/weather-alert-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Deploy with Docker Compose
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

---

## Production Deployment

### Google Cloud Run

Cloud Run is the recommended Google Cloud target for this repository because the application is already containerized. A few constraints matter for this service:

- Scheduler work can now be disabled inside the app and triggered externally through admin job endpoints.
- Alert activity is delivered through authenticated API reads, so the service no longer depends on an instance-local push broker.
- Cloud SQL works cleanly through the Cloud SQL Java connector now included in `pom.xml`.

Files added for this path:

- `scripts/deploy-cloud-run.sh`
- `deploy/cloudrun/env.example.yaml`
- `cloudbuild.yaml`
- `infra/terraform/`
- `.gcloudignore`

#### 1. Install and authenticate Google Cloud CLI

```bash
brew install --cask gcloud-cli
gcloud auth login
gcloud auth application-default login
gcloud config set project YOUR_PROJECT_ID
```

#### 2. Provision backing services

- **Cloud Run**: application runtime
- **Cloud SQL for PostgreSQL**: primary database
Terraform in `infra/terraform` can provision the Cloud SQL instance, database, and database user directly.

#### 3. Fill environment variables

Use `deploy/cloudrun/env.example.yaml` as the starting point for `deploy/cloudrun/env.yaml`.

Required values include:

- Cloud SQL sizing and database credentials
- `APP_SECURITY_*` usernames/passwords
- `APP_SECURITY_JWT_SECRET` with at least 32 UTF-8 bytes
- `APP_ADMIN_JOBS_TOKEN` if Cloud Scheduler will call `/api/admin/jobs/**`
- SMTP settings if email delivery is enabled
- Stripe settings if subscription billing is enabled:
  - `APP_BILLING_STRIPE_ENABLED=true`
  - `APP_BILLING_STRIPE_SECRET_KEY`
  - `APP_BILLING_STRIPE_WEBHOOK_SECRET`
  - `APP_BILLING_STRIPE_PRICE_ID`
  - optional `APP_BILLING_STRIPE_SUCCESS_URL`
  - optional `APP_BILLING_STRIPE_CANCEL_URL`
- `APP_WEATHER_PROCESSING_SCHEDULE_ENABLED=false`
- `APP_NOTIFICATION_DELIVERY_RETRY_POLLER_ENABLED=false`
- `APP_RETENTION_SCHEDULE_ENABLED=false`

#### 4. Deploy

```bash
chmod +x scripts/deploy-cloud-run.sh
PROJECT_ID=YOUR_PROJECT_ID REGION=us-east1 ./scripts/deploy-cloud-run.sh
```

The deploy script enables the required Google APIs and runs `gcloud run deploy` from source.

#### 5. Trigger scheduled work from Cloud Scheduler

Recommended Cloud Scheduler targets:

- `POST /api/admin/jobs/weather-processing`
- `POST /api/admin/jobs/alert-delivery-retries`
- `POST /api/admin/jobs/data-retention`

Send the shared token in the `X-Admin-Job-Token` header. The app accepts that header only for `/api/admin/jobs/**` when `APP_ADMIN_JOBS_TOKEN` is configured.

#### 6. Infra as Code

Terraform scaffolding lives in `infra/terraform`.

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
terraform apply
```

This provisions:

- required Google APIs
- Artifact Registry
- Cloud SQL instance, database, and database user
- Secret Manager secrets
- Cloud Run service
- optional Cloud Scheduler jobs

Stripe secret values can also be carried through Terraform:

- `stripe_enabled`
- `stripe_price_id`
- `stripe_secret_key`
- `stripe_webhook_secret`
- optional `stripe_success_url`
- optional `stripe_cancel_url`

For a brand-new project, publish the first container image before the first full `terraform apply`:

```bash
gcloud builds submit --config cloudbuild.yaml \
  --substitutions=_REGION=us-east1,_SERVICE_NAME=weather-alert-backend,_AR_REPOSITORY=weather-alert-backend,_IMAGE_NAME=weather-alert-backend,_DEPLOY=false
```

#### 7. Continuous Delivery

`cloudbuild.yaml` builds the image, pushes it to Artifact Registry, and deploys a new Cloud Run revision by default.

Manual run:

```bash
gcloud builds submit --config cloudbuild.yaml \
  --substitutions=_REGION=us-east1,_SERVICE_NAME=weather-alert-backend,_AR_REPOSITORY=weather-alert-backend,_IMAGE_NAME=weather-alert-backend
```

Bootstrap image only:

```bash
gcloud builds submit --config cloudbuild.yaml \
  --substitutions=_REGION=us-east1,_SERVICE_NAME=weather-alert-backend,_AR_REPOSITORY=weather-alert-backend,_IMAGE_NAME=weather-alert-backend,_DEPLOY=false
```

For repeated deploys, create a Cloud Build trigger that points at this repo and `cloudbuild.yaml`.

#### 8. Verify

```bash
gcloud run services describe weather-alert-backend \
  --region us-east1 \
  --format='value(status.url)'

curl https://YOUR_CLOUD_RUN_URL/actuator/health
```

For Stripe in test mode, point a webhook endpoint at:

```text
https://YOUR_CLOUD_RUN_URL/api/stripe/webhook
```

Listen for at least:

- `checkout.session.completed`
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`

### AWS Deployment

#### 1. Using AWS Elastic Beanstalk
```bash
# Install EB CLI
pip install awsebcli

# Initialize Elastic Beanstalk
eb init -p java-17 weather-alert-backend

# Create environment
eb create production

# Deploy
eb deploy

# Open application
eb open
```

#### 2. Using AWS ECS (Fargate)
```bash
# Build and push Docker image
docker build -t weather-alert-backend .
docker tag weather-alert-backend:latest <ecr-repo-url>:latest
aws ecr get-login-password | docker login --username AWS --password-stdin <ecr-repo-url>
docker push <ecr-repo-url>:latest

# Create ECS task definition and service using AWS Console or CLI
```

#### 3. Services Configuration
- **RDS PostgreSQL**: For database
- **Application Load Balancer**: For traffic distribution
- **CloudWatch**: For monitoring and logs

### Kubernetes Deployment

Create `k8s/deployment.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: weather-alert-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: weather-alert-backend
  template:
    metadata:
      labels:
        app: weather-alert-backend
    spec:
      containers:
      - name: app
        image: weather-alert-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: database.url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: database.username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: database.password
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: weather-alert-backend
spec:
  selector:
    app: weather-alert-backend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

Deploy to Kubernetes:
```bash
# Apply configurations
kubectl apply -f k8s/

# Check status
kubectl get pods
kubectl get services

# View logs
kubectl logs -f deployment/weather-alert-backend

# Scale deployment
kubectl scale deployment/weather-alert-backend --replicas=5
```

---

## Environment Variables

### Required Variables
```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/weather_alerts
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<secure-password>

# Application
SERVER_PORT=8080
```

### Optional Variables
```bash
# Logging
LOGGING_LEVEL_COM_WEATHER_ALERT=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=WARN

# JPA
SPRING_JPA_SHOW_SQL=false
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

# Actuator
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics

# NOAA client resilience/pacing
APP_NOAA_REQUEST_TIMEOUT_SECONDS=8
APP_NOAA_RETRY_MAX_ATTEMPTS=2
APP_NOAA_RETRY_BACKOFF_MILLIS=250
APP_NOAA_MIN_REQUEST_INTERVAL_MILLIS=150
APP_NOAA_OUTAGE_FAILURE_THRESHOLD=4
APP_NOAA_OUTAGE_OPEN_SECONDS=30
```

---

## Monitoring

### Spring Boot Actuator

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Enable endpoints in `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### Prometheus & Grafana

1. **Add Micrometer Prometheus**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

2. **Configure Prometheus** (`prometheus.yml`):
```yaml
scrape_configs:
  - job_name: 'weather-alert-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

3. **Import Grafana Dashboard**: Use Spring Boot dashboard template

### Logging

Configure structured logging in `application.yml`:
```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  level:
    com.weather.alert: INFO
    org.springframework: WARN
  file:
    name: logs/weather-alert.log
    max-size: 10MB
    max-history: 30
```

### Scheduler Orchestration (Operational Notes)

- Scheduler uses fixed-delay execution (`5m`) with initial delay (`30s`) to avoid overlapping runs.
- Criteria are processed in batches of `100`.
- Current/forecast NOAA calls are cached per scheduler run by coordinate/window to reduce duplicate upstream calls.
- Criteria evaluation outcomes include:
  - `MET`
  - `NOT_MET`
  - `UNAVAILABLE` (provider outage/short-circuit)
- `UNAVAILABLE` does not mutate `criteria_state`, preventing false clear/rearm transitions.

Useful actuator metrics:

```bash
curl http://localhost:8080/actuator/metrics/weather.alert.processing.duration
curl http://localhost:8080/actuator/metrics/weather.alert.criteria.evaluated
curl http://localhost:8080/actuator/metrics/weather.alert.triggered
curl http://localhost:8080/actuator/metrics/weather.noaa.requests
curl http://localhost:8080/actuator/metrics/weather.noaa.request.duration
```

---

## Troubleshooting

### Common Issues

#### 1. Cannot Connect to PostgreSQL
```bash
# Check if PostgreSQL is running
pg_isready -h localhost -p 5432

# Check connection
psql -h localhost -U postgres -d weather_alerts

# Verify credentials in application.yml
```

#### 2. Application Won't Start
```bash
# Check Java version
java -version

# Verify port 8080 is available
netstat -an | grep 8080

# Check application logs
tail -f logs/weather-alert.log

# Run with debug
java -jar app.jar --debug
```

#### 3. NOAA API Rate Limiting
- Tune `APP_NOAA_MIN_REQUEST_INTERVAL_MILLIS` to reduce upstream pressure
- Tune retries/timeouts (`APP_NOAA_REQUEST_TIMEOUT_SECONDS`, `APP_NOAA_RETRY_*`)
- Use outage guard defaults (`APP_NOAA_OUTAGE_FAILURE_THRESHOLD`, `APP_NOAA_OUTAGE_OPEN_SECONDS`) to short-circuit repeated failures
- Verify logs for `operation=point_metadata|hourly_forecast|latest_observation`

### Performance Tuning

#### JVM Options
```bash
java -Xms512m -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar app.jar
```

#### Database Connection Pool
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

---

## Security Checklist

- [ ] Change default database password
- [ ] Enable HTTPS/TLS
- [ ] Implement API rate limiting
- [ ] Add Spring Security
- [ ] Use secrets management (AWS Secrets Manager, HashiCorp Vault)
- [ ] Enable CORS properly
- [ ] Implement input validation
- [ ] Add API authentication (JWT, OAuth2)
- [ ] Regular security updates
- [ ] Enable audit logging

---

## Backup & Recovery

### Database Backup
```bash
# Backup
pg_dump -h localhost -U postgres weather_alerts > backup.sql

# Restore
psql -h localhost -U postgres weather_alerts < backup.sql

# Automated backups with cron
0 2 * * * pg_dump -h localhost -U postgres weather_alerts > /backups/weather_$(date +\%Y\%m\%d).sql
```

## Support & Maintenance

### Health Checks
- Database connectivity
- NOAA API availability
- Disk space and memory usage

### Regular Maintenance
- Monitor logs for errors
- Review and rotate logs
- Update dependencies
- Apply security patches
- Review and optimize database queries
- Clean up old data
- Monitor resource usage

---

## Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [NOAA Weather API](https://www.weather.gov/documentation/services-web-api)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

## Contact

For deployment support:
- GitHub Issues: https://github.com/armper/Weather-alert-backend/issues
- Email: support@weatheralert.example.com
