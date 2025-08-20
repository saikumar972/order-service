package com.order.exceptions.paymentExceptions;

public class PaymentException extends RuntimeException{
    public PaymentException(String message){
        super(message);
    }
}
