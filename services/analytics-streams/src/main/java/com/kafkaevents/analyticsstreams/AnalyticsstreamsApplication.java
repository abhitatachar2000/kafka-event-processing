package com.kafkaevents.analyticsstreams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class AnalyticsstreamsApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnalyticsstreamsApplication.class, args);
	}

}
