# Kafka Event Processing System

## Problem and Use Case

In large-scale commerce systems, orders and inventory updates must be processed reliably and in real time, while multiple downstream systems (notification, inventory, analytics) consume the same data independently.

The system must:
- Handle high-throughput order traffic
- Maintain consistent inventory state
- Generate real-time business metrics
- Be resilient to failures and consumer slowdowns

Apache Kafka is used as the central event backbone, with Kafka Streams powering real-time analytics.

### Key Problems with Queues and Push-Based Systems

Traditional queue-based systems and push-based architectures have some limitations in event-driven architectures:

- **Client Availability Dependency**: Push-based systems require clients to be online and actively listening at all times. If a consumer service is down or experiencing issues, events can be lost or delayed indefinitely.
- **Processing Capability Constraints**: Clients must have the capacity to handle incoming events immediately. If a consumer is overwhelmed or slow, it can cause backpressure, leading to dropped messages or system-wide slowdowns.
- **Tight Coupling**: Push systems create tight coupling between producers and consumers, making it difficult to add new consumers or modify existing ones without affecting the entire system.
- **Scalability Challenges**: Scaling consumers in push-based systems is complex and often requires coordination between producers and consumers.
- **Reliability Issues**: Without proper buffering and retry mechanisms, transient failures can result in lost events.

Kafka's pull-based model addresses these issues by allowing consumers to pull events at their own pace, maintain their own offsets, and operate independently of producers.

## Project Architecture

This project is a monorepo containing a microservices-based event processing system using Apache Kafka as the central messaging backbone. The system consists of five main services that work together to process orders, manage inventory, and generate real-time analytics.

### Services and Responsibilities

1. **Order Producer** (Port: 8005)
   - Generates high-throughput mock order events
   - Publishes orders to the `orders.raw` topic
   - Provides REST API for controlling order generation rate and duration

2. **Order Validator**
   - Consumes raw orders from `orders.raw` topic
   - Validates orders based on business rules (quantity > 0 and price > 0)
   - Publishes valid orders to `orders.validated` topic
   - Publishes invalid orders to `orders.dlq` (Dead Letter Queue)

3. **Inventory Service** (Port: 8007)
   - Consumes validated orders from `orders.validated` topic
   - Maintains in-memory inventory state
   - Updates stock levels based on order processing
   - Publishes accepted orders to `orders.accepted` topic
   - Publishes rejected orders (insufficient stock) to `orders.rejected` topic
   - Publishes restock notifications to `inventory.restock` topic when stock < 5
   - Publishes inventory updates to `inventory.updates` topic

4. **Analytics Streams**
   - Uses Kafka Streams for real-time analytics processing
   - Generates three key metrics:
     - **Orders Per Product Per Day**: Counts orders per product from `orders.validated` topic
     - **Revenue Per Product Per Day**: Aggregates revenue from `orders.accepted` topic
     - **Out of Stock Products**: Tracks products with zero stock from `inventory.updates` topic
   - Publishes metrics to `metrics.output` topic

5. **Metrics API** (Port: 8009)
   - Consumes metrics from `metrics.output` topic
   - Maintains in-memory metrics store
   - Provides REST API for querying real-time metrics

### Component Diagram

```plantuml
@startuml Component Diagram
!define RECTANGLE class

RECTANGLE "Order Producer" as OP #lightblue
RECTANGLE "Order Validator" as OV #lightgreen
RECTANGLE "Inventory Service" as INV #lightyellow
RECTANGLE "Analytics Streams" as AS #lightcoral
RECTANGLE "Metrics API" as MA #lightgray

database "Kafka Topics" as KT {
  rectangle "orders.raw" as OR
  rectangle "orders.validated" as OVAL
  rectangle "orders.dlq" as ODLQ
  rectangle "orders.accepted" as OACC
  rectangle "orders.rejected" as OREJ
  rectangle "inventory.updates" as IUPD
  rectangle "inventory.restock" as IREST
  rectangle "metrics.output" as MOUT
}

OP --> OR : produces
OV --> OR : consumes
OV --> OVAL : produces (valid)
OV --> ODLQ : produces (invalid)

INV --> OVAL : consumes
INV --> OACC : produces
INV --> OREJ : produces
INV --> IREST : produces
INV --> IUPD : produces

AS --> OVAL : consumes (ORDERS_PER_PRODUCT_PER_DAY)
AS --> IUPD : consumes (REVENUE_PER_PRODUCT_PER_DAY, STOCK_EMPTY)
AS --> MOUT : produces

MA --> MOUT : consumes

note right of OP
  Generates mock orders
  REST API: /api/v1/orders/generate
end note

note right of AS
  Kafka Streams processing
  Real-time aggregations
end note

note right of MA
  REST API: /api/v1/metrics
  In-memory metrics store
end note
@enduml
```

