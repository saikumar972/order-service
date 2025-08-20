package com.order.exceptions.paymentExceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PaymentErrorResponse {
    private String errorMessage;
    private HttpStatus httpStatus;
    private int statusCode;
}
