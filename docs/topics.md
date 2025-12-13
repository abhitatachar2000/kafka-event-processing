### Topics created

This documentation lists the configurations used for creating different kafka topics for reproducibility. 

#### 1. Orders.raw
```angular2html
docker exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic orders.raw \
  --partitions 6 \
  --replication-factor 1
```