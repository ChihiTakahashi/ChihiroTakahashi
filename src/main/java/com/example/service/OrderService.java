package com.example.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.constants.TaxType;
import com.example.enums.OrderStatus;
import com.example.enums.PaymentStatus;
import com.example.form.OrderForm;
import com.example.form.OrderShippingList;
import com.example.model.Order;
import com.example.model.OrderDeliveries;
import com.example.model.OrderPayment;
import com.example.model.OrderProduct;
import com.example.repository.OrderDeliveriesRepository;
import com.example.repository.OrderRepository;
import com.example.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OrderDeliveriesRepository orderDeliveriesRepository;

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public OrderService(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Order> findAll() {
		return orderRepository.findAllWithOrderProducts();
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
		// 支払い済み金額を計算(OrderPaymentで支払情報が完了のもののみ対象)
		var paid = order.getOrderPayments().stream()
				.filter(p -> "完了".equals(p.getType()))
				.mapToDouble(p -> p.getPaid()).sum();
		// 合計金額から支払いステータスを判定
		var paymentStatus = paid > order.getGrandTotal() ? PaymentStatus.OVERPAID
				: paid < order.getGrandTotal() ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PAID;

		// 更新
		order.setPaid(paid);
		// 支払のtypeが完了の時のみ受注情報の支払いステータスを更新
		if (entity.getType().equals("完了")) {
			order.setPaymentStatus(paymentStatus);
		}
		// 発送済みかつ入金済みの場合、ordersのstatusをcompletedに更新
		if (order.getStatus().equals("shipping") && paymentStatus == PaymentStatus.PAID) {
			order.setStatus("completed");
		}

		orderRepository.save(order);
	}

	/**
	 * 入金用CSVインポート処理
	 *
	 * @param file
	 * @throws IOException
	 */
	// インポートしたCSVをOrderForm.CreatePayment型に格納してcreatePaymentメソッドを呼び出す
	// エラーがある場合はエラーを返す
	@Transactional
	public List<String> importPaymentCSV(MultipartFile file) throws IOException {
		List<String> validationErrors = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line = br.readLine(); // 1行目はヘッダーなので読み飛ばす
			while ((line = br.readLine()) != null) {
				final String[] split = line.replace("\"", "").split(",");
				// CSVの項目が空の場合にエラーとする
				if (split.length < 5 || split[0].isEmpty() || split[1].isEmpty() ||
						split[2].isEmpty()
						|| split[3].isEmpty() || split[4].isEmpty()) {
					validationErrors.add("項目が足りない行があります。");
					return validationErrors;
				}
				// 該当の受注がない場合エラーとする
				Long orderId = Long.parseLong(split[0]);
				if (!orderRepository.existsById(orderId)) {
					validationErrors.add("該当の受注がありません。");
					return validationErrors;
				}
				try {
					Long.parseLong(split[0]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					validationErrors.add("受注IDが数値ではありません");
					return validationErrors;
				}
				try {
					Long.parseLong(split[2]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					validationErrors.add("金額が数値ではありません");
					return validationErrors;
				}
				// paymentEntityにCSVの情報を格納
				OrderForm.CreatePayment paymentEntity = new OrderForm.CreatePayment();
				paymentEntity.setOrderId(Long.valueOf(split[0]));
				paymentEntity.setType(split[1]);
				paymentEntity.setPaid(Double.valueOf(split[2]));
				paymentEntity.setPaidAt(Timestamp.valueOf(split[3]));
				paymentEntity.setMethod(split[4]);
				this.createPayment(paymentEntity);
			}
			return validationErrors;
		} catch (IOException e) {
			throw new RuntimeException("ファイルが読み込めません", e);
		}
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
	// インポートしたCSVに合致するオーダー情報をList<Order>形式で返す
	@Transactional
	public List<Order> importCSV(MultipartFile file) throws IOException {
		List<Order> orders = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line = br.readLine(); // 1行目はヘッダーなので読み飛ばす

			while ((line = br.readLine()) != null) {
				final String[] split = line.replace("\"", "").split(",");

				// CSV から order_id を取得
				Long orderId = Long.valueOf(split[0]);

				// orders テーブルからデータを取得する
				Order orderData = orderRepository.findById(orderId).orElse(null);

				// 取得したデータを orders リストに追加
				if (orderData != null) {
					orders.add(orderData);
				}
			}
			return orders;
		} catch (IOException e) {
			throw new RuntimeException("ファイルが読み込めません", e);
		}
	}

	// インポートしたcsvをフォームに格納
	@Transactional
	public List<OrderShippingList> importCSVyoForm(MultipartFile file) throws IOException {
		List<OrderShippingList> orderShippingList = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line = br.readLine(); // 1行目はヘッダーなので読み飛ばす

			while ((line = br.readLine()) != null) {
				final String[] split = line.replace("\"", "").split(",");
				// CSVデータをOrderShippingListに変換してリストに追加
				OrderShippingList shippingList = createOrderShippingListFromCSV(split);
				orderShippingList.add(shippingList);
			}
		} catch (IOException e) {
			throw new RuntimeException("ファイルが読み込めません", e);
		}
		return orderShippingList;
	}

	// CSVデータをOrderShippingListに変換するメソッド
	private OrderShippingList createOrderShippingListFromCSV(String[] split) {
		OrderShippingList orderShippingList = new OrderShippingList();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		// CSVのデータをOrderShippingListにセット
		orderShippingList.setChecked(false); // チェックボックスの初期値はfalse（変更可能）
		orderShippingList.setOrderId(Long.valueOf(split[0]));
		orderShippingList.setShippingCode(split[1]);

		try {
			Date shippingDate = dateFormat.parse(split[2]);
			Date deliveryDate = dateFormat.parse(split[3]);
			orderShippingList.setShippingDate(shippingDate);
			orderShippingList.setDeliveryDate(deliveryDate);
		} catch (ParseException e) {
			e.printStackTrace(); // 適切なエラーハンドリングが必要
		}

		orderShippingList.setDeliveryTimezone(split[4]);
		orderShippingList.setUploadStatus(null); // 初期値はnull（変更可能）

		return orderShippingList;
	}

	// インポートしたCSVをOrderDeliveriesのリストに変換して返す
	@Transactional
	public List<OrderDeliveries> importOrderDeliveriesCSV(MultipartFile file) throws IOException {
		List<OrderDeliveries> orderDelivery = new ArrayList<>();
		List<String> validationErrors = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line = br.readLine(); // 1行目はヘッダーなので読み飛ばす
			while ((line = br.readLine()) != null) {
				final String[] split = line.replace("\"", "").split(",");
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				Date date1 = null;
				Date date2 = null;
				OrderDeliveries orderDeliveries = new OrderDeliveries();
				try {
					date1 = dateFormat.parse(split[2]);
					date2 = dateFormat.parse(split[3]);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				orderDeliveries.setOrderId(Long.valueOf(split[0]));
				orderDeliveries.setShippingCode(split[1]);
				orderDeliveries.setShippingDate(date1);
				orderDeliveries.setDeliveryDate(date2);
				orderDeliveries.setDeliveryTimezone(split[4]);
				orderDelivery.add(orderDeliveries);
			}
		} catch (IOException e) {
			throw new RuntimeException("ファイルが読み込めません", e);
		}
		return orderDelivery;
	}

	// バリデーションチェック
	@Transactional
	public List<String> validate(MultipartFile file) throws IOException {
		List<String> validationErrors = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line = br.readLine(); // 1行目はヘッダーなので読み飛ばす
			while ((line = br.readLine()) != null) {
				final String[] split = line.replace("\"", "").split(",");
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				Date date1 = null;
				Date date2 = null;
				// CSVの項目が空の場合にエラーとする
				if (split.length < 5 || split[0].isEmpty() || split[1].isEmpty() ||
						split[2].isEmpty()
						|| split[3].isEmpty() || split[4].isEmpty()) {
					validationErrors.add("項目が足りない行があります。");
					continue;
				}
				try {
					Long.parseLong(split[0]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					validationErrors.add("受注IDが数値ではありません");
				}
				try {
					date1 = dateFormat.parse(split[2]);
					date2 = dateFormat.parse(split[3]);
				} catch (ParseException e) {
					e.printStackTrace();
					validationErrors.add("日付の形式がyyyy-MM-ddではありません");
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("ファイルが読み込めません", e);
		}
		return validationErrors;
	}

	// 渡されたorderDeliveriesをOrderDeliveriesテーブルに保存
	@Transactional
	public void save(OrderDeliveries orderDeliveries) {
		Long orderId = orderDeliveries.getOrderId();
		// 既存のデータがあるか確認
		OrderDeliveries existingOrderDeliveries = orderDeliveriesRepository.findByOrderId(orderId);
		if (existingOrderDeliveries != null) {
			existingOrderDeliveries.setShippingCode(orderDeliveries.getShippingCode());
			existingOrderDeliveries.setShippingDate(orderDeliveries.getShippingDate());
			existingOrderDeliveries.setDeliveryDate(orderDeliveries.getDeliveryDate());
			existingOrderDeliveries.setDeliveryTimezone(orderDeliveries.getDeliveryTimezone());
			existingOrderDeliveries.setUploadStatus(orderDeliveries.getUploadStatus());
			orderDeliveriesRepository.save(existingOrderDeliveries);
		} else {
			// 既存データがない場合はOrderDeliveries をデータベースに新規保存
			orderDeliveriesRepository.save(orderDeliveries);
		}
	}

	// Ordersテーブルのステータスを更新する
	@Transactional
	public void updateOrderStatus(Long orderId) {
		Optional<Order> optionalOrder = orderRepository.findById(orderId);
		if (optionalOrder.isPresent()) {
			Order order = optionalOrder.get();
			// Ordersテーブルのstatusをshippingに変更
			order.setStatus("shipping");
			// Ordersテーブルのpayment_statusがpaidの場合、statusをcompletedに変更
			if ("paid".equals(order.getPaymentStatus())) {
				order.setStatus("completed");
			}
			orderRepository.save(order);
		} else {
			throw new RuntimeException("Order not found with ID: " + orderId);
		}
	}

}
