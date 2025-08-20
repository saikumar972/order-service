package com.order.service;

import com.order.client.InventoryClient;
import com.order.client.PaymentClient;
import com.order.dto.InventoryResponse;
import com.order.dto.OrderRequest;
import com.order.dto.OrderResponse;
import com.order.exceptions.inventoryExceptions.ProductException;
import com.order.exceptions.orderExceptions.OrderServiceException;
import com.order.exceptions.paymentExceptions.PaymentException;
import com.order.repo.OrderRepo;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Log4j2
public class OrderServiceV1 {
    public OrderRepo orderRepo;
    public InventoryClient inventoryClient;
    public PaymentClient paymentClient;

    public OrderResponse orderResponseV1(OrderRequest orderRequest) {
        List<InventoryResponse> inventoryAvailability = checkAllProductInventory(orderRequest);
        double totalPurchaseCost = calculateTotalAmount(inventoryAvailability);
        String paymentStatus = processPayment(orderRequest, totalPurchaseCost);
        updateInventoryAfterPayment(orderRequest);
        return buildAndSaveOrderResponse(orderRequest, paymentStatus, totalPurchaseCost);
    }

    // 1. Check inventory for all products with proper exception propagation
    private List<InventoryResponse> checkAllProductInventory(OrderRequest orderRequest) {
        try {
            return orderRequest.getProducts()
                    .stream()
                    .map(product -> inventoryClient.checkInventory(product.getProductName(), product.getQuantity()))
                    .toList();
        } catch (Exception e) {
            throw buildOrderServiceException(e, orderRequest, "payment failed due to inventory check failed");
        }
    }

    // 2. Calculate the total amount to be paid
    private double calculateTotalAmount(List<InventoryResponse> inventoryAvailability) {
        double total = inventoryAvailability.stream()
                .mapToDouble(InventoryResponse::getAmountPurchased)
                .sum();
        log.info("Total purchase cost: {}", total);
        return total;
    }

    // 3. Handle the payment call and exceptions
    private String processPayment(OrderRequest orderRequest, double totalPurchaseCost) {
        try {
            return paymentClient.getPaymentResponse(orderRequest.getPaymentMode(), totalPurchaseCost);
        } catch (Exception e) {
            throw buildOrderServiceException(e, orderRequest, "payment failed due to payment service failed");
        }
    }

    // 4. Update inventory after successful payment
    private void updateInventoryAfterPayment(OrderRequest orderRequest) {
        orderRequest.getProducts().forEach(product ->
                inventoryClient.updateInventory(product.getProductName(), product.getQuantity()));
    }

    // 5. Build, save, and return order response
    private OrderResponse buildAndSaveOrderResponse(OrderRequest orderRequest, String paymentStatus, double totalPurchaseCost) {
        OrderResponse response = OrderResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .paymentStatus(paymentStatus)
                .products(orderRequest.getProducts())
                .paymentMode(orderRequest.getPaymentMode())
                .purchaseAmount(totalPurchaseCost)
                .userName(orderRequest.getUserName())
                .build();
        orderRepo.save(response);
        return response;
    }

    // Utility: Build an OrderServiceException with code and response
    private OrderServiceException buildOrderServiceException(Exception e, OrderRequest orderRequest, String paymentStatus) {
        int statusCode = 500;
        if (e instanceof ProductException) {
            statusCode = 400;
        }
        if (e instanceof PaymentException) {
            statusCode = 400;
        }
        log.error("Exception occurred: {}", e.getMessage());
        OrderResponse failedResponse = OrderResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .paymentStatus(paymentStatus)
                .products(orderRequest.getProducts())
                .paymentMode(orderRequest.getPaymentMode())
                .purchaseAmount(0.0)
                .userName(orderRequest.getUserName())
                .exceptionMessage(e.getMessage())
                .build();
        return new OrderServiceException(statusCode, failedResponse);
    }

}
