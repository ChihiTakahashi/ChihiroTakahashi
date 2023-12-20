package com.example.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "taxes")
public class Tax extends TimeEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tax", nullable = false)
	@NotNull(message = "入力してください")
	@Positive(message = "正の整数を入力してください")
	private Integer tax;

	@Column(name = "taxIncluded", nullable = false)
	private boolean taxIncluded;

	@Column(name = "rouonding", nullable = false)
	private String rounding;

	public Integer rate;

	public void setTax(Integer tax) {
		this.tax = tax;
	}

	public Integer getTax() {
		return tax;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setTaxIncluded(boolean taxIncluded) {
		this.taxIncluded = taxIncluded;
	}

	public boolean getTaxIncluded() {
		return taxIncluded;
	}

	public void setRounding(String rounding) {
		this.rounding = rounding;
	}

	public String getRounding() {
		return rounding;
	}

}
