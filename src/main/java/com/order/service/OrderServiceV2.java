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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
@Log4j2
public class OrderServiceV2 {

    private final OrderRepo orderRepo;
    private final InventoryAsyncClient inventoryAsyncClient;
    private final PaymentAsyncClient paymentAsyncClient;

    @SneakyThrows
    public OrderResponse orderResponseAsyncV2(OrderRequest orderRequest) {
        // STEP 1: Check Inventory for each product
        List<CompletableFuture<InventoryResponse>> checkInventoryFutures = orderRequest.getProducts().stream()
                .map(product -> inventoryAsyncClient.checkInventory(product.getProductName(), product.getQuantity()))
                .toList();

        // Wait for all futures to complete
        CompletableFuture<Void> allInventoryChecks = CompletableFuture.allOf(checkInventoryFutures.toArray(new CompletableFuture[0]));
        List<InventoryResponse> checkInventoryResponses = allInventoryChecks
                .thenApply(v -> checkInventoryFutures.stream().map(CompletableFuture::join).toList())
                .get();

        boolean anyInventoryError = checkInventoryResponses.stream()
                .anyMatch(Objects::isNull);

        if (anyInventoryError) {
            log.warn("OrderServiceV2 :: One or more inventory checks failed. Failing order.");
            return OrderResponse.builder()
                    .transactionId(UUID.randomUUID().toString())
                    .paymentStatus("FAILED")
                    .products(orderRequest.getProducts())
                    .paymentMode(orderRequest.getPaymentMode())
                    .purchaseAmount(0.0)
                    .userName(orderRequest.getUserName())
                    .exceptionMessage("Order failed: One or more products exceed available quantity.")
                    .build();
        }

        // STEP 2: Calculate total cost and initiate payment
        double totalCost = checkInventoryResponses.stream()
                .mapToDouble(InventoryResponse::getAmountPurchased)
                .sum();

        CompletableFuture<String> paymentFuture = paymentAsyncClient.getPaymentResponse(orderRequest.getPaymentMode(), totalCost);

        String paymentStatus = paymentFuture.get();  // safe since exception is already thrown from client

        if (!"payment success".equalsIgnoreCase(paymentStatus)) {
            return OrderResponse.builder()
                    .transactionId(UUID.randomUUID().toString())
                    .paymentStatus("FAILED")
                    .products(orderRequest.getProducts())
                    .paymentMode(orderRequest.getPaymentMode())
                    .purchaseAmount(0.0)
                    .userName(orderRequest.getUserName())
                    .exceptionMessage("Order failed: Payment unsuccessful.")
                    .build();
        }

        // STEP 3: Update Inventory after successful payment
        List<CompletableFuture<InventoryResponse>> updateInventoryFutures = orderRequest.getProducts().stream()
                .map(product -> inventoryAsyncClient.updateInventory(product.getProductName(), product.getQuantity()))
                .toList();

        CompletableFuture.allOf(updateInventoryFutures.toArray(new CompletableFuture[0])).get();

        // STEP 4: Save successful order
        OrderResponse orderResponse = OrderResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .paymentStatus("SUCCESS")
                .products(orderRequest.getProducts())
                .paymentMode(orderRequest.getPaymentMode())
                .purchaseAmount(totalCost)
                .userName(orderRequest.getUserName())
                .build();

        orderRepo.save(orderResponse);
        return orderResponse;
    }
}
