## Analytics service

The analytics service makes uses of Kafka streams (KStreams and KTables) to stream events from the Kafka broker and generates `count`, `aggregates`, and `filters`.

For analytics three metrics are being generated:

1. Orders per product per day `ORDERS_PER_PRODUCT_DAILY` - Reads the events from `orders.validated` topic, groups by key (productId), windows by 1 day duration and counts the number of events.
2. Revenue per product per day `REVENUE_PER_PRODUCT_DAILY` - Reads the event from `orders.accepted` topic, groups by key (productId), and creates an aggregation to add the prices of the order.
3. Out of stock product table `STOCK_EMPTY` - Creates a KTable by streaming events from `inventory.updates` and filters those products whose stock is 0.

These metrics are streamed to an output topic `metrics.output`.

The following is the streams configuration:
```yaml
spring:
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    streams:
      application-id: analytics-streams-app
      properties:
        processing.guarantee: exactly_once_v2
        default.key.serde: org.apache.kafka.common.serialization.Serdes$StringSerde
        default.value.serde: org.springframework.kafka.support.serializer.JsonSerde
```

Kafka Streams by default guarantees that every process is processed at least once.
Based on config, it can also be made to process exactly once. Here the processing guarantee is set to exactly once. 

The `application-id` is used to create the consumer group and for state management. 

### My Learning

#### How are Kafka Streams are better than any regular Kafka consumers

Kafka consumers are stateless, meaning they do not persist the state of the event once after processing.
For analytics applications, it is important to maintain the state of the events, for example, summing up the prices of the sales made.

If we were to use the regular Kafka consumer for analytics applications then, we need to manually maintain:

- State (DB reads and writes)
- At least once and exactly once processing guarantee.
- Rebalancing between consumer.
- Committing offsets.
- Performing analytics operations like filtering, aggregations, joins, groupBy, windowedBy.

Kafka streams solves these problems. Kafka streams are useful in use-cases where a continuous stream of events are to be read and needs to perform transformation on these events in realtime, and the output needs to be written to a different topic.

What Kafka Stream does:
1. Reads the events from a input topic.
2. Performs real time transformation - filter, groupBy, windowedBy, aggregate, count, joins.
3. Writes the events to a output topic. 

Kafka streams takes care of:
1. Creating consumer groups and stream threads for consumers.
2. Polling the kafka broker for topics, and reading from partitions.
3. Managing state.
4. Ensuring at-least once and exactly once processing.
5. Managing and commiting offset.

When should Kafka Streams be used:
1. Real time data transformation. Eg. Adding user metadata to clickstream events.
2. Real time analytics and aggregations. Eg. Calculating orders per min, revenue per day.
3. Combining tables with streams. Eg. Orders stream joined to user tables.
4. Fraud detection - sliding window aggregations, and pattern detection.
5. ETL pipelines
6. Data enrichment pipelines - combine two topics into richer output topic.