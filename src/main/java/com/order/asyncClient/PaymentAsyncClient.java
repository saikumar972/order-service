package com.order.asyncClient;

import com.order.exceptions.paymentExceptions.PaymentException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
@Log4j2
@Async
public class PaymentAsyncClient {
    @Value("${payment.name}")
    String userName;
    @Value("${payment.password}")
    String password;
    @Autowired
    RestTemplate restTemplate;
    private final String url="http://localhost:9100/payment";

    public CompletableFuture<String> getPaymentResponse(String paymentMode, double purchaseAmount) {
        HttpHeaders headers = getHeaders();
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        return CompletableFuture.supplyAsync(() -> {
            log.debug("payment service runs in {}",Thread.currentThread().getName());
            ResponseEntity<String> response = restTemplate.exchange(
                    url + "/option/" + paymentMode + "/" + purchaseAmount,
                    HttpMethod.PUT,
                    httpEntity,
                    String.class
            );
            return response.getBody();
        }).exceptionally(ex -> {
            log.error("payment failed : {}", ex.getMessage());
            if(ex instanceof HttpClientErrorException exception){
                throw new PaymentException(exception.getResponseBodyAsString());
            }else{
                throw new RuntimeException(ex.getMessage());
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
