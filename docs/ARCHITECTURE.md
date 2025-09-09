# Aegis Support: Multi-Agent Banking System Architecture

## System Overview

Aegis Support is a multi-agent banking fraud detection and triage system designed for care agents to analyze transactions, generate AI reports, and handle suspicious activity alerts through an intelligent workflow.

## High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   Multi-Agent   │
│   (React)       │◄──►│   (Spring Boot) │◄──►│   System        │
│                 │    │                 │    │                 │
│ • Dashboard     │    │ • REST APIs     │    │ • Orchestrator  │
│ • Triage UI     │    │ • SSE Streaming │    │ • Fraud Agent   │
│ • Customer View │    │ • Rate Limiting │    │ • KB Agent      │
│ • Evals UI      │    │ • Auth/RBAC     │    │ • Compliance    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Observability │    │   Data Layer    │    │   Guardrails    │
│                 │    │                 │    │                 │
│ • Metrics       │    │ • PostgreSQL    │    │ • Retries       │
│ • Logging       │    │ • Redis Cache   │    │ • Timeouts      │
│ • Tracing       │    │ • Object Store  │    │ • Circuit Breaker│
│ • PII Redaction │    │ • Fixtures      │    │ • Schema Validation│
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Core Components

### 1. Frontend (React + TypeScript)
- **Dashboard**: KPIs, fraud triage table, filters
- **Customer View**: Transaction timeline, spend insights
- **Alerts Queue**: Risk scoring, triage workflow
- **Triage Drawer**: Multi-agent workflow visualization
- **Evals Interface**: Test results and metrics

### 2. Backend (Spring Boot + Java)
- **Ingestion Service**: Transaction loading and deduplication
- **Insights Service**: Spend analysis and reporting
- **Fraud Agent Service**: Multi-agent orchestration
- **Actions Service**: Card freeze, dispute creation
- **KB Service**: Knowledge base search
- **Evals Service**: Test execution and metrics

### 3. Multi-Agent System
- **Orchestrator**: Plans and coordinates agent workflow
- **Fraud Agent**: Risk assessment and scoring
- **Insights Agent**: Transaction analysis and categorization
- **KB Agent**: Knowledge base retrieval with citations
- **Compliance Agent**: Policy enforcement and validation
- **Redactor**: PII scrubbing and data protection
- **Summarizer**: Report generation

### 4. Data Architecture
- **PostgreSQL**: Primary data store with monthly partitioning
- **Redis**: Caching, rate limiting, work queues
- **Object Store**: Artifacts, reports, traces
- **Fixtures**: Test data and golden cases

## Key Design Decisions

### Performance
- **Database Partitioning**: Monthly partitions for transactions
- **Indexing Strategy**: (customerId, ts desc), (merchant), (mcc)
- **Caching**: Redis for frequently accessed data
- **Virtualization**: Frontend table virtualization for >2k rows

### Security
- **API Authentication**: X-API-Key for mutating operations
- **RBAC**: Agent vs Lead role separation
- **PII Protection**: Automatic redaction in logs and UI
- **Rate Limiting**: Token bucket with 5 req/s per session

### Reliability
- **Circuit Breakers**: 30s timeout after 3 consecutive failures
- **Retry Logic**: Exponential backoff (150ms, 400ms)
- **Fallbacks**: Deterministic rule-based alternatives
- **Idempotency**: Duplicate request protection

### Observability
- **Metrics**: Prometheus-compatible endpoints
- **Logging**: Structured JSON with request tracing
- **Tracing**: Human-readable agent execution traces
- **Health Checks**: Service availability monitoring

## Technology Stack

### Frontend
- React 18 + TypeScript
- Material-UI for components
- React Query for data fetching
- React Virtual for table virtualization
- WebSocket/SSE for real-time updates

### Backend
- Spring Boot 3.x
- Spring Security for authentication
- Spring Data JPA for database access
- Redis for caching and queues
- WebFlux for reactive streaming

### Database
- PostgreSQL 15+ with partitioning
- Redis 7+ for caching
- Flyway for migrations

### DevOps
- Docker Compose for local development
- Gradle for build management
- JUnit 5 for testing
- Mockito for mocking

## Data Flow

1. **Transaction Ingestion**: CSV/fixture data → validation → deduplication → storage
2. **Fraud Detection**: Alert trigger → multi-agent analysis → risk scoring → action recommendation
3. **Triage Workflow**: Agent review → policy validation → action execution → audit trail
4. **Insights Generation**: Transaction analysis → categorization → reporting → dashboard update

## Performance Targets

- **Query Performance**: p95 ≤ 100ms for 90-day customer transactions
- **Agent Latency**: E2E triage decision ≤ 5s
- **Throughput**: 5 req/s per session with rate limiting
- **Data Scale**: Support 1M+ transactions with efficient indexing

## Security Considerations

- **PII Redaction**: Automatic masking of sensitive data
- **Input Validation**: Schema validation for all inputs
- **Prompt Injection Defense**: Sanitized agent inputs
- **Audit Logging**: Complete action trail with redacted PII
- **CSP**: Content Security Policy for XSS protection
