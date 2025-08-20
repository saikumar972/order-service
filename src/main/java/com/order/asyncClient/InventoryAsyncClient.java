package com.order.asyncClient;

import com.order.dto.InventoryResponse;
import com.order.exceptions.inventoryExceptions.ProductException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Service
@Log4j2
@Async
public class InventoryAsyncClient {
    @Value("${inventory.name}")
    String userName;
    @Value("${inventory.password}")
    String password;
    private final RestTemplate restTemplate;
    public InventoryAsyncClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    private final String url="http://localhost:9000/inventory";

    public CompletableFuture<InventoryResponse> checkInventory(String product, double quantity) {
        HttpHeaders headers = getHeaders();
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        return CompletableFuture.supplyAsync(() -> {
            log.debug("checkInventory service runs in {}",Thread.currentThread().getName());
            ResponseEntity<InventoryResponse> response = restTemplate.exchange(
                    url + "/checkAvailability?productName=" + product + "&quantity=" + quantity,
                    HttpMethod.GET,
                    httpEntity,
                    InventoryResponse.class
            );
            return response.getBody();
        }).exceptionally(ex -> {
            log.error("Inventory exception caught for product {}: {}", product, ex.getMessage());
            // Unwrap CompletionException to get the real cause
            Throwable cause = (ex instanceof CompletionException || ex instanceof ExecutionException) ? ex.getCause() : ex;
            log.error("unwrap Inventory exception for product {}: {}", product, ex.getMessage());
            if (cause instanceof HttpClientErrorException httpClientErrorException) {
                throw new ProductException(httpClientErrorException.getResponseBodyAsString());
            } else {
                log.error("RuntimeException caught for the product {}: {}", product, cause.getMessage());
                throw new RuntimeException(cause);
            }
        });
    }

    public CompletableFuture<InventoryResponse> updateInventory(String product,double quantity){
        return CompletableFuture.supplyAsync(() -> {
            HttpHeaders headers = getHeaders();
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
            log.debug("updateInventory service runs in {}",Thread.currentThread().getName());
            ResponseEntity<InventoryResponse> inventory = restTemplate.exchange(
                    url + "/updateInventory?productName=" + product + "&quantity=" + quantity,
                    HttpMethod.PUT,
                    httpEntity,
                    InventoryResponse.class
            );
            return inventory.getBody();
        }).exceptionally(ex -> {
            log.error("Update Inventory exception caught for product {}: {}", product, ex.getMessage());
            // Unwrap CompletionException to get the real cause
            Throwable cause = (ex instanceof CompletionException || ex instanceof ExecutionException) ? ex.getCause() : ex;
            log.error("unwrap Update Inventory exception for product {}: {}", product, ex.getMessage());
            if (cause instanceof HttpClientErrorException httpClientErrorException) {
                throw new ProductException(httpClientErrorException.getResponseBodyAsString());
            } else {
                log.error("Update inventory : RuntimeException caught for the product {}: {}", product, cause.getMessage());
                throw new RuntimeException(cause);
            }
        });
    }

    private HttpHeaders getHeaders() {
        String credentials = userName + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + encodedCredentials);
        return headers;
    }
}
