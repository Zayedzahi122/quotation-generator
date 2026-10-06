package com.riyalo.quotation.service.impl;

import com.riyalo.quotation.entity.CompanyProfile;
import com.riyalo.quotation.repository.CompanyProfileRepository;
import com.riyalo.quotation.service.CompanyProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CompanyProfileServiceImpl implements CompanyProfileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "svg");

    private final CompanyProfileRepository companyProfileRepository;
    private final String logoDir;

    public CompanyProfileServiceImpl(CompanyProfileRepository companyProfileRepository,
                                     @Value("${quotation.storage.logo-dir:./uploads/logos}") String logoDir) {
        this.companyProfileRepository = companyProfileRepository;
        this.logoDir = logoDir;
    }

    @Transactional
    @Override
    public CompanyProfile getProfile() {
        return companyProfileRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    CompanyProfile profile = new CompanyProfile();
                    profile.setCompanyName("Your Company Name");
                    return companyProfileRepository.save(profile);
                });
    }

    @Transactional
    @Override
    public CompanyProfile updateProfile(CompanyProfile submitted) {
        CompanyProfile profile = getProfile();
        if (submitted == null) {
            return profile;
        }
        if (isPresent(submitted.getCompanyName())) {
            profile.setCompanyName(submitted.getCompanyName().trim());
        }
        if (isPresent(submitted.getAddress())) {
            profile.setAddress(submitted.getAddress().trim());
        }
        if (isPresent(submitted.getPhone())) {
            profile.setPhone(submitted.getPhone().trim());
        }
        if (isPresent(submitted.getEmail())) {
            profile.setEmail(submitted.getEmail().trim());
        }
        return companyProfileRepository.save(profile);
    }

    @Transactional
    @Override
    public CompanyProfile saveLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No logo file supplied");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported logo format. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        CompanyProfile profile = getProfile();
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(resolveLogoDir());
            Files.write(resolveLogoDir().resolve(fileName), file.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Could not store the logo file", e);
        }

        deleteLogoFile(profile.getLogoFileName());
        profile.setLogoFileName(fileName);
        return companyProfileRepository.save(profile);
    }

    @Transactional
    @Override
    public CompanyProfile deleteLogo() {
        CompanyProfile profile = getProfile();
        deleteLogoFile(profile.getLogoFileName());
        profile.setLogoFileName(null);
        return companyProfileRepository.save(profile);
    }

    @Override
    public LogoFile readLogo(String fileName) {
        if (fileName == null || fileName.isBlank() || !isSafeFileName(fileName)) {
            return null;
        }
        Path path = resolveLogoDir().resolve(fileName);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            return new LogoFile(Files.readAllBytes(path), contentTypeOf(fileName));
        } catch (IOException e) {
            return null;
        }
    }

    private Path resolveLogoDir() {
        return Paths.get(logoDir).toAbsolutePath().normalize();
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private void deleteLogoFile(String fileName) {
        if (fileName == null || !isSafeFileName(fileName)) {
            return;
        }
        try {
            Files.deleteIfExists(resolveLogoDir().resolve(fileName));
        } catch (IOException ignored) {
            // Best effort: a leftover file must not block the update.
        }
    }

    private boolean isSafeFileName(String fileName) {
        Path resolved = resolveLogoDir().resolve(fileName).normalize();
        return resolved.startsWith(resolveLogoDir());
    }

    private String extensionOf(String originalName) {
        if (originalName == null) {
            return null;
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return null;
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String contentTypeOf(String fileName) {
        return switch (extensionOf(fileName)) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }
}
