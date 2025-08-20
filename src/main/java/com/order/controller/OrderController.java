package com.order.controller;

import com.order.dto.OrderRequest;
import com.order.dto.OrderResponse;
import com.order.service.OrderServiceV1;
import com.order.service.OrderServiceV2;
import com.order.service.OrderServiceV3;
import com.order.service.OrderServiceV4;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@AllArgsConstructor
@Log4j2
public class OrderController {
    private final OrderServiceV1 orderServiceV1;
    private final OrderServiceV2 orderServiceV2;
    private final OrderServiceV3 orderServiceV3;
    private final OrderServiceV4 orderServiceV4;

    @PostMapping("/placeOrder/v1")
    public ResponseEntity<OrderResponse> saveOrder(@RequestBody OrderRequest orderRequest){
        log.info("Received order request: {}", orderRequest);
        OrderResponse orderResponse=orderServiceV1.orderResponseV1(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }

    @PostMapping("/placeOrder/v2")
    public ResponseEntity<OrderResponse> saveOrderAsync(@RequestBody OrderRequest orderRequest){
        OrderResponse orderResponse=orderServiceV2.orderResponseAsyncV2(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }

    @PostMapping("/placeOrder/v3")
    public ResponseEntity<OrderResponse> saveOrderV3(@RequestBody OrderRequest orderRequest){
        log.info("Received order request in orderV3: {}", orderRequest);
        OrderResponse orderResponse=orderServiceV3.orderResponseV3(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }

    @PostMapping("/placeOrder/v4")
    public ResponseEntity<OrderResponse> saveOrderV4(@RequestBody OrderRequest orderRequest){
        log.info("Received order request in orderV4: {}", orderRequest);
        OrderResponse orderResponse=orderServiceV4.orderResponseV4(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }
}
