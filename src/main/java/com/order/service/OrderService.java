package com.order.service;

import com.order.client.InventoryClient;
import com.order.client.PaymentClient;
import com.order.dto.*;
import com.order.repo.OrderRepo;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
public class OrderService {
    public OrderRepo orderRepo;
    public InventoryClient inventoryClient;
    public PaymentClient paymentClient;
    @SneakyThrows
    public OrderResponse orderResponse(OrderRequest orderRequest){
        List<CompletableFuture<InventoryResponse>> inventoryFutures=orderRequest.getProducts()
                .stream()
                .map(product->inventoryClient.getInventoryResponse(product.getProductName(),product.getQuantity())).toList();
        //wait all the operations to be completed
        CompletableFuture<Void> allInventoryDone = CompletableFuture.allOf(inventoryFutures.toArray(new CompletableFuture[0]));
        //collecting result Once done
        List<InventoryResponse> inventoryResponses=allInventoryDone.thenApply(v->{
            return inventoryFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        }).get();
        //calculating amount
        double totalPurchaseCost=inventoryResponses.stream()
                .map(InventoryResponse::getAmountPurchased)
                .mapToDouble(k->k)
                .sum();
        //calling payment service
        CompletableFuture<String> payment=paymentClient.getPaymentResponse(orderRequest.getPaymentMode(),totalPurchaseCost);
        //getting payment response
        String paymentStatus= payment.get();
        //generating order response
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
