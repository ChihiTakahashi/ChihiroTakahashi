package com.example.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.constants.TaxType;
import com.example.enums.OrderStatus;
import com.example.enums.PaymentStatus;
import com.example.form.OrderForm;
import com.example.model.Order;
import com.example.model.OrderPayment;
import com.example.model.OrderProduct;
import com.example.repository.OrderRepository;
import com.example.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ProductRepository productRepository;

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public OrderService(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Order> findAll() {
		return orderRepository.findAll();
	}

	public Optional<Order> findOne(Long id) {
		return orderRepository.findById(id);
	}

	@Transactional(readOnly = false)
	public Order save(Order entity) {
		return orderRepository.save(entity);
	}

	@Transactional(readOnly = false)
	public Order create(OrderForm.Create entity) {
		Order order = new Order();
		order.setCustomerId(entity.getCustomerId());
		order.setShipping(entity.getShipping());
		order.setNote(entity.getNote());
		order.setPaymentMethod(entity.getPaymentMethod());
		order.setStatus(OrderStatus.ORDERED);
		order.setPaymentStatus(PaymentStatus.UNPAID);
		order.setPaid(0.0);

		var orderProducts = new ArrayList<OrderProduct>();
		entity.getOrderProducts().forEach(p -> {
			var product = productRepository.findById(p.getProductId()).get();
			var orderProduct = new OrderProduct();
			orderProduct.setProductId(product.getId());
			orderProduct.setCode(product.getCode());
			orderProduct.setName(product.getName());
			orderProduct.setQuantity(p.getQuantity());
			orderProduct.setPrice((double)product.getPrice());
			orderProduct.setDiscount(p.getDiscount());
			orderProduct.setTaxType(TaxType.get(product.getTaxType()));
			orderProducts.add(orderProduct);
		});

		// 計算
		var total = 0.0;
		var totalTax = 0.0;
		var totalDiscount = 0.0;
		for (var orderProduct : orderProducts) {
			var price = orderProduct.getPrice();
			var quantity = orderProduct.getQuantity();
			var discount = orderProduct.getDiscount();
			var tax = 0.0;
			/**
			 * 税額を計算する
			 */
			if (orderProduct.getTaxIncluded()) {
				// 税込みの場合
				tax = price * quantity * orderProduct.getTaxRate() / (100 + orderProduct.getTaxRate());
			} else {
				// 税抜きの場合
				tax = price * quantity * orderProduct.getTaxRate() / 100;
			}
			// 端数処理
			tax = switch (orderProduct.getTaxRounding()) {
			case TaxType.ROUND -> Math.round(tax);
			case TaxType.CEIL -> Math.ceil(tax);
			case TaxType.FLOOR -> Math.floor(tax);
			default -> tax;
			};
			var subTotal = price * quantity + tax - discount;
			total += subTotal;
			totalTax += tax;
			totalDiscount += discount;
		}
		order.setTotal(total);
		order.setTax(totalTax);
		order.setDiscount(totalDiscount);
		order.setGrandTotal(total + order.getShipping());
		order.setOrderProducts(orderProducts);

		orderRepository.save(order);

		return order;

	}

	@Transactional()
	public void delete(Order entity) {
		orderRepository.delete(entity);
	}

	@Transactional(readOnly = false)
	public void createPayment(OrderForm.CreatePayment entity) {
		var order = orderRepository.findById(entity.getOrderId()).get();
		/**
		 * 新しい支払い情報を登録する
		 */
		var payment = new OrderPayment();
		payment.setType(entity.getType());
		payment.setPaid(entity.getPaid());
		payment.setMethod(entity.getMethod());
		payment.setPaidAt(entity.getPaidAt());

		/**
		 * 支払い情報を更新する
		 */
		// orderのorderPaymentsに追加
		order.getOrderPayments().add(payment);
		// 支払い済み金額を計算
		var paid = order.getOrderPayments().stream().mapToDouble(p -> p.getPaid()).sum();
		// 合計金額から支払いステータスを判定
		var paymentStatus = paid > order.getGrandTotal() ? PaymentStatus.OVERPAID
				: paid < order.getGrandTotal() ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PAID;

		// 更新
		order.setPaid(paid);
		order.setPaymentStatus(paymentStatus);
		orderRepository.save(order);
	}

	public List<Order> findByStatus(String status) {
		return orderRepository.findByStatus(status);
	}

	/**
	 * CSVインポート処理
	 *
	 * @param file
	 * @throws IOException
	 */
	@Transactional
	public List<Order> importCSV(MultipartFile file) throws IOException {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line = br.readLine(); // 1行目はヘッダーなので読み飛ばす
			List<Order> orders = new ArrayList<>();
			while ((line = br.readLine()) != null) {
				final String[] split = line.replace("\"", "").split(",");
				Order order;
				Optional<Order> existingOrder = orderRepository.findById(Long.valueOf(split[0]));
				// order = existingOrder.get();
				if (existingOrder.isPresent()) {
					// すでにデータが存在する場合は更新
					order = existingOrder.get();
					updateOrderFromCSV(order, split);
					orders.add(order);
				} else {
					// データが存在しない場合は新規作成
					order = createOrderFromCSV(split);
					orders.add(order);
				}
				// Integer customerId = existingOrder.map(Order::getCustomerId).orElse(null);
				// if ((split[6]).equals("paid")) {
				// order.setId(Long.valueOf(split[0]));
				// order.setShippingCode(split[1]);
				// order.setShippingDate(LocalDate.parse(split[2]));
				// order.setDeliveryDate(LocalDate.parse(split[3]));
				// order.setDeliveryTimezone(split[4]);
				// order.setStatus("completed");
				// order.setPaymentStatus(split[6]);
				// order.setCustomerId(customerId);
				// orders.add(order);
				// } else {
				// order.setId(Long.valueOf(split[0]));
				// order.setShippingCode(split[1]);
				// order.setShippingDate(LocalDate.parse(split[2]));
				// order.setDeliveryDate(LocalDate.parse(split[3]));
				// order.setDeliveryTimezone(split[4]);
				// order.setStatus("shipping");
				// order.setPaymentStatus(split[6]);
				// order.setCustomerId(customerId);
				// orders.add(order);
				// }
			}
			batchInsert(orders);
			return orders;
		} catch (IOException e) {
			throw new RuntimeException("ファイルが読み込めません", e);
		}
	}

	private void updateOrderFromCSV(Order order, String[] split) {
		// 変更がある場合だけデータを更新
		if ((split[6]).equals("paid")) {
			order.setId(Long.valueOf(split[0]));
			order.setShippingCode(split[1]);
			order.setShippingDate(LocalDate.parse(split[2]));
			order.setDeliveryDate(LocalDate.parse(split[3]));
			order.setDeliveryTimezone(split[4]);
			order.setStatus("completed");
			order.setPaymentStatus(split[6]);
		} else {
			order.setId(Long.valueOf(split[0]));
			order.setShippingCode(split[1]);
			order.setShippingDate(LocalDate.parse(split[2]));
			order.setDeliveryDate(LocalDate.parse(split[3]));
			order.setDeliveryTimezone(split[4]);
			order.setStatus("shipping");
			order.setPaymentStatus(split[6]);
		}
	}

	private Order createOrderFromCSV(String[] split) {
		Order order = new Order();
		order.setId(Long.valueOf(split[0]));
		order.setShippingCode(split[1]);
		order.setShippingDate(LocalDate.parse(split[2]));
		order.setDeliveryDate(LocalDate.parse(split[3]));
		order.setDeliveryTimezone(split[4]);
		if ((split[6]).equals("paid")) {
			order.setStatus("completed");
		} else {
			order.setStatus("shipping");
		}
		order.setPaymentStatus(split[6]);

		return order;
	}

	/**
	 * 一括更新処理実行
	 *
	 * @param orders
	 */
	@SuppressWarnings("unused")
	private int[] batchInsert(List<Order> orders) {
		String sql = "INSERT INTO orders (id, shipping_code, shipping_date, delivery_date, delivery_timezone, status, payment_status, create_at, update_at) "
				+
				"VALUES (:id, :shipping_code, :shipping_date, :delivery_date, :delivery_timezone, :status, :payment_status, :create_at, :update_at)";

		List<MapSqlParameterSource> batchParams = new ArrayList<>();

		for (Order order : orders) {
			MapSqlParameterSource params = new MapSqlParameterSource()
					.addValue("id", order.getId())
					.addValue("shipping_code", order.getShippingCode())
					.addValue("shipping_date", order.getShippingDate())
					.addValue("delivery_date", order.getDeliveryDate())
					.addValue("delivery_timezone", order.getDeliveryTimezone())
					.addValue("status", order.getStatus())
					.addValue("payment_status", order.getPaymentStatus())
					.addValue("create_at", new Date())
					.addValue("update_at", new Date());
			batchParams.add(params);
		}
		return jdbcTemplate.batchUpdate(sql, batchParams.toArray(new MapSqlParameterSource[0]));
	}

}
