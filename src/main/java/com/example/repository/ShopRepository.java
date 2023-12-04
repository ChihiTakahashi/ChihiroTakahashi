package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.model.Shop;

public interface ShopRepository extends JpaRepository<Shop, Long> {
	List<Shop> findByNameContaining(String name);
}
