package com.example.controller.api;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.model.Category;
import com.example.model.CategoryProduct;
import com.example.service.CategoryProductService;
import com.example.service.CategoryService;

@RestController
@RequestMapping("/api/categories")
public class ApiCategoryController {

	@Autowired
	private CategoryService categoryService;
	@Autowired
	private CategoryProductService categoryProductService;

	@GetMapping("/all")
	List<Category> findAll() {
		return categoryService.findAll();
	}

	@GetMapping("/{id}")
	List<CategoryProduct> findCategoryProducts(@PathVariable("id") Long id) {
		// Optional<Category> category = categoryService.findOne(id);
		List<CategoryProduct> categoryProducts = categoryProductService.findByCategoryId(id);
		return categoryProducts;
	}

	@PostMapping("/{id}/updateCategoryProduct")
	public ResponseEntity<String> deleteInsertCategoryProduct(@PathVariable("id") Long categoryId,
			@RequestBody Map<String, List<Long>> request) {
		System.out.println("呼び出し");
		List<Long> productIds = request.get("productIds");
		boolean result = categoryService.deleteInsertCategoryProduct(categoryId, productIds);
		if (result) {
			return ResponseEntity.ok("カテゴリーと商品の紐付設定更新を完了しました。");
		} else {
			return ResponseEntity.badRequest().body("カテゴリーと商品の紐付設定更新に失敗しました。");
		}
	}

}
