package com.order.controller;

import com.order.dto.OrderRequest;
import com.order.dto.OrderResponse;
import com.order.service.OrderServiceV1;
import com.order.service.OrderServiceV2;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@AllArgsConstructor
public class OrderController {
    OrderServiceV1 orderServiceV1;
    OrderServiceV2 orderServiceV2;

    @PostMapping("/placeOrder/v1")
    public ResponseEntity<OrderResponse> saveOrder(@RequestBody OrderRequest orderRequest){
        OrderResponse orderResponse=orderServiceV1.orderResponseV1(orderRequest);
        return ResponseEntity.ok(orderResponse);
    }

    @PostMapping("/placeOrder/v2")
    public ResponseEntity<OrderResponse> saveOrderAsync(@RequestBody OrderRequest orderRequest){
        OrderResponse orderResponse=orderServiceV2.orderResponseAsyncV2(orderRequest);
        return ResponseEntity.ok(orderResponse);
    }
}
