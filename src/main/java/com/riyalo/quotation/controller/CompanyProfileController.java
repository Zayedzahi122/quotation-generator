package com.riyalo.quotation.controller;

import com.riyalo.quotation.entity.CompanyProfile;
import com.riyalo.quotation.service.CompanyProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    @GetMapping("/profile")
    public String edit(Model model) {
        model.addAttribute("profileForm", companyProfileService.getProfile());
        return "profile";
    }

    @PostMapping("/profile")
    public String update(@ModelAttribute("profileForm") CompanyProfile profileForm,
                         RedirectAttributes redirectAttributes) {
        companyProfileService.updateProfile(profileForm);
        redirectAttributes.addFlashAttribute("message", "Company details saved");
        return "redirect:/profile";
    }

    @PostMapping("/profile/logo")
    public String uploadLogo(@RequestParam("logo") MultipartFile logo,
                             RedirectAttributes redirectAttributes) {
        try {
            companyProfileService.saveLogo(logo);
            redirectAttributes.addFlashAttribute("message", "Logo uploaded");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/logo/delete")
    public String deleteLogo(RedirectAttributes redirectAttributes) {
        companyProfileService.deleteLogo();
        redirectAttributes.addFlashAttribute("message", "Logo removed");
        return "redirect:/profile";
    }

    @GetMapping("/logos/{fileName}")
    @ResponseBody
    public ResponseEntity<byte[]> serveLogo(@PathVariable String fileName) {
        CompanyProfileService.LogoFile logo = companyProfileService.readLogo(fileName);
        if (logo == null) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(logo.contentType()));
        headers.setCacheControl("max-age=86400");
        return ResponseEntity.ok().headers(headers).body(logo.content());
    }
}