### Service Interactions

1. **Order Flow**:
   - Order Producer generates and publishes orders to `orders.raw`
   - Order Validator consumes and validates orders
   - Valid orders go to `orders.validated`, invalid to `orders.dlq`

2. **Inventory Processing**:
   - Inventory Service processes validated orders
   - Updates internal stock state
   - Publishes order outcomes (accepted/rejected)
   - Publishes inventory updates and restock alerts

3. **Analytics Processing**:
   - Analytics Streams consumes order and inventory events
   - Performs real-time aggregations using Kafka Streams
   - Publishes computed metrics

4. **Metrics Serving**:
   - Metrics API consumes and caches metrics
   - Serves metrics via REST endpoints

### Kafka Topics

The system uses the following Kafka topics (all configured with 6 partitions and replication factor 1):

| Topic | Purpose | Producer | Consumer(s) |
|-------|---------|----------|-------------|
| `orders.raw` | Raw order events | Order Producer | Order Validator |
| `orders.validated` | Validated orders | Order Validator | Inventory Service, Analytics Streams |
| `orders.dlq` | Dead letter queue for invalid orders | Order Validator | - |
| `orders.accepted` | Successfully processed orders | Inventory Service | - |
| `orders.rejected` | Orders rejected due to insufficient stock | Inventory Service | - |
| `inventory.updates` | Inventory state changes | Inventory Service | Analytics Streams |
| `inventory.restock` | Restock alerts | Inventory Service | - |
| `metrics.output` | Computed analytics metrics | Analytics Streams | Metrics API |

For detailed topic creation commands, see [docs/topics.md](docs/topics.md).

## Running the Services

### Prerequisites

- Docker and Docker Compose
- Java 25+ (for local development)
- Maven 3.6+ (for local development)

### Using Docker Compose

1. **Start all services**:
   ```bash
   docker-compose up -d
   ```

2. **Create topics** (see [docs/topics.md](docs/topics.md))

3. **Verify services are running**:
   ```bash
   docker-compose ps
   ```

4. **View Kafka UI**:
   - Open http://localhost:8080 in your browser
   - Connect to the `local` cluster

5. **Stop services**:
   ```bash
   docker-compose down
   ```

### Generating High-Throughput Orders

Use the Order Producer service to generate test orders:

```bash
curl -X POST "http://localhost:8005/api/v1/orders/generate?rate=1000&duration=10"
```

- `rate`: Orders per second (e.g., 1000)
- `duration`: Duration in seconds (e.g., 10)

This will generate 10,000 orders over 10 seconds at a rate of 1000 orders/second.

### Viewing Metrics
Query the Metrics API for real-time analytics:

```bash
# Orders per minute by product
curl -X GET "http://localhost:8009/api/v1/metrics?metricName=ordersPerProductPerDay"

# Revenue per product per day
curl -X GET "http://localhost:8009/api/v1/metrics?metricName=revenuePerProductPerDay"

# Out of stock products
curl -X GET "http://localhost:8009/api/v1/metrics?metricName=outOfStockProducts"
```

Available metric names:
- `ordersPerMinute`: Orders aggregated by product ID
- `revenuePerProductPerDay`: Revenue aggregated by product
- `outOfStockProducts`: Products with zero stock

## Technologies Used

- **Apache Kafka**: Event streaming platform
- **Kafka Streams**: Stream processing library
- **Spring Boot**: Java framework for microservices
- **Docker**: Containerization
- **Docker Compose**: Multi-container orchestration
- **Java 25**: Programming language
- **Maven**: Build tool

## Development

### Local Development Setup

1. **Start Kafka infrastructure**:
   ```bash
   docker-compose up kafka kafka-ui -d
   ```

2. **Create topics** (see [docs/topics.md](docs/topics.md))

3. **Run services individually**:
   ```bash
   cd services/<service-name>
   mvn spring-boot:run
   ```

### Service Ports

- Order Producer: 8005
- Order Validator: 8006
- Inventory Service: 8007
- Metrics API: 8009
- Kafka UI: 8080
- Kafka Broker: 9092

## Monitoring and Observability

- **Kafka UI**: Web interface for Kafka cluster management at http://localhost:8080
- **Application Logs**: Each service logs to console and can be monitored via Docker logs.
- **Metrics API**: REST endpoints for real-time business metrics.
