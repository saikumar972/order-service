package com.order.exceptions.orderExceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.exceptions.inventoryExceptions.ProductErrorResponse;
import com.order.exceptions.inventoryExceptions.ProductException;
import com.order.exceptions.paymentExceptions.PaymentException;
import com.order.exceptions.paymentExceptions.PaymentErrorResponse;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@RestControllerAdvice
@Log4j2
public class OrderExceptionalHandler {
    ObjectMapper objectMapper=new ObjectMapper();
    @ExceptionHandler(ProductException.class)
    @SneakyThrows
    public ResponseEntity<Object> productException(ProductException exception){
        ProductErrorResponse productErrorResponse= objectMapper.readValue(exception.getMessage(), ProductErrorResponse.class);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(productErrorResponse);
    }

    @SneakyThrows
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Object> paymentException(PaymentException exception){
        PaymentErrorResponse paymentErrorResponse= objectMapper.readValue(exception.getMessage(), PaymentErrorResponse.class);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentErrorResponse);
    }

    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<Object> handleOrderServiceException(OrderServiceException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBody());
    }

    @ExceptionHandler({ CompletionException.class, ExecutionException.class })
    public ResponseEntity<Object> handleFutureExceptions(Exception ex) {
        Throwable cause = ex.getCause();
        log.error("CompletableFuture Exception handled in controller advice {}",cause.getMessage());
        if (cause instanceof ProductException)
            return productException((ProductException) cause);
        if (cause instanceof PaymentException)
            return paymentException((PaymentException) cause);
        // other mappings
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unknown error", "message", cause.getMessage()));
    }




}
