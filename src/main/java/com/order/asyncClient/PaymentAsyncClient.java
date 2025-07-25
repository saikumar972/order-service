package com.order.asyncClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentAsyncClient {
    @Value("${payment.name}")
    String userName;
    @Value("${payment.password}")
    String password;
    @Autowired
    RestTemplate restTemplate;
    private final String url="http://localhost:9100/payment";
    public CompletableFuture<String> getPaymentResponse(String paymentMode,double purchaseAmount){
        String credentials = userName + ":" + password;
        String encodedCredentials= Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpHeaders httpHeaders=new HttpHeaders();
        httpHeaders.add("Authorization","Basic "+encodedCredentials);
        HttpEntity<Void> httpEntity=new HttpEntity<Void>(httpHeaders);
        ResponseEntity<String> payment=restTemplate.exchange(url+"/option/"+paymentMode+"/"+purchaseAmount,
                HttpMethod.PUT,
                httpEntity,
                String.class);
        return CompletableFuture.completedFuture(payment.getBody());
    }
}
