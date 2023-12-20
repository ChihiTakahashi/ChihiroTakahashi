package com.example.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.constants.Message;
import com.example.model.Tax;
import com.example.service.TaxService;

@Controller
@RequestMapping("/taxes")
public class TaxController {

	public static final String FLOOR = "floor";
	public static final String ROUND = "round";
	public static final String CEIL = "ceil";

	@Autowired
	private TaxService taxService;

	@GetMapping
	public String index(Model model) {
		List<Tax> all = taxService.findAll();
		Set<Integer> uniqueTaxValues = new HashSet<>();
		List<Tax> uniqueTaxList = new ArrayList<>();
		for (Tax tax : all) {
			// すでに存在する税率でない場合に追加
			if (uniqueTaxValues.add(tax.getTax())) {
				uniqueTaxList.add(tax);
			}
		}
		model.addAttribute("listTax", uniqueTaxList);
		return "tax/index";
	}

	@GetMapping("/{id}")
	public String show(Model model, @PathVariable("id") Long id) {
		if (id != null) {
			Optional<Tax> tax = taxService.findOne(id);
			model.addAttribute("tax", tax.get());
		}
		return "tax/show";
	}

	@GetMapping(value = "/new")
	public String create(Model model, @ModelAttribute Tax entity) {
		model.addAttribute("tax", entity);
		return "tax/form";
	}

	@PostMapping
	public String create(@Validated @ModelAttribute Tax entity, BindingResult result,
			RedirectAttributes redirectAttributes) {
		// 同じ税率がある場合
		if (taxService.existsByTax(entity.getTax())) {
			result.rejectValue("tax", "error.tax", "すでに同じ税率が存在します");
			return "tax/form";
		}
		try {
			List<Tax> taxList = createMultipleTaxEntities(entity);
			taxService.saveAll(taxList);
			redirectAttributes.addFlashAttribute("success", Message.MSG_SUCESS_INSERT);
			return "redirect:/taxes";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", Message.MSG_ERROR);
			e.printStackTrace();
			return "redirect:/taxes";
		}
	}

	private List<Tax> createMultipleTaxEntities(Tax entity) {
		List<Tax> taxList = new ArrayList<>();
		boolean[] taxIncludeds = { false, true };
		String[] roundings = { FLOOR, ROUND, CEIL };
		for (int i = 0; i < taxIncludeds.length; i++) {
			for (int j = 0; j < roundings.length; j++) {
				Tax tax = new Tax();
				tax.setTax(entity.getTax());
				tax.setTaxIncluded(taxIncludeds[i]);
				tax.setRounding(roundings[j]);
				taxList.add(tax);
			}
		}
		return taxList;
	}

	@DeleteMapping("/{id}")
	public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		try {
			if (id != null) {
				Optional<Tax> entity = taxService.findOne(id);
				if (entity.isPresent()) {
					taxService.delete(id);
					taxService.deleteByTax(entity.get().getTax());
					redirectAttributes.addFlashAttribute("success", Message.MSG_SUCESS_DELETE);
				}

			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", Message.MSG_ERROR);
			throw new ServiceException(e.getMessage());
		}
		return "redirect:/taxes";
	}
}
