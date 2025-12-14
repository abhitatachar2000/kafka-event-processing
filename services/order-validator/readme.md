## Order Validator Service

The order validator service is responsible for validating the orders writen to `orders.raw`.
The service reads the logs from `orders.raw` and validates it based on the conditon: `quantity > 0 and price > 0`

If order is valid then writes a new event log into `orders.validated` and if the order is not valid then it is written into `orders.dlq`. A DLQ (dead letter queue) is used to log any invlaid orders.

The consumer is created with this configuration:
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:29092
    consumer:
      group-id: order-validator-group
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.kafkaevents.events
        partition.assignment.strategy: org.apache.kafka.clients.consumer.RoundRobinAssignor
```

- The consumer belongs to `order-validator-group`. If you look at the [OrderValidationConsumer.java](https://github.com/abhitatachar2000/kafka-event-processing/blob/main/services/order-validator/src/main/java/com/kafkaevents/ordervalidator/OrderValidationConsumer.java) you should see a `concurency` parameter being set in the kafka consumer. We maintain 6 concurrent consumers, each reading from one partition of the `orders.raw`.
- The `enable-auto-commit` is set to false. We only want to commit the offset once we have processed the event being read in the current round. The manual commit config is managed in the [KafkaConsumerConfig.java](https://github.com/abhitatachar2000/kafka-event-processing/blob/main/services/order-validator/src/main/java/com/kafkaevents/ordervalidator/KafkaConsumerConfig.java).


### Something I experimented and Learnt

Initially I was running the consumer-group with the configuration of 2 application instances and each instance had 3 concurrent threads acting as the consumer within the same consumer group. Meaning, there were 6 consumers with the consumer group `order-validator-group`.

The partition assignment strategy is set to use `org.apache.kafka.clients.consumer.RoundRobinAssignor`. So each thread received 1 partition (according to Kafka's rules, each consumer within the consumer group reads from only one partition at a time). So maximum parallelism achievable = numbers of partitions for a topic.

So, Initially application 1 logs said:
```log
2025-12-14 13:17:17.869 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.s.k.l.KafkaMessageListenerContainer - [] order-validator-group: partitions assigned: [orders.raw-0] 
2025-12-14 13:17:17.869 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.s.k.l.KafkaMessageListenerContainer - [] order-validator-group: partitions assigned: [orders.raw-3] 
2025-12-14 13:17:17.869 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.s.k.l.KafkaMessageListenerContainer - [] order-validator-group: partitions assigned: [orders.raw-4]
```

And application 2 logs said:
```log
2025-12-14 13:17:17.426 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.s.k.l.KafkaMessageListenerContainer - [] order-validator-group: partitions assigned: [orders.raw-5] 
2025-12-14 13:17:17.426 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.s.k.l.KafkaMessageListenerContainer - [] order-validator-group: partitions assigned: [orders.raw-1] 
2025-12-14 13:17:17.426 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.s.k.l.KafkaMessageListenerContainer - [] order-validator-group: partitions assigned: [orders.raw-2]
```

I crashed application 1 to see if there are any messages lost. But, what I observed was Kafka does something called `rebalacing`.
Once the three consumers (three threads from the first applications), the group coordinator automatically revoked the existing assignments on application two and assigned 2 partitions per consumer. The following were the logs:

```angular2html
2025-12-14 13:27:26.633 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-2, groupId=order-validator-group] Request joining group due to: group is already rebalancing 
2025-12-14 13:27:26.636 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.a.k.c.c.i.ConsumerRebalanceListenerInvoker - [] [Consumer clientId=consumer-order-validator-group-3, groupId=order-validator-group] Revoke previously assigned partitions orders.raw-5 
2025-12-14 13:27:26.636 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.a.k.c.c.i.ConsumerRebalanceListenerInvoker - [] [Consumer clientId=consumer-order-validator-group-2, groupId=order-validator-group] Revoke previously assigned partitions orders.raw-2 
2025-12-14 13:27:26.638 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.s.k.l.KafkaMessageListenerContainer - [] order-validator-group: partitions revoked: [orders.raw-2] 
2025-12-14 13:27:26.638 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.s.k.l.KafkaMessageListenerContainer - [] order-validator-group: partitions revoked: [orders.raw-5] 
2025-12-14 13:27:26.640 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-2, groupId=order-validator-group] (Re-)joining group 
2025-12-14 13:27:26.640 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-3, groupId=order-validator-group] (Re-)joining group 
2025-12-14 13:27:26.654 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO c.k.o.OrderValidationConsumer - [] Order with id 41 is valid 
2025-12-14 13:27:26.656 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-1, groupId=order-validator-group] Request joining group due to: group is already rebalancing 
2025-12-14 13:27:26.656 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.a.k.c.c.i.ConsumerRebalanceListenerInvoker - [] [Consumer clientId=consumer-order-validator-group-1, groupId=order-validator-group] Revoke previously assigned partitions orders.raw-1 
2025-12-14 13:27:26.656 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.s.k.l.KafkaMessageListenerContainer - [] order-validator-group: partitions revoked: [orders.raw-1] 
2025-12-14 13:27:26.657 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-1, groupId=order-validator-group] (Re-)joining group 
2025-12-14 13:27:26.659 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-3, groupId=order-validator-group] Successfully joined group with generation Generation{generationId=3, memberId='consumer-order-validator-group-3-f94a1039-78ca-472d-b402-faa18641214b', protocol='roundrobin'} 
2025-12-14 13:27:26.659 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-1, groupId=order-validator-group] Successfully joined group with generation Generation{generationId=3, memberId='consumer-order-validator-group-1-b9f34fbc-1ff4-4945-98a4-e2b08b5da22c', protocol='roundrobin'} 
2025-12-14 13:27:26.659 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-2, groupId=order-validator-group] Successfully joined group with generation Generation{generationId=3, memberId='consumer-order-validator-group-2-3576a6ad-8259-44a9-b9ea-e8e9b9c1bae8', protocol='roundrobin'} 
2025-12-14 13:27:26.666 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-3, groupId=order-validator-group] Finished assignment for group at generation 3: {consumer-order-validator-group-3-f94a1039-78ca-472d-b402-faa18641214b=Assignment(partitions=[orders.raw-2, orders.raw-5]), consumer-order-validator-group-2-3576a6ad-8259-44a9-b9ea-e8e9b9c1bae8=Assignment(partitions=[orders.raw-1, orders.raw-4]), consumer-order-validator-group-1-b9f34fbc-1ff4-4945-98a4-e2b08b5da22c=Assignment(partitions=[orders.raw-0, orders.raw-3])} 
2025-12-14 13:27:26.669 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-3, groupId=order-validator-group] Successfully synced group in generation Generation{generationId=3, memberId='consumer-order-validator-group-3-f94a1039-78ca-472d-b402-faa18641214b', protocol='roundrobin'} 
2025-12-14 13:27:26.669 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-2, groupId=order-validator-group] Successfully synced group in generation Generation{generationId=3, memberId='consumer-order-validator-group-2-3576a6ad-8259-44a9-b9ea-e8e9b9c1bae8', protocol='roundrobin'} 
2025-12-14 13:27:26.669 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-1, groupId=order-validator-group] Successfully synced group in generation Generation{generationId=3, memberId='consumer-order-validator-group-1-b9f34fbc-1ff4-4945-98a4-e2b08b5da22c', protocol='roundrobin'}
2025-12-14 13:27:26.669 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-3, groupId=order-validator-group] Notifying assignor about the new Assignment(partitions=[orders.raw-2, orders.raw-5]) 2025-12-14 13:27:26.669 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-1, groupId=order-validator-group] Notifying assignor about the new Assignment(partitions=[orders.raw-0, orders.raw-3]) 
2025-12-14 13:27:26.669 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.a.k.c.c.i.ConsumerRebalanceListenerInvoker - [] [Consumer clientId=consumer-order-validator-group-3, groupId=order-validator-group] Adding newly assigned partitions: orders.raw-2, orders.raw-5 
2025-12-14 13:27:26.669 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-2, groupId=order-validator-group] Notifying assignor about the new Assignment(partitions=[orders.raw-1, orders.raw-4]) 
2025-12-14 13:27:26.669 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.a.k.c.c.i.ConsumerRebalanceListenerInvoker - [] [Consumer clientId=consumer-order-validator-group-1, groupId=order-validator-group] Adding newly assigned partitions: orders.raw-0, orders.raw-3 
2025-12-14 13:27:26.670 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.a.k.c.c.i.ConsumerRebalanceListenerInvoker - [] [Consumer clientId=consumer-order-validator-group-2, groupId=order-validator-group] Adding newly assigned partitions: orders.raw-1, orders.raw-4 
2025-12-14 13:27:26.673 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.a.k.c.c.i.ConsumerCoordinator - [] [Consumer clientId=consumer-order-validator-group-2, groupId=order-validator-group] Found no committed offset for partition orders.raw-4 
2025-12-14 13:27:26.674 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.a.k.c.c.internals.ConsumerUtils - [] Setting offset for partition orders.raw-2 to the committed offset FetchPosition{offset=20, offsetEpoch=Optional.empty, currentLeader=LeaderAndEpoch{leader=Optional[localhost:29092 (id: 1 rack: null)], epoch=0}} 
2025-12-14 13:27:26.674 [org.springframework.kafka.KafkaListenerEndpointContainer#0-1-C-1] INFO o.a.k.c.c.internals.ConsumerUtils - [] Setting offset for partition orders.raw-1 to the committed offset FetchPosition{offset=22, offsetEpoch=Optional.empty, currentLeader=LeaderAndEpoch{leader=Optional[localhost:29092 (id: 1 rack: null)], epoch=0}} 
2025-12-14 13:27:26.674 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.a.k.c.c.internals.ConsumerUtils - [] Setting offset for partition orders.raw-5 to the committed offset FetchPosition{offset=14, offsetEpoch=Optional.empty, currentLeader=LeaderAndEpoch{leader=Optional[localhost:29092 (id: 1 rack: null)], epoch=0}} 
2025-12-14 13:27:26.674 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.a.k.c.c.internals.ConsumerUtils - [] Setting offset for partition orders.raw-0 to the committed offset FetchPosition{offset=19, offsetEpoch=Optional.empty, currentLeader=LeaderAndEpoch{leader=Optional[localhost:29092 (id: 1 rack: null)], epoch=0}} 
2025-12-14 13:27:26.674 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.a.k.c.c.internals.ConsumerUtils - [] Setting offset for partition orders.raw-3 to the committed offset FetchPosition{offset=16, offsetEpoch=Optional.empty, currentLeader=LeaderAndEpoch{leader=Optional[localhost:29092 (id: 1 rack: null)], epoch=0}} 
2025-12-14 13:27:26.674 [org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1] INFO o.s.k.l.KafkaMessageListenerContainer - [] order-validator-group: partitions assigned: [orders.raw-2, orders.raw-5] 
2025-12-14 13:27:26.674 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO o.s.k.l.KafkaMessageListenerContainer - [] order-validator-group: partitions assigned: [orders.raw-0, orders.raw-3] 
2025-12-14 13:27:26.863 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO c.k.o.OrderValidationConsumer - [] Order with id 42 is valid 
2025-12-14 13:27:26.959 [org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1] INFO c.k.o.OrderValidationConsumer - [] Order with id 44 is valid
```

And, it correctly identified the last committed offset and started reading the messages again. There was no loss of messgaes. Since the offset was only commited after the message was processed. So if the service was terminated even in between the processing, the message would be re-read by the new consumer and processed.

The service can be started by executing:
```bash
mvn spring-boot:run
```
The service runs on port 8006.
