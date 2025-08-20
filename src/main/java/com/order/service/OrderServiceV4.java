package com.order.service;


import com.order.asyncClient.InventoryAsyncClient;
import com.order.asyncClient.PaymentAsyncClient;
import com.order.dto.InventoryResponse;
import com.order.dto.OrderRequest;
import com.order.dto.OrderResponse;
import com.order.repo.OrderRepo;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
@Log4j2
public class OrderServiceV4 {
    private final OrderRepo orderRepo;
    private final InventoryAsyncClient inventoryAsyncClient;
    private final PaymentAsyncClient paymentAsyncClient;

    @SneakyThrows
    public OrderResponse orderResponseV4(OrderRequest orderRequest){
        //check inventory and grab totalCost
        List<CompletableFuture<InventoryResponse>> inventoryFutures = checkProductAvailability(orderRequest);
        CompletableFuture<Void> futures=CompletableFuture.allOf(inventoryFutures.toArray(new CompletableFuture[0]));
        List<InventoryResponse> inventoryResponses=futures.thenApply(v->
            inventoryFutures.stream()
                    .map(CompletableFuture::join)
                    .toList()).get();
        //calculate total Amount and send to payment service
        double totalPurchaseAmount=inventoryResponses.stream().map(InventoryResponse::getAmountPurchased).mapToDouble(Double::doubleValue).sum();
        String paymentStatus=getPaymentResponse(orderRequest.getPaymentMode(),totalPurchaseAmount);
        //update inventory and return orderResponse
        List<CompletableFuture<InventoryResponse>> updateInventoryResponseFuture=updateInventory(orderRequest);
        CompletableFuture<Void> updateFutures=CompletableFuture.allOf(updateInventoryResponseFuture.toArray(new CompletableFuture[0]));
        List<InventoryResponse> updateResponse=updateFutures.thenApply(v->
                updateInventoryResponseFuture.stream()
                        .map(CompletableFuture::join).toList()).get();
        //save and return order response
        OrderResponse orderResponse = OrderResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .paymentStatus("SUCCESS")
                .products(orderRequest.getProducts())
                .paymentMode(orderRequest.getPaymentMode())
                .purchaseAmount(totalPurchaseAmount)
                .userName(orderRequest.getUserName())
                .build();
        orderRepo.save(orderResponse);
        return orderResponse;
    }

    private List<CompletableFuture<InventoryResponse>> checkProductAvailability(OrderRequest orderRequest){
        return orderRequest.getProducts()
                .stream()
                .map(item -> inventoryAsyncClient.checkInventory(item.getProductName(), item.getQuantity())).toList();
    }

    @SneakyThrows
    private String getPaymentResponse(String paymentMode,double amount){
        return paymentAsyncClient.getPaymentResponse(paymentMode,amount).get();
    }

    private List<CompletableFuture<InventoryResponse>> updateInventory(OrderRequest orderRequest) {
        return orderRequest.getProducts()
                .stream()
                .map(item->inventoryAsyncClient.updateInventory(item.getProductName(),item.getQuantity())).toList();
    }

}
