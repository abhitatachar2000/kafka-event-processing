package com.kafkaevents.ordervalidator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class OrderValidatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderValidatorApplication.class, args);
	}

}
