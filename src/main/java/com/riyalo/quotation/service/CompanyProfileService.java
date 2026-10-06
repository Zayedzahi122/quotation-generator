package com.riyalo.quotation.service;

import com.riyalo.quotation.entity.CompanyProfile;
import org.springframework.web.multipart.MultipartFile;

public interface CompanyProfileService {

    /** Returns the singleton profile, creating it with defaults when missing. */
    CompanyProfile getProfile();

    /** Updates our own company details (name, address, phone, email) on the singleton profile. */
    CompanyProfile updateProfile(CompanyProfile submitted);

    /** Stores the uploaded logo on disk and links it to the profile. Returns the saved profile. */
    CompanyProfile saveLogo(MultipartFile file);

    /** Removes the logo file from disk and from the profile. */
    CompanyProfile deleteLogo();

    /** Reads the logo bytes for serving over HTTP, or null when no logo exists. */
    LogoFile readLogo(String fileName);

    /** Logo bytes plus their content type. */
    record LogoFile(byte[] content, String contentType) {
    }
}
