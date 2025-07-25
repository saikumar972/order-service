package com.order.repo;

import com.order.dto.OrderResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepo extends JpaRepository<OrderResponse,Long> {
}
