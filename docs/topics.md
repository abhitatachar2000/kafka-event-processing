### Topics created

This documentation lists the configurations used for creating different kafka topics for reproducibility. 

### 1. orders.raw
```bash
docker exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic orders.raw \
  --partitions 6 \
  --replication-factor 1
```
### 2. orders.validated
```bash
docker exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic orders.validated \
  --partitions 6 \
  --replication-factor 1
```

### 3. orders.dlq
```bash
docker exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic orders.validated \
  --partitions 6 \
  --replication-factor 1
```
### 3. inventory.restock
```
docker exec kafka kafka-topics \
--bootstrap-server localhost:9092 \
--create \
--topic inventory.restock \
--partitions 6 \
--replication-factor 1
```

### 4. orders.rejected
```
docker exec kafka kafka-topics \
--bootstrap-server localhost:9092 \
--create \
--topic orders.rejected \
--partitions 6 \
--replication-factor 1
```

### 5. orders.rejected
```
docker exec kafka kafka-topics \
--bootstrap-server localhost:9092 \
--create \
--topic inventory.updates \
--partitions 6 \
--replication-factor 1
```