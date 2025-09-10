# Aegis Support: Multi-Agent Banking Insights & Fraud Triage

A production-ready internal tool for care agents to analyze transactions, generate AI reports, and triage suspected fraud through a multi-agent pipeline.

## Quick Start

### Option 1: Docker Compose (Recommended)
```bash
# 1. Start the entire system
docker compose up

# 2. Access the application
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# Metrics: http://localhost:8080/metrics
# Prometheus: http://localhost:9090

# 3. Run evaluations
npm run eval
```

### Quick Start Scenarios

#### 🚀 Just Want to See It Work
```bash
docker compose up
# Wait for services to start, then visit http://localhost:3000
```

#### 🔧 Want to Develop/Modify Code
```bash
# Terminal 1: Start backend
cd backend && ./gradlew bootRun

# Terminal 2: Start frontend  
cd frontend && npm start

# Terminal 3: Run tests
npm run eval
```

#### 🧪 Just Want to Run Tests
```bash
# Start services in background
docker compose up -d

# Run evaluation suite
npm run eval

# Stop services
docker compose down
```

### Option 2: Local Development

#### Prerequisites
- Java 17+
- Node.js 16+
- PostgreSQL 15+
- Redis 7+

#### Backend Setup
```bash
# 1. Navigate to backend directory
cd backend

# 2. Start PostgreSQL and Redis (using Docker)
docker run -d --name postgres -e POSTGRES_DB=aegis_support -e POSTGRES_USER=aegis_user -e POSTGRES_PASSWORD=aegis_password -p 5432:5432 postgres:15-alpine
docker run -d --name redis -p 6379:6379 redis:7-alpine

# 3. Run the Spring Boot application
./gradlew bootRun

# Alternative: Build and run JAR
./gradlew build
java -jar build/libs/aegis-support-1.0.0.jar
```

#### Frontend Setup
```bash
# 1. Navigate to frontend directory
cd frontend

# 2. Install dependencies
npm install

# 3. Start the React development server
npm start

# The frontend will be available at http://localhost:3000
```

#### Running Evaluations
```bash
# From the project root directory
npm run eval

# Or run directly with Node.js
node scripts/run-evals.js

# Or using Gradle (from backend directory)
./gradlew eval
```

## Architecture Overview

- **Frontend**: React + TypeScript dashboard with real-time triage workflow
- **Backend**: Spring Boot microservices with multi-agent fraud detection
- **Database**: PostgreSQL with optimized schema and indexing
- **Agents**: Orchestrated AI agents for fraud analysis, compliance, and insights

## Key Features

- 🔍 **Transaction Analysis**: Upload and analyze customer transactions
- 🤖 **Multi-Agent Triage**: AI-powered fraud detection with explainable decisions
- 📊 **Real-time Dashboard**: KPIs, risk scoring, and agent performance metrics
- 🛡️ **Security**: Rate limiting, input validation, and policy enforcement
- 📈 **Observability**: Health checks, metrics, and comprehensive logging
- ♿ **Accessibility**: WCAG-compliant UI with keyboard navigation
- 🧹 **Optimized**: Clean codebase with removed unused components

## Directory Structure

```
├── frontend/                    # React TypeScript application
│   ├── src/                    # Source code
│   │   ├── components/         # Reusable UI components
│   │   ├── pages/             # Page components
│   │   └── hooks/             # Custom React hooks
│   ├── public/                # Static assets
│   └── package.json           # Frontend dependencies
├── backend/                    # Spring Boot microservices
│   ├── src/main/java/com/aegis/ # Java source code
│   │   ├── controller/        # REST controllers
│   │   ├── service/           # Business logic
│   │   ├── model/             # Data models
│   │   └── eval/              # Evaluation framework
│   ├── src/main/resources/    # Configuration and fixtures
│   └── build.gradle           # Backend dependencies
├── fixtures/                   # Test data and golden cases
│   ├── evals/                 # Evaluation test cases
│   ├── kb/                    # Knowledge base data
│   └── *.json                 # Sample data files
├── scripts/                    # Data generation and deployment
│   ├── run-evals.js           # Evaluation runner
│   ├── performance-test.js    # Performance testing
│   └── prometheus.yml         # Metrics configuration
├── docs/                       # Architecture and design documents
└── docker-compose.yml          # Local development environment
```

## Available Scripts

### Root Level
```bash
npm run eval              # Run evaluation suite
```

### Frontend (`cd frontend`)
```bash
npm start                 # Start development server
npm run build            # Build for production
npm test                 # Run tests
npm run eject            # Eject from Create React App
```

### Backend (`cd backend`)
```bash
./gradlew bootRun        # Start Spring Boot application
./gradlew build          # Build the project
./gradlew test           # Run tests
./gradlew eval           # Run evaluation suite
./gradlew flywayMigrate  # Run database migrations
```

## Performance Targets

