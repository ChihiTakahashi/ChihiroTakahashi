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

}
