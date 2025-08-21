package com.order.service;

import com.order.asyncClient.InventoryAsyncClient;
import com.order.asyncClient.PaymentAsyncClient;
import com.order.dto.InventoryResponse;
import com.order.dto.OrderRequest;
import com.order.dto.OrderResponse;
import com.order.exceptions.inventoryExceptions.ProductException;
import com.order.exceptions.orderExceptions.OrderServiceException;
import com.order.exceptions.paymentExceptions.PaymentException;
import com.order.repo.OrderRepo;
import com.order.util.JsonConverter;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@AllArgsConstructor
@Log4j2
public class OrderServiceV2 {

    private final OrderRepo orderRepo;
    private final InventoryAsyncClient inventoryAsyncClient;
    private final PaymentAsyncClient paymentAsyncClient;

    public OrderResponse orderResponseAsyncV2(OrderRequest orderRequest) {
        List<InventoryResponse> checkInventoryResponses = checkInventoryForAllProducts(orderRequest);

        double totalCost = calculateTotalCost(checkInventoryResponses);

        String paymentStatus = processPayment(orderRequest, totalCost);

        updateInventoryAfterPayment(orderRequest);

        return saveSuccessfulOrder(orderRequest, paymentStatus, totalCost);
    }

    private List<InventoryResponse> checkInventoryForAllProducts(OrderRequest orderRequest) {
        List<CompletableFuture<InventoryResponse>> checkInventoryFutures = orderRequest.getProducts().stream()
                .map(product -> inventoryAsyncClient.checkInventory(product.getProductName(), product.getQuantity()))
                .toList();

        CompletableFuture<Void> allInventoryChecks = CompletableFuture.allOf(checkInventoryFutures.toArray(new CompletableFuture[0]));
        try {
            return allInventoryChecks
                    .thenApply(v -> checkInventoryFutures.stream().map(CompletableFuture::join).toList())
                    .get();
        } catch (ExecutionException ex) {
            log.error("Exception caught at inventory logic {}", ex.getMessage());
            Throwable cause = ex.getCause();
            if (cause instanceof ProductException exception) {
                log.error("Exception caught at inventory service {}", cause.getMessage());
                throw buildFailedOrderResponse(exception,orderRequest,"order failed due to inventory service");
            }
            throw new OrderServiceException(500, "Internal error: " + cause.getMessage());
        } catch (Exception ex) {
            throw new OrderServiceException(500, "Unexpected error: " + ex.getMessage());
        }
    }

    private double calculateTotalCost(List<InventoryResponse> checkInventoryResponses) {
        double totalCost = checkInventoryResponses.stream()
                .mapToDouble(InventoryResponse::getAmountPurchased)
                .sum();
        log.info("Total purchase cost: {}", totalCost);
        return totalCost;
    }

    private String processPayment(OrderRequest orderRequest, double totalCost) {
        CompletableFuture<String> paymentFuture = paymentAsyncClient.getPaymentResponse(orderRequest.getPaymentMode(), totalCost);
        try {
            return paymentFuture.get();
        } catch (ExecutionException ex) {
            log.error("Exception caught at payment logic {}", ex.getMessage());
            Throwable cause = ex.getCause();
            if (cause instanceof PaymentException exception) {
                log.error("Exception caught at payment service {}", cause.getMessage());
                throw buildFailedOrderResponse(exception,orderRequest,"order failed due to payment service");
            }
            throw new OrderServiceException(500, "Internal error: " + cause.getMessage());
        } catch (Exception ex) {
            throw new OrderServiceException(500, "Unexpected error: " + ex.getMessage());
        }
    }

    private void updateInventoryAfterPayment(OrderRequest orderRequest) {
        List<CompletableFuture<InventoryResponse>> updateInventoryFutures = orderRequest.getProducts().stream()
                .map(product -> inventoryAsyncClient.updateInventory(product.getProductName(), product.getQuantity()))
                .toList();
        try {
            CompletableFuture.allOf(updateInventoryFutures.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private OrderResponse saveSuccessfulOrder(OrderRequest orderRequest, String paymentStatus, double totalCost) {
        OrderResponse orderResponse = OrderResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .paymentStatus(paymentStatus)
                .products(orderRequest.getProducts())
                .paymentMode(orderRequest.getPaymentMode())
                .purchaseAmount(totalCost)
                .userName(orderRequest.getUserName())
                .build();
        orderRepo.save(orderResponse);
        return orderResponse;
    }

    private OrderServiceException buildFailedOrderResponse(Exception exception,OrderRequest orderRequest, String paymentStatus) {
        int statusCode=500;
        if (exception instanceof ProductException) {
            statusCode = 400;
        }
        if (exception instanceof PaymentException) {
            statusCode = 400;
        }
        OrderResponse orderResponse = OrderResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .paymentStatus(paymentStatus)
                .products(orderRequest.getProducts())
                .paymentMode(orderRequest.getPaymentMode())
                .purchaseAmount(0.0)
                .userName(orderRequest.getUserName())
                .exceptionMessage(exception.getMessage())
                .orderErrorResponse(JsonConverter.getOrderErrorResponse(exception.getMessage()))
                .build();
        throw new OrderServiceException(statusCode,orderResponse);
    }
}