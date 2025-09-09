# Aegis Support: Multi-Agent Banking Insights & Fraud Triage

A production-ready internal tool for care agents to analyze transactions, generate AI reports, and triage suspected fraud through a multi-agent pipeline.

## Quick Start

```bash
# 1. Start the system
docker compose up

# 2. Access the application
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# Metrics: http://localhost:8080/metrics
```

## Architecture Overview

- **Frontend**: React + TypeScript dashboard with real-time triage workflow
- **Backend**: Spring Boot microservices with multi-agent fraud detection
- **Database**: PostgreSQL with Redis caching and object storage
- **Agents**: Orchestrated AI agents for fraud analysis, compliance, and insights

## Key Features

- 🔍 **Transaction Analysis**: Upload and analyze customer transactions
- 🤖 **Multi-Agent Triage**: AI-powered fraud detection with explainable decisions
- 📊 **Real-time Dashboard**: KPIs, risk scoring, and agent performance metrics
- 🛡️ **Security**: PII redaction, rate limiting, and policy enforcement
- 📈 **Observability**: Comprehensive metrics, logging, and tracing
- ♿ **Accessibility**: WCAG-compliant UI with keyboard navigation

## Directory Structure

```
├── frontend/          # React TypeScript application
├── backend/           # Spring Boot microservices
├── fixtures/          # Test data and golden cases
├── scripts/           # Data generation and deployment
├── docs/              # Architecture and design documents
└── docker-compose.yml # Local development environment
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

## Evaluation

Run the evaluation suite to verify system performance:

```bash
npm run eval  # or ./scripts/run-evals.sh
```

## Documentation

- [Architecture Design](docs/ARCHITECTURE.md)
- [API Documentation](docs/API.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
- [ADR Decisions](docs/ADR.md)
