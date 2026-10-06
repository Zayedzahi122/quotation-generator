package com.riyalo.quotation.controller;

import com.riyalo.quotation.entity.Customer;
import com.riyalo.quotation.repository.CustomerRepository;
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
public class CustomerController {

    private final CustomerRepository customerRepository;

    @GetMapping("/customers")
    public String list(Model model) {
        model.addAttribute("customers", customerRepository.findAll(Sort.by("name")));
        return "customers/list";
    }

    @GetMapping("/customers/new")
    public String createForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customers/form";
    }

    @PostMapping("/customers")
    public String create(@ModelAttribute Customer customer, RedirectAttributes redirectAttributes) {
        customerRepository.save(customer);
        redirectAttributes.addFlashAttribute("success", "Customer created");
        return "redirect:/customers";
    }

    @GetMapping("/customers/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerRepository.findById(id).orElseThrow());
        return "customers/form";
    }

    @PostMapping("/customers/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Customer customer, RedirectAttributes redirectAttributes) {
        customer.setId(id);
        customerRepository.save(customer);
        redirectAttributes.addFlashAttribute("success", "Customer updated");
        return "redirect:/customers";
    }

    @PostMapping("/customers/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        customerRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Customer deleted");
        return "redirect:/customers";
    }
}