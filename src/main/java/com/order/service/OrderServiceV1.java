package com.order.service;

import com.order.client.InventoryClient;
import com.order.client.PaymentClient;
import com.order.dto.InventoryResponse;
import com.order.dto.OrderRequest;
import com.order.dto.OrderResponse;
import com.order.repo.OrderRepo;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderServiceV1 {
    public OrderRepo orderRepo;
    public InventoryClient inventoryClient;
    public PaymentClient paymentClient;
    @SneakyThrows
    public OrderResponse orderResponseV1(OrderRequest orderRequest){
        List<InventoryResponse> inventoryResponses=orderRequest.getProducts()
                .stream()
                .map(product-> inventoryClient.getInventoryResponse(product.getProductName(),product.getQuantity())).toList();
        //calculating amount
        double totalPurchaseCost=inventoryResponses.stream()
                .map(InventoryResponse::getAmountPurchased)
                .mapToDouble(k->k)
                .sum();
        String paymentStatus= paymentClient.getPaymentResponse(orderRequest.getPaymentMode(),totalPurchaseCost);
        OrderResponse orderResponse=OrderResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .paymentStatus(paymentStatus)
                .products(orderRequest.getProducts())
                .paymentMode(orderRequest.getPaymentMode())
                .purchaseAmount(totalPurchaseCost)
                .userName(orderRequest.getUserName())
                .build();
        orderRepo.save(orderResponse);
        return orderResponse;
    }
}
