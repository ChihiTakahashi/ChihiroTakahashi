package com.example.model;

import java.io.Serializable;
import java.util.List;

import com.example.enums.OrderStatus;
import com.example.enums.PaymentMethod;
import com.example.enums.PaymentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "orders", indexes = @Index(name = "idx_customer_id", columnList = "customer_id"))
public class Order extends TimeEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "customer_id", nullable = false, columnDefinition = "INT(11) UNSIGNED")
	private Integer customerId;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "total", nullable = false)
	private Double total;

	@Column(name = "tax", nullable = false)
	private Double tax;

	@Column(name = "discount", nullable = false)
	private Double discount;

	@Column(name = "shipping", nullable = false)
	private Double shipping;

	@Column(name = "grand_total", nullable = false)
	private Double grandTotal;

	@Column(name = "paid", nullable = false)
	private Double paid;

	@Column(name = "payment_method", nullable = false)
	private String paymentMethod;

	@Column(name = "payment_status", nullable = false)
	private String paymentStatus;

	@Column(name = "note", nullable = false, columnDefinition = "TEXT")
	private String note;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private List<OrderProduct> orderProducts;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private List<OrderPayment> orderPayments;

	public Order() {}

	/**
	 * 支払い方法名を取得する
	 */
	public String getPaymentMethodName() {
		var value = PaymentMethod.getPaymentMethod(this.paymentMethod);
		if (value != null) {
			return value.getName();
		}
		return "";
	}

	/**
	 * 支払いステータス名を取得する
	 */
	public String getPaymentStatusName() {
		var value = PaymentStatus.getPaymentStatus(this.paymentStatus);
		if (value != null) {
			return value.getName();
		}
		return "";
	}

	/**
	 * ステータスを取得する
	 */
	public String getStatusName() {
		var value = OrderStatus.getOrderStatus(this.status);
		if (value != null) {
			return value.getName();
		}
		return "";
	}

	public Order(Long id, Integer customerId, Double discount, Double shipping, Double tax, Double total,
			Double grandTotal,
			String status, String paymentMethod, String paymentStatus, Double paid, String note) {
		this.id = id;
		this.customerId = customerId;
		this.discount = discount;
		this.shipping = shipping;
		this.tax = tax;
		this.total = total;
		this.grandTotal = grandTotal;
		this.status = status;
		this.paymentMethod = paymentMethod;
		this.paymentStatus = paymentStatus;
		this.paid = paid;
		this.note = note;

	}

}
