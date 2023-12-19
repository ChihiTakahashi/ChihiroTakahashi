package com.example.form;

import java.util.Date;
import java.util.List;

import com.example.model.OrderDeliveries;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderShippingList {

	private List<OrderShippingList> orderShippingList;

	private List<Boolean> checkedList;
	private boolean checked;
	private Long orderId;
	private String shippingCode;
	private Date shippingDate;
	private Date deliveryDate;
	private String deliveryTimezone;
	private String uploadStatus;

	public OrderShippingList(OrderDeliveries orderDeliveries) {
		this.setOrderId(orderDeliveries.getOrderId());
		this.setShippingCode(orderDeliveries.getShippingCode());
		this.setShippingDate(orderDeliveries.getShippingDate());
		this.setDeliveryDate(orderDeliveries.getDeliveryDate());
		this.setDeliveryTimezone(orderDeliveries.getDeliveryTimezone());
	}

	// // チェックボックスの状態を取得するためのgetter
	// public boolean isChecked() {
	// return checked;
	// }

	// // チェックボックスの状態を設定するためのsetter
	// public void setOrderId(boolean checked) {
	// this.checked = checked;
	// }

	// public Long getOrderId() {
	// return orderId;
	// }

	// public void setOrderId(Long orderId) {
	// this.orderId = orderId;
	// }

	// public String getShippingCode() {
	// return shippingCode;
	// }

	// public void setShippingCode(String shippingCode) {
	// this.shippingCode = shippingCode;
	// }

	// public String getShippingDate() {
	// return shippingDate;
	// }

	// public void setShippingDate(String shippingDate) {
	// this.shippingDate = shippingDate;
	// }

	// public String getDeliveryDate() {
	// return deliveryDate;
	// }

	// public void setDeliveryDate(String deliveryDate) {
	// this.deliveryDate = deliveryDate;
	// }

	// public String getDeliveryTimezone() {
	// return deliveryTimezone;
	// }

	// public void setDeliveryTimezone(String deliveryTimezone) {
	// this.deliveryTimezone = deliveryTimezone;
	// }

	// public String getUploadStatus() {
	// return uploadStatus;
	// }

	// public void setUploadStatus(String uploadStatus) {
	// this.uploadStatus = uploadStatus;
	// }

	// public ProductForm(Product product) {
	// this.setId(product.getId());
	// this.setShopId(product.getShopId());
	// this.setName(product.getName());
	// this.setCode(product.getCode());
	// // 紐づくカテゴリIDのリストを作成
	// List<CategoryProduct> categoryProducts = product.getCategoryProducts();
	// if (categoryProducts != null) {
	// List<Long> categoryIds = categoryProducts.stream().map(categoryProduct ->
	// categoryProduct.getCategoryId())
	// .collect(Collectors.toList());
	// this.setCategoryIds(categoryIds);
	// }
	// this.setWeight(product.getWeight());
	// this.setHeight(product.getHeight());
	// this.setPrice(product.getPrice().doubleValue());
	// var tax = TaxType.get(product.getTaxType());
	// this.setRate(tax.rate);
	// this.setTaxIncluded(tax.taxIncluded);
	// this.setRounding(tax.rounding);
	// }

}
