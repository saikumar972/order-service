package com.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name="order_table")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String paymentStatus;
    private String transactionId;
    private String paymentMode;
    @ElementCollection
    @CollectionTable(name = "order_products", joinColumns = @JoinColumn(name = "order_id"))
    private List<Product> products;
    private double purchaseAmount;
    private String userName;
    private String exceptionMessage;
    @Transient
    private OrderErrorResponse orderErrorResponse;
}
