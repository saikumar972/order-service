package com.order.exceptions.orderExceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.exceptions.inventoryExceptions.ProductErrorResponse;
import com.order.exceptions.inventoryExceptions.ProductException;
import com.order.exceptions.paymentExceptions.PaymentException;
import com.order.exceptions.paymentExceptions.PaymentErrorResponse;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderExceptionalHandler {
    ObjectMapper objectMapper=new ObjectMapper();
    @ExceptionHandler(ProductException.class)
    @SneakyThrows
    public ResponseEntity<ProductErrorResponse> productException(ProductException exception){
        ProductErrorResponse productErrorResponse= objectMapper.readValue(exception.getMessage(), ProductErrorResponse.class);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(productErrorResponse);
    }

    @SneakyThrows
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<PaymentErrorResponse> paymentException(PaymentException exception){
        PaymentErrorResponse paymentErrorResponse= objectMapper.readValue(exception.getMessage(), PaymentErrorResponse.class);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentErrorResponse);
    }

    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<Object> handleOrderServiceException(OrderServiceException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBody());
    }
}
