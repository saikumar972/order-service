package com.order.exceptions.inventoryExceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProductErrorResponse {
   private HttpStatus httpStatus;
   private String message;
   private int statusCode;
}
