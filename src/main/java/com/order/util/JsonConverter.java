package com.order.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.dto.OrderErrorResponse;
import lombok.SneakyThrows;

public class JsonConverter {
    @SneakyThrows
    public static OrderErrorResponse getOrderErrorResponse(String message){
        ObjectMapper objectMapper=new ObjectMapper();
        return objectMapper.readValue(message, OrderErrorResponse.class);
    }
}
