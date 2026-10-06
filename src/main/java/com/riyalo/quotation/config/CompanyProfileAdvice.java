package com.riyalo.quotation.config;

import com.riyalo.quotation.entity.CompanyProfile;
import com.riyalo.quotation.service.CompanyProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the company profile (name, logo, contact details) available to every view
 * without each controller having to load it.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class CompanyProfileAdvice {

    private final CompanyProfileService companyProfileService;

    @ModelAttribute("profile")
    public CompanyProfile profile() {
        return companyProfileService.getProfile();
    }
}
