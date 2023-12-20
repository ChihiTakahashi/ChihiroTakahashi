package com.example.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.constants.Message;
import com.example.enums.OrderStatus;
import com.example.enums.PaymentMethod;
import com.example.enums.PaymentStatus;
import com.example.form.OrderForm;
import com.example.form.OrderShippingList;
import com.example.model.Order;
import com.example.model.OrderDeliveries;
import com.example.service.OrderService;
import com.example.service.ProductService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/orders")
public class OrderController {

	@Autowired
	private OrderService orderService;

	@Autowired
	private ProductService productService;

	List<Order> orderShippingData;
	List<OrderDeliveries> orderDeliveries;

	@GetMapping
	public String index(Model model) {
		List<Order> all = orderService.findAll();
		model.addAttribute("listOrder", all);
		return "order/index";
	}

	// 入金ページ表示
	@GetMapping("/payments")
	public String indexPayments(Model model, HttpSession session) {
		List<Order> all = orderService.findAll();
		model.addAttribute("listOrder", all);
		// セッションからエラーメッセージを取得してモデルに追加
		List<String> validationErrors = (List<String>)session.getAttribute("validationErrors");
		if (validationErrors != null && !validationErrors.isEmpty()) {
			model.addAttribute("validationErrors", validationErrors);
			session.removeAttribute("validationErrors"); // 不要になったエラーメッセージはセッションから削除
		}
		return "order/payments";
	}

	@GetMapping("/{id}")
	public String show(Model model, @PathVariable("id") Long id) {
		if (id != null) {
			Optional<Order> order = orderService.findOne(id);
			model.addAttribute("order", order.get());
		}
		return "order/show";
	}

	@GetMapping(value = "/new")
	public String create(Model model, @ModelAttribute OrderForm.Create entity) {
		model.addAttribute("order", entity);
		model.addAttribute("products", productService.findAll());
		model.addAttribute("paymentMethods", PaymentMethod.values());
		return "order/create";
	}

	@PostMapping
	public String create(@Validated @ModelAttribute OrderForm.Create entity, BindingResult result,
			RedirectAttributes redirectAttributes) {
		Order order = null;
		try {
			order = orderService.create(entity);
			redirectAttributes.addFlashAttribute("success", Message.MSG_SUCESS_INSERT);
			return "redirect:/orders/" + order.getId();
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", Message.MSG_ERROR);
			e.printStackTrace();
			return "redirect:/orders";
		}
	}

	@GetMapping("/{id}/edit")
	public String update(Model model, @PathVariable("id") Long id) {
		try {
			if (id != null) {
				Optional<Order> entity = orderService.findOne(id);
				model.addAttribute("order", entity.get());
				model.addAttribute("paymentMethods", PaymentMethod.values());
				model.addAttribute("paymentStatus", PaymentStatus.values());
				model.addAttribute("orderStatus", OrderStatus.values());
			}
		} catch (Exception e) {
			throw new ServiceException(e.getMessage());
		}
		return "order/form";
	}

	@PutMapping
	public String update(@Validated @ModelAttribute Order entity, BindingResult result,
			RedirectAttributes redirectAttributes) {
		Order order = null;
		try {
			order = orderService.save(entity);
			redirectAttributes.addFlashAttribute("success", Message.MSG_SUCESS_UPDATE);
			return "redirect:/orders/" + order.getId();
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", Message.MSG_ERROR);
			e.printStackTrace();
			return "redirect:/orders";
		}
	}

	@DeleteMapping("/{id}")
	public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		try {
			if (id != null) {
				Optional<Order> entity = orderService.findOne(id);
				orderService.delete(entity.get());
				redirectAttributes.addFlashAttribute("success", Message.MSG_SUCESS_DELETE);
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", Message.MSG_ERROR);
			throw new ServiceException(e.getMessage());
		}
		return "redirect:/orders";
	}

