package com.order.service;

import com.order.client.InventoryClient;
import com.order.client.PaymentClient;
import com.order.dto.InventoryResponse;
import com.order.dto.OrderRequest;
import com.order.dto.OrderResponse;
import com.order.repo.OrderRepo;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import java.util.*;

@AllArgsConstructor
@Service
@Log4j2
public class OrderServiceV3 {
    private final OrderRepo orderRepo;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    public OrderResponse orderResponseV3(OrderRequest orderRequest) {
        //check inventory and grab totalCost
        List<InventoryResponse> inventoryResponses=checkInventory(orderRequest);
        //process the payment
        Pair<String,Double> paymentResponse=getPaymentResponse(inventoryResponses,orderRequest);
        //update the inventory
        updateInventory(orderRequest);
        //return the order Response
        OrderResponse successResponse = OrderResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .paymentStatus(paymentResponse.getLeft())
                .products(orderRequest.getProducts())
                .paymentMode(orderRequest.getPaymentMode())
                .purchaseAmount(paymentResponse.getRight())
                .userName(orderRequest.getUserName())
                .build();
        orderRepo.save(successResponse);
        return successResponse;
    }

    //check the availability
    private List<InventoryResponse> checkInventory(OrderRequest orderRequest){
        return orderRequest.getProducts()
                .stream()
                .map(product->inventoryClient.checkInventory(product.getProductName(),product.getQuantity()))
                .toList();
    }

    //process the payment
    //to da use pair to return status and amount
    private Pair<String,Double> getPaymentResponse(List<InventoryResponse> inventoryResponses, OrderRequest orderRequest) {
        double totalAmountPurchased=inventoryResponses.stream().map(InventoryResponse::getAmountPurchased).mapToDouble(Double::doubleValue).sum();
        String paymentStatus= paymentClient.getPaymentResponse(orderRequest.getPaymentMode(),totalAmountPurchased);
        return Pair.of(paymentStatus,totalAmountPurchased);
    }

    //update the inventory
    private void updateInventory(OrderRequest orderRequest){
         orderRequest.getProducts()
                 .forEach(product -> inventoryClient.updateInventory(product.getProductName(),product.getQuantity()));
    }

}