- Query Performance: p95 ≤ 100ms for 90-day customer transactions
- Agent Latency: E2E triage decision ≤ 5s
- Data Scale: 1M+ transactions with efficient indexing
- Rate Limiting: 5 req/s per session

## Security & Compliance

- Automatic PII redaction in logs and UI
- API key authentication for mutating operations
- Role-based access control (Agent vs Lead)
- Content Security Policy and input validation

## Development

### Environment Variables

#### Backend Configuration
Create `backend/src/main/resources/application-local.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/aegis_support
    username: aegis_user
    password: aegis_password
  redis:
    host: localhost
    port: 6379
  profiles:
    active: local

logging:
  level:
    com.aegis: DEBUG
    org.springframework.web: DEBUG
```

#### Frontend Configuration
Create `frontend/.env.local`:
```env
REACT_APP_API_URL=http://localhost:8080
REACT_APP_WS_URL=ws://localhost:8080/ws
```

### Database Setup

```bash
# Initialize database with sample data
cd backend
./gradlew flywayMigrate

# Load test fixtures
./gradlew bootRun --args="--spring.profiles.active=local"
```

### API Testing

#### Health Check
```bash
curl http://localhost:8080/health
```

#### Triage Endpoint
```bash
curl -X POST http://localhost:8080/api/triage \
  -H "Content-Type: application/json" \
  -H "X-API-Key: test-key" \
  -d '{
    "customerId": "cust_001",
    "suspectTxnId": "txn_01001",
    "alertType": "fraud_alert",
    "userMessage": "Suspicious transaction detected"
  }'
```

#### Available Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | System health check |
| GET | `/metrics` | Prometheus metrics |
| POST | `/api/triage` | Main fraud triage endpoint |
| GET | `/api/customers/{id}` | Get customer details |
| GET | `/api/transactions/{id}` | Get transaction details |
| GET | `/api/evals` | Get evaluation results |
| POST | `/api/actions` | Execute agent actions |

#### Sample API Responses

**Triage Response:**
```json
{
  "riskScore": "high",
  "recommendedAction": "block_transaction",
  "reasons": ["unusual_location", "high_amount"],
  "requiresOTP": true,
  "fallbackUsed": false,
  "traceSteps": ["fraud_analysis", "policy_check", "risk_scoring"]
}
```

### Troubleshooting

#### Common Issues

1. **Port conflicts**: Ensure ports 3000, 8080, 5432, and 6379 are available
2. **Database connection**: Verify PostgreSQL is running and accessible
3. **Redis connection**: Check Redis server status
4. **CORS issues**: Ensure frontend proxy is configured correctly

#### Logs

```bash
# Backend logs
tail -f backend/logs/aegis-support.log

# Docker logs
docker compose logs -f backend
docker compose logs -f frontend
```

## Evaluation

Run the evaluation suite to verify system performance:

```bash
# From project root
npm run eval

# Or directly
node scripts/run-evals.js

# Using Gradle (from backend directory)
./gradlew eval
```

### Evaluation Test Cases

The evaluation suite includes 12 comprehensive test cases:

1. **Card Lost** - Customer reports lost card
2. **Unauthorized Charge** - Suspicious transaction detection
3. **Duplicate Charges** - Multiple identical transactions
4. **Geo Velocity** - Unusual location patterns
5. **Device MCC Anomaly** - Device-merchant category mismatch
6. **Chargeback Escalation** - High-risk chargeback scenarios
7. **Risk Timeout** - Agent response time limits
8. **Rate Limiting** - API throttling validation
9. **Policy Block** - Business rule enforcement
10. **PII Redaction** - Sensitive data protection
11. **KB FAQ** - Knowledge base integration
12. **Merchant Disambiguation** - Transaction categorization

### Performance Metrics

The evaluation measures:
- **Task Success Rate**: Percentage of correctly handled cases
- **Fallback Rate**: Frequency of fallback to default responses
- **Agent Latency**: Response time for triage decisions
- **P50/P95 Latency**: Performance percentiles
- **Risk Score Distribution**: Accuracy of risk assessment

## Documentation

- [Architecture Design](docs/ARCHITECTURE.md)
- [API Documentation](docs/API.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
- [ADR Decisions](docs/ADR.md)
- [Code Cleanup Summary](CODE_CLEANUP_SUMMARY.md)

## Recent Updates

### Code Optimization (Latest)
The system has been optimized for production readiness with comprehensive code cleanup:

- **Removed 8 unused classes** including TestController, PII redaction classes, and unused configurations
- **Eliminated 7 major dependencies** including Redis, WebFlux, Security, and unused monitoring libraries
- **Cleaned up frontend imports** and removed unused Material-UI components
- **Streamlined build configuration** for faster compilation and smaller JAR size
- **Maintained 100% test success rate** while reducing codebase complexity

See [CODE_CLEANUP_SUMMARY.md](CODE_CLEANUP_SUMMARY.md) for detailed information about the optimization changes.
