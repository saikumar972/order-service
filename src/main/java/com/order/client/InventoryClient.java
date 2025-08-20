package com.order.client;

import com.order.dto.InventoryResponse;
import com.order.exceptions.inventoryExceptions.ProductException;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Log4j2
public class InventoryClient {
    @Value("${inventory.name}")
    String userName;
    @Value("${inventory.password}")
    String password;
    @Autowired
    RestTemplate restTemplate;
    private final static String url="http://localhost:9000/inventory";
    @SneakyThrows
    public InventoryResponse checkInventory(String product, double quantity){
        HttpEntity<Void> httpEntity = getHttpEntity();
        try{
            ResponseEntity<InventoryResponse> inventory=restTemplate.exchange(url + "/checkAvailability?productName=" + product + "&quantity=" + quantity,
                    HttpMethod.GET,
                    httpEntity,
                    InventoryResponse.class);
            return inventory.getBody();
        }catch (HttpClientErrorException exception){
            // Propagate exception
            throw new ProductException(exception.getResponseBodyAsString());
        }catch (Exception e){
            throw new ProductException(e.getMessage());
        }
    }

    public InventoryResponse updateInventory(String product, double quantity){
        HttpEntity<Void> httpEntity = getHttpEntity();
        try{
            ResponseEntity<InventoryResponse> inventory=restTemplate.exchange(url + "/updateInventory?productName=" + product + "&quantity=" + quantity,
                    HttpMethod.PUT,
                    httpEntity,
                    InventoryResponse.class);
            return inventory.getBody();
        }catch (HttpClientErrorException exception){
            // Propagate exception
            throw new ProductException(exception.getResponseBodyAsString());
        }catch (Exception e){
            throw new ProductException(e.getMessage());
        }
    }

    private HttpEntity<Void> getHttpEntity() {
        HttpHeaders httpHeaders=new HttpHeaders();
        String credentials = userName + ":" + password;
        String encodedCredentials= Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        httpHeaders.add("Authorization","Basic "+encodedCredentials);
        return new HttpEntity<>(httpHeaders);
    }
}
