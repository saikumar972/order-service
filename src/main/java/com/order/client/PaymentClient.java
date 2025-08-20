package com.order.client;

import com.order.exceptions.paymentExceptions.PaymentException;
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
public class PaymentClient {
    @Value("${payment.name}")
    String userName;
    @Value("${payment.password}")
    String password;
    @Autowired
    RestTemplate restTemplate;

    public String getPaymentResponse(String paymentMode,double purchaseAmount) {
        HttpEntity<Void> httpEntity = getVoidHttpEntity();
        try {
            String url = "http://localhost:9100/payment";
            ResponseEntity<String> payment = restTemplate.exchange(url + "/option/" + paymentMode + "/" + purchaseAmount,
                    HttpMethod.PUT,
                    httpEntity,
                    String.class);
            return payment.getBody();
        } catch (HttpClientErrorException e) {
            throw new PaymentException(e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private HttpEntity<Void> getVoidHttpEntity() {
        String credentials = userName + ":" + password;
        String encodedCredentials= Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpHeaders httpHeaders=new HttpHeaders();
        httpHeaders.add("Authorization","Basic "+encodedCredentials);
        return new HttpEntity<>(httpHeaders);
    }
}