	@PostMapping("/{id}/payments")
	public String createPayment(@Validated @ModelAttribute OrderForm.CreatePayment entity, BindingResult result,
			RedirectAttributes redirectAttributes) {
		try {
			orderService.createPayment(entity);
			redirectAttributes.addFlashAttribute("success", Message.MSG_SUCESS_PAYMENT_INSERT);
			return "redirect:/orders/" + entity.getOrderId();
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", Message.MSG_ERROR);
			e.printStackTrace();
			return "redirect:/orders";
		}
	}

	/**
	 * 入金用CSVインポート処理
	 *
	 * @param uploadFile
	 * @param redirectAttributes
	 * @return
	 */
	@PostMapping("/payments/upload_file")
	public String uploadFile(@RequestParam("file") MultipartFile uploadFile, RedirectAttributes redirectAttributes,
			HttpSession session)
			throws IOException {

		if (uploadFile.isEmpty()) {
			// ファイルが存在しない場合
			redirectAttributes.addFlashAttribute("error", "ファイルを選択してください。");
			return "redirect:/orders/payments";
		}
		if (!"text/csv".equals(uploadFile.getContentType())) {
			// CSVファイル以外の場合
			redirectAttributes.addFlashAttribute("error", "CSVファイルを選択してください。");
			return "redirect:/orders/payments";
		}
		List<String> validationErrors = orderService.importPaymentCSV(uploadFile);

		if (!validationErrors.isEmpty()) {
			// バリデーションエラーがあればエラーメッセージをモデルに追加
			session.setAttribute("validationErrors", validationErrors);
			return "redirect:/orders/payments";
		}

		return "redirect:/orders/payments";
	}

	@GetMapping("/shipping")
	public String indexOrders(Model model, RedirectAttributes redirectAttributes,
			@ModelAttribute OrderShippingList orderShippingList, HttpSession session) {
		List<Order> all = orderService.findAll();
		model.addAttribute("listOrder", all);
		model.addAttribute("orderShippingData", orderShippingData);
		model.addAttribute("orderDeliveries", orderDeliveries);
		model.addAttribute("orderShippingList", orderShippingList);
		// セッションからエラーメッセージを取得してモデルに追加
		List<String> validationErrors = (List<String>)session.getAttribute("validationErrors");
		if (validationErrors != null && !validationErrors.isEmpty()) {
			model.addAttribute("validationErrors", validationErrors);
			session.removeAttribute("validationErrors"); // 不要になったエラーメッセージはセッションから削除
		}
		return "order/shipping";
	}

	/**
	 * CSVインポート処理
	 *
	 * @param uploadFile
	 * @param redirectAttributes
	 * @return
	 */
	@PostMapping("/shipping/upload_file")
	public String uploadFile(@RequestParam("file") MultipartFile uploadFile,
			@ModelAttribute OrderShippingList orderShippingList, BindingResult result,
			RedirectAttributes redirectAttributes, Model model, HttpSession session) {

		if (uploadFile.isEmpty()) {
			// ファイルが存在しない場合
			redirectAttributes.addFlashAttribute("error", "ファイルを選択してください。");
			return "redirect:/orders/shipping";
		}
		if (!"text/csv".equals(uploadFile.getContentType())) {
			// CSVファイル以外の場合
			redirectAttributes.addFlashAttribute("error", "CSVファイルを選択してください。");
			return "redirect:/orders/shipping";
		}
		try {
			List<String> validationErrors = orderService.validate(uploadFile);

			if (!validationErrors.isEmpty()) {
				// バリデーションエラーがあればエラーメッセージをモデルに追加
				session.setAttribute("validationErrors", validationErrors);
				return "redirect:/orders/shipping";
			}
			// インポートしたCSVの情報をorderDeliveriesへ格納(modelでの追加はshowメソッドにて実装)
			this.orderDeliveries = orderService.importOrderDeliveriesCSV(uploadFile);
			return "redirect:/orders/shipping";
		} catch (Throwable e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			e.printStackTrace();
			return "redirect:/orders/shipping";
		}
	}

