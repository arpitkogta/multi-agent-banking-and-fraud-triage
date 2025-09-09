# System Architecture Diagram

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Multi-Agent Banking System                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐ │
│  │    Frontend     │    │    Backend      │    │   Database   │ │
│  │   (React)       │◄──►│  (Spring Boot)  │◄──►│ (PostgreSQL) │ │
│  │   Port: 3000    │    │   Port: 8080    │    │  Port: 5432  │ │
│  └─────────────────┘    └─────────────────┘    └──────────────┘ │
│           │                       │                       │     │
│           │                       │                       │     │
│           │              ┌─────────────────┐              │     │
│           │              │     Redis       │              │     │
│           │              │   (Cache)       │              │     │
│           │              │   Port: 6379    │              │     │
│           │              └─────────────────┘              │     │
│           │                       │                       │     │
│           │              ┌─────────────────┐              │     │
│           │              │   Prometheus    │              │     │
│           │              │  (Monitoring)   │              │     │
│           │              │   Port: 9090    │              │     │
│           │              └─────────────────┘              │     │
└─────────────────────────────────────────────────────────────────┘
```

## Agent Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Agent Orchestrator                          │
│                   (Main Controller)                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │
│  │ ProfileAgent│  │Transaction  │  │  RiskAgent  │  │   KB    │ │
│  │             │  │   Agent     │  │             │  │  Agent  │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────┘ │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │  Merchant   │  │ Compliance  │  │  Specialized│             │
│  │Disambiguation│  │   Agent     │  │  Workflows  │             │
│  │   Agent     │  │             │  │             │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

## Workflow Routing Logic

```
Request → Alert Type Detection
    │
    ├─ card_lost → Card Lost Workflow
    ├─ unauthorized_charge → Unauthorized Charge Workflow  
    ├─ duplicate_charge → Duplicate Charge Workflow
    ├─ geo_velocity → Geo-Velocity Workflow
    ├─ chargeback_history → Chargeback Escalation Workflow
    ├─ kb_faq → KB FAQ Workflow
    ├─ merchant_disambiguation → Merchant Disambiguation Workflow
    └─ standard → Standard Triage Workflow
```

## Data Flow

```
1. Frontend Request
   ↓
2. Backend API (/api/triage)
   ↓
3. Agent Orchestrator
   ↓
4. Workflow Detection
   ↓
5. Specialized Workflow Execution
   ↓
6. Agent Coordination
   ↓
7. Response Building
   ↓
8. Frontend Display
```

## Workflow Steps (Example: Card Lost)

```
Step 1: Get Profile (ProfileAgent)
   ↓
Step 2: Get Recent Transactions (TransactionAgent)
   ↓
Step 3: Risk Assessment (RiskAgent)
   ↓
Step 4: Knowledge Base Lookup (KnowledgeBaseAgent)
   ↓
Step 5: Decision Making (Orchestrator)
   ↓
Step 6: Action Execution (Orchestrator)
   ↓
Response: Risk Score + Action + Trace Data
```

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Build**: Gradle
- **Container**: Docker

### Frontend
- **Framework**: React 18
- **Language**: JavaScript/TypeScript
- **Build**: Webpack
- **Server**: Nginx
- **Container**: Docker

### Infrastructure
- **Orchestration**: Docker Compose
- **Monitoring**: Prometheus
- **Logging**: SLF4J + Logback
- **Security**: Spring Security

## API Endpoints

```
POST /api/triage              - Main triage endpoint
GET  /api/customer/{id}       - Customer profile
GET  /api/customer/{id}/txns  - Customer transactions
GET  /api/kb/search           - Knowledge base search
POST /api/ingest/fixtures     - Load test data
GET  /api/actuator/health     - Health check
GET  /api/actuator/metrics    - Metrics
```

## Performance Characteristics

- **Average Latency**: 196.42ms
- **P50 Latency**: 101ms
- **P95 Latency**: 1074ms
- **Success Rate**: 100%
- **Throughput**: 5 requests/second (rate limited)
- **Concurrent Users**: 10+ (with rate limiting)
