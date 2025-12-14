## Inventory Service

The inventory service is responsible for updating the inventory based on the orders being placed. This service maintains a in-memory store of products and their stocks.The choice of in-memory is only for learning purposes.

The service reads the orders logs from `orders.validated` topic and for each order, reduces the stock of the corresponding product. 


Once there are fewer than 5 items of a product in the store, service sends out a restock notification event to the `inventory.restock` topic. 

If the stock depletes for the product, then the service discards the order and send the order to the `orders.rejected` topic for next actions. However, the next actions was not considered within the scope of the implementation.

You can find the configuration for the `invenvtory.updates`, `inventory.restock` and `orders.rejected` topics [here](https://github.com/abhitatachar2000/kafka-event-processing/blob/main/docs/topics.md). Each of these services are created with 6 partitions and a replication factor of 1 (since there is only one broker running locally).


The service can be started by executing:
```bash
mvn spring-boot:run
```
The service runs on port 8007¯.