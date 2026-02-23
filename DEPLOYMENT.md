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
- Apache Kafka 3.0+
- Elasticsearch 8.0+

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

### 3. Start Kafka with Zookeeper
```bash
# Using Docker Compose
docker-compose up -d zookeeper kafka

# Or manually
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka
bin/kafka-server-start.sh config/server.properties

# Create topic
bin/kafka-topics.sh --create \
  --topic weather-alerts \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

### 4. Start Elasticsearch
```bash
# Using Docker
docker run --name elasticsearch-weather \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -p 9200:9200 \
  -d elasticsearch:8.0.0

# Or start locally
bin/elasticsearch
```

### 5. Configure Application
Edit `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/weather_alerts
    username: postgres
    password: postgres
  
  kafka:
    bootstrap-servers: localhost:9092
    
  elasticsearch:
    uris: http://localhost:9200
```

### 6. Build and Run
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR
java -jar target/weather-alert-backend-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

### 7. Verify Installation
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

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    networks:
      - weather-net

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - weather-net

  elasticsearch:
    image: elasticsearch:8.0.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data
    networks:
      - weather-net

  app:
    build: .
    depends_on:
      - postgres
      - kafka
      - elasticsearch
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/weather_alerts
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SPRING_ELASTICSEARCH_URIS: http://elasticsearch:9200
    networks:
      - weather-net

volumes:
  postgres-data:
  elasticsearch-data:

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
- **Amazon MSK**: For Kafka
- **Amazon Elasticsearch Service**: For search
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

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Elasticsearch
SPRING_ELASTICSEARCH_URIS=http://localhost:9200

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
SPRING_JPA_HIBERNATE_DDL_AUTO=update

# Actuator
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics
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

#### 2. Kafka Connection Failed
```bash
# Check Kafka status
bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# Check if topic exists
bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Create topic if missing
bin/kafka-topics.sh --create --topic weather-alerts --bootstrap-server localhost:9092
```

#### 3. Elasticsearch Not Responding
```bash
# Check Elasticsearch status
curl http://localhost:9200/_cluster/health

# Check indexes
curl http://localhost:9200/_cat/indices

# View logs
docker logs elasticsearch-weather
```

#### 4. Application Won't Start
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

#### 5. NOAA API Rate Limiting
- Implement caching with Redis
- Add exponential backoff
- Respect NOAA's rate limits
- Use appropriate User-Agent header

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

#### Kafka Producer Settings
```yaml
spring:
  kafka:
    producer:
      batch-size: 16384
      buffer-memory: 33554432
      compression-type: gzip
```

---

## Security Checklist

- [ ] Change default database password
- [ ] Enable HTTPS/TLS
- [ ] Configure Kafka SASL/SSL
- [ ] Secure Elasticsearch with authentication
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

### Elasticsearch Snapshot
```bash
# Create snapshot repository
curl -X PUT "localhost:9200/_snapshot/my_backup" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/mount/backups/elasticsearch"
  }
}'

# Create snapshot
curl -X PUT "localhost:9200/_snapshot/my_backup/snapshot_1?wait_for_completion=true"

# Restore snapshot
curl -X POST "localhost:9200/_snapshot/my_backup/snapshot_1/_restore"
```

---

## Support & Maintenance

### Health Checks
- Database connectivity
- Kafka broker status
- Elasticsearch cluster health
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
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Elasticsearch Documentation](https://www.elastic.co/guide/index.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

## Contact

For deployment support:
- GitHub Issues: https://github.com/armper/Weather-alert-backend/issues
- Email: support@weatheralert.example.com
