package com.riyalo.quotation.controller;

import com.riyalo.quotation.entity.Customer;
import com.riyalo.quotation.entity.Product;
import com.riyalo.quotation.entity.Quotation;
import com.riyalo.quotation.enums.LayoutType;
import com.riyalo.quotation.enums.QuotationStatus;
import com.riyalo.quotation.repository.CustomerRepository;
import com.riyalo.quotation.repository.ProductRepository;
import com.riyalo.quotation.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @GetMapping("/quotations")
    public String list(Model model) {
        List<Quotation> quotations = quotationService.findAll();
        model.addAttribute("quotations", quotations);
        return "quotation/list";
    }

    @GetMapping("/quotations/new")
    public String createForm(Model model) {
        Quotation quotation = new Quotation();
        quotation.setDate(LocalDate.now());
        quotation.setStatus(QuotationStatus.DRAFT);
        quotation.setLayoutType(LayoutType.CLASSIC);
        quotation.setSubtotal(BigDecimal.ZERO);
        quotation.setTaxAmount(BigDecimal.ZERO);
        quotation.setTotal(BigDecimal.ZERO);
        addFormAttributes(model, quotation);
        return "quotation/form";
    }

    @PostMapping("/quotations")
    public String create(@ModelAttribute Quotation quotation, RedirectAttributes redirectAttributes) {
        Quotation saved = quotationService.createFromForm(quotation);
        redirectAttributes.addFlashAttribute("success", "Quotation created: " + saved.getNumber());
        return "redirect:/quotations/" + saved.getId();
    }

    @GetMapping("/quotations/{id}")
    public String view(@PathVariable Long id, Model model) {
        Quotation quotation = quotationService.findById(id);
        model.addAttribute("quotation", quotation);
        model.addAttribute("layoutTypes", LayoutType.values());
        return "quotation/" + quotation.getLayoutType().name().toLowerCase() + "/view";
    }

    @GetMapping("/quotations/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Quotation quotation = quotationService.findById(id);
        addFormAttributes(model, quotation);
        return "quotation/form";
    }

    @PostMapping("/quotations/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Quotation quotation, RedirectAttributes redirectAttributes) {
        quotation.setId(id);
        Quotation saved = quotationService.updateFromForm(quotation);
        redirectAttributes.addFlashAttribute("success", "Quotation updated: " + saved.getNumber());
        return "redirect:/quotations/" + saved.getId();
    }

    @PostMapping("/quotations/{id}/layout")
    public String updateLayout(@PathVariable Long id, @RequestParam LayoutType layoutType, RedirectAttributes redirectAttributes) {
        Quotation updated = quotationService.updateLayout(id, layoutType);
        redirectAttributes.addFlashAttribute("success", "Layout changed to " + updated.getLayoutType());
        return "redirect:/quotations/" + id;
    }

    @PostMapping("/quotations/{id}/vat")
    public String applyVat(@PathVariable Long id,
                           @RequestParam boolean applyVat,
                           @RequestParam(required = false) BigDecimal vatRate,
                           RedirectAttributes redirectAttributes) {
        BigDecimal rate = vatRate != null ? vatRate : quotationService.getDefaultVatRate();
        quotationService.applyVat(id, applyVat, rate);
        redirectAttributes.addFlashAttribute("success", applyVat
                ? "VAT applied to all items (" + rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%)"
                : "VAT removed from all items");
        return "redirect:/quotations/" + id;
    }

    @PostMapping("/quotations/{id}/recalculate")
    public String recalculate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        quotationService.recalculateTotals(id);
        redirectAttributes.addFlashAttribute("success", "Totals recalculated");
        return "redirect:/quotations/" + id;
    }

    @PostMapping("/quotations/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        quotationService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Quotation deleted");
        return "redirect:/quotations";
    }

    private void addFormAttributes(Model model, Quotation quotation) {
        model.addAttribute("quotation", quotation);
        model.addAttribute("customerNames", customerRepository.findAll(Sort.by("name")).stream()
                .map(Customer::getName).toList());
        model.addAttribute("productNames", productRepository.findAll(Sort.by("name")).stream()
                .map(Product::getName).toList());
        model.addAttribute("layoutTypes", LayoutType.values());
        model.addAttribute("statuses", QuotationStatus.values());
        model.addAttribute("defaultVatPercent", quotationService.getDefaultVatRate()
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
    }
}