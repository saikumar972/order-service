package com.order.asyncClient;

import com.order.dto.InventoryResponse;
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
public class InventoryAsyncClient {
    @Value("${inventory.name}")
    String userName;
    @Value("${inventory.password}")
    String password;
    @Autowired
    RestTemplate restTemplate;
    private final String url="http://localhost:9000/inventory";
    public CompletableFuture<InventoryResponse> getInventoryResponse(String product,double quantity){
        String credentials = userName + ":" + password;
        String encodedCredentials= Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpHeaders httpHeaders=new HttpHeaders();
        httpHeaders.add("Authorization","Basic "+encodedCredentials);
        HttpEntity<Void> httpEntity=new HttpEntity<Void>(httpHeaders);
        ResponseEntity<InventoryResponse> inventory=restTemplate.exchange(url + "/updateInventory?productName=" + product + "&quantity=" + quantity,
                HttpMethod.PUT,
                httpEntity,
                InventoryResponse.class);
        return CompletableFuture.completedFuture(inventory.getBody());
    }
}
