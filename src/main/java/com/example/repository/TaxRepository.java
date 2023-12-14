package com.example.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.model.Tax;

import jakarta.transaction.Transactional;

public interface TaxRepository extends JpaRepository<Tax, Long> {
	boolean existsByTax(Integer tax);

	@Transactional
	void deleteByTax(Integer tax);

	Optional<Tax> findByTaxAndTaxIncludedAndRounding(Integer tax, boolean taxIncluded, String rounding);

	List<Tax> findAll();
}
