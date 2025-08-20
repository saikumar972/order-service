package com.order.exceptions.orderExceptions;

import lombok.Getter;

@Getter
public class OrderServiceException extends RuntimeException {
    private final int statusCode;
    private final Object responseBody;

    public OrderServiceException(int statusCode, Object responseBody) {
        super(responseBody.toString());
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

}