	/**
	 * CSVテンプレートダウンロード処理
	 *
	 * @param response
	 * @param redirectAttributes
	 * @return
	 */
	@PostMapping("/shipping/template_download")
	public String downloadTemplate(HttpServletResponse response, RedirectAttributes redirectAttributes) {
		try (OutputStream os = response.getOutputStream();) {
			Path filePath = new ClassPathResource("static/templates/order.csv").getFile().toPath();
			byte[] fb1 = Files.readAllBytes(filePath);
			String attachment = "attachment; filename=orderTemplate_" + new Date().getTime() + ".csv";

			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", attachment);
			response.setContentLength(fb1.length);
			os.write(fb1);
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * CSV未発送受注ダウンロード処理
	 *
	 * @param response
	 * @param redirectAttributes
	 * @return
	 */
	@PostMapping("/shipping/download")
	public void downloadOrdersCsv(HttpServletResponse response) {
		try {
			List<Order> orders = orderService.findByStatus("ordered");
			String csvData = this.convertToCSV(orders);
			response.setContentType("text/csv");
			response.setHeader("Content-Disposition", "attachment; filename=no_shipping_orders.csv");

			try (OutputStream outputStream = response.getOutputStream()) {
				outputStream.write(csvData.getBytes(StandardCharsets.UTF_8));
				outputStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// データベースをCSVに変換
	public String convertToCSV(List<Order> orders) {
		String header = "id,customer_id,status,total,tax,discount,shipping,grand_total,paid,payment_method,payment_status,note";

		String data = orders.stream()
				.map(order -> String.join(",",
						String.valueOf(order.getId()),
						String.valueOf(order.getCustomerId()),
						String.valueOf(order.getStatus()),
						String.valueOf(order.getTotal()),
						String.valueOf(order.getTax()),
						String.valueOf(order.getDiscount()),
						String.valueOf(order.getShipping()),
						String.valueOf(order.getGrandTotal()),
						String.valueOf(order.getPaid()),
						String.valueOf(order.getPaymentMethod()),
						String.valueOf(order.getPaymentStatus()),
						String.valueOf(order.getNote())))
				.collect(Collectors.joining("\n"));
		return header + "\n" + data;
	}

	// 出荷情報更新ボタン押下時の処理
	@PutMapping("/shipping")
	public String updateShipping(@ModelAttribute OrderShippingList orderShippingList, BindingResult result,
			RedirectAttributes redirectAttributes) {
		try {
			boolean isError = false;
			// チェックボックスがチェックされているか確認
			for (int i = 0; i < orderShippingList.getOrderShippingList().size(); i++) {
				OrderShippingList shippingList = orderShippingList.getOrderShippingList().get(i);
				// チェックされている場合の処理
				if (shippingList.isChecked()) {
					// orders テーブルから id に合致するデータを取得
					Optional<Order> optionalOrder = orderService
							.findOne(Long.valueOf(orderDeliveries.get(i).getOrderId()));
					if (optionalOrder.isPresent()) {
						Order order = optionalOrder.get();
						if (order.getStatus().equals("completed")) {
							// ステータスがcompletedの場合
							orderDeliveries.get(i).setUploadStatus("error");
							isError = true;
						} else {
							// orderDeliveriesのentityをsaveメソッドに渡す(iはチェックされたデータの行数)
							orderService.save(orderDeliveries.get(i));
							orderDeliveries.get(i).setUploadStatus("success");
							// チェックされた受注のorderIdをupdateOrderStatusメソッドに渡す
							orderService.updateOrderStatus(orderDeliveries.get(i).getOrderId());
						}
					} else {
						// 合致する受注IDがない場合
						orderDeliveries.get(i).setUploadStatus("error");
						isError = true;
					}
				}
			}
			if (isError) {
				throw new Exception("エラー行があります");
			}
			redirectAttributes.addFlashAttribute("success", "出荷情報を一括更新しました。");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "出荷情報の一括更新中にエラーが発生しました。");
			e.printStackTrace();
		}
		return "redirect:/orders/shipping";
	}
}
