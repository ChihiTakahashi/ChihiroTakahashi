package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
	List<Order> findByStatus(String status);

	List<Order> findByIdIn(List<Long> orderIds);

	@Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderProducts")
	List<Order> findAllWithOrderProducts();

}
