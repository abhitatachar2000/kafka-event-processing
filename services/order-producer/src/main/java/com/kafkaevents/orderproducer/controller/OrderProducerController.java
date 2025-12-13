package com.kafkaevents.orderproducer.controller;

import com.kafkaevents.orderproducer.service.OrdersGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderProducerController {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private OrdersGeneratorService ordersGeneratorService;


    @Autowired
    public OrderProducerController(OrdersGeneratorService ordersGeneratorService) {
        this.ordersGeneratorService = ordersGeneratorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateOrders(@RequestParam Integer rate, @RequestParam(defaultValue = "10") Integer duration) {
        try {
            logger.info(("New request recieved for generating orders."));
            ordersGeneratorService.generate(rate, duration);
            return ResponseEntity.status(HttpStatus.OK).body(String.format("Generating new orders for %s s at a speed of %s orders per second", duration, rate));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
