package com.example.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.model.Tax;
import com.example.repository.TaxRepository;

@Service
@Transactional(readOnly = true)
public class TaxService {

	@Autowired
	private TaxRepository taxRepository;

	public List<Tax> findAll() {
		return taxRepository.findAll();
	}

	public Optional<Tax> findOne(Long id) {
		return taxRepository.findById(id);
	}

	@Transactional(readOnly = false)
	public Tax save(Tax entity) {
		return taxRepository.save(entity);
	}

	@Transactional(readOnly = false)
	public List<Tax> saveAll(List<Tax> entities) {
		return taxRepository.saveAll(entities);
	}

	@Transactional(readOnly = false)
	public void delete(Tax entity) {
		taxRepository.delete(entity);
	}

	// 同じ税率が存在しているか確認
	public boolean existsByTax(Integer tax) {
		return taxRepository.existsByTax(tax);
	}

	@Transactional(readOnly = false)
	public void delete(Long id) {
		taxRepository.deleteById(id);
	}

	@Transactional(readOnly = false)
	public void deleteByTax(Integer tax) {
		taxRepository.deleteByTax(tax);
	}

	public Optional<Tax> findTaxByRateAndTaxIncludedAndRounding(Integer tax, boolean taxIncluded, String rounding) {
		return taxRepository.findByTaxAndTaxIncludedAndRounding(tax, taxIncluded, rounding);
	}
}
