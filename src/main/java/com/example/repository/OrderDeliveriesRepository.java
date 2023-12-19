package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.model.OrderDeliveries;

public interface OrderDeliveriesRepository extends JpaRepository<OrderDeliveries, Long> {
	OrderDeliveries findByOrderId(Long orderId);
}
