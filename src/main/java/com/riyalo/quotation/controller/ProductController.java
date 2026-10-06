package com.riyalo.quotation.controller;

import com.riyalo.quotation.entity.Product;
import com.riyalo.quotation.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping("/products")
    public String list(Model model) {
        model.addAttribute("products", productRepository.findAll(Sort.by("name")));
        return "products/list";
    }

    @GetMapping("/products/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Product());
        return "products/form";
    }

    @PostMapping("/products")
    public String create(@ModelAttribute Product product, RedirectAttributes redirectAttributes) {
        productRepository.save(product);
        redirectAttributes.addFlashAttribute("success", "Product created");
        return "redirect:/products";
    }

    @GetMapping("/products/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", productRepository.findById(id).orElseThrow());
        return "products/form";
    }

    @PostMapping("/products/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Product product, RedirectAttributes redirectAttributes) {
        product.setId(id);
        productRepository.save(product);
        redirectAttributes.addFlashAttribute("success", "Product updated");
        return "redirect:/products";
    }

    @PostMapping("/products/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Product deleted");
        return "redirect:/products";
    }
}