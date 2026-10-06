package com.riyalo.quotation.service;

import com.riyalo.quotation.entity.CompanyProfile;
import com.riyalo.quotation.repository.CompanyProfileRepository;
import com.riyalo.quotation.service.impl.CompanyProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceImplTest {

    @Mock
    private CompanyProfileRepository companyProfileRepository;

    @TempDir
    Path logoDir;

    private CompanyProfileServiceImpl companyProfileService;

    private CompanyProfile profile;

    @BeforeEach
    void setUp() {
        companyProfileService = new CompanyProfileServiceImpl(companyProfileRepository, logoDir.toString());
        profile = new CompanyProfile();
        profile.setId(1L);
        profile.setCompanyName("Riyalo LLC");
    }

    @Test
    void getProfile_returnsExistingProfile() {
        when(companyProfileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));

        CompanyProfile result = companyProfileService.getProfile();

        assertThat(result.getCompanyName()).isEqualTo("Riyalo LLC");
        verify(companyProfileRepository, never()).save(any(CompanyProfile.class));
    }

    @Test
    void getProfile_createsDefaultProfileWhenMissing() {
        when(companyProfileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(i -> i.getArgument(0));

        CompanyProfile result = companyProfileService.getProfile();

        assertThat(result.getCompanyName()).isNotBlank();
        verify(companyProfileRepository).save(any(CompanyProfile.class));
    }

    @Test
    void updateProfile_savesProvidedDetailsTrimmed() {
        when(companyProfileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(i -> i.getArgument(0));

        CompanyProfile submitted = new CompanyProfile();
        submitted.setCompanyName("  Riyalo LLC  ");
        submitted.setAddress(" Muscat, Oman ");
        submitted.setPhone(" +968 9000 0000 ");
        submitted.setEmail(" info@riyalo.om ");

        CompanyProfile result = companyProfileService.updateProfile(submitted);

        assertThat(result.getCompanyName()).isEqualTo("Riyalo LLC");
        assertThat(result.getAddress()).isEqualTo("Muscat, Oman");
        assertThat(result.getPhone()).isEqualTo("+968 9000 0000");
        assertThat(result.getEmail()).isEqualTo("info@riyalo.om");
        verify(companyProfileRepository).save(profile);
    }

    @Test
    void updateProfile_keepsExistingValuesWhenSubmittedBlank() {
        profile.setAddress("Old Address");
        when(companyProfileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(i -> i.getArgument(0));

        CompanyProfile submitted = new CompanyProfile();
        submitted.setCompanyName("   ");
        submitted.setAddress("   ");

        CompanyProfile result = companyProfileService.updateProfile(submitted);

        assertThat(result.getCompanyName()).isEqualTo("Riyalo LLC");
        assertThat(result.getAddress()).isEqualTo("Old Address");
    }

    @Test
    void updateProfile_nullSubmissionReturnsProfileUnchanged() {
        when(companyProfileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));

        CompanyProfile result = companyProfileService.updateProfile(null);

        assertThat(result).isSameAs(profile);
        verify(companyProfileRepository, never()).save(any(CompanyProfile.class));
    }

    @Test
    void saveLogo_storesFileAndUpdatesProfile() throws Exception {
        when(companyProfileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(i -> i.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("logo", "logo.png", "image/png", "png-bytes".getBytes());

        CompanyProfile result = companyProfileService.saveLogo(file);

        assertThat(result.getLogoFileName()).endsWith(".png").doesNotContain("logo");
        assertThat(logoDir.resolve(result.getLogoFileName())).exists();
        assertThat(Files.readString(logoDir.resolve(result.getLogoFileName()))).isEqualTo("png-bytes");
    }

    @Test
    void saveLogo_replacesPreviousLogoFile() throws Exception {
        Path old = logoDir.resolve("old.png");
        Files.writeString(old, "old-bytes");
        profile.setLogoFileName("old.png");
        when(companyProfileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(i -> i.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("logo", "new.jpg", "image/jpeg", "new-bytes".getBytes());

        CompanyProfile result = companyProfileService.saveLogo(file);

        assertThat(result.getLogoFileName()).endsWith(".jpg");
        assertThat(old).doesNotExist();
        assertThat(logoDir.resolve(result.getLogoFileName())).exists();
    }

    @Test
    void saveLogo_rejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("logo", "evil.exe", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> companyProfileService.saveLogo(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported logo format");
        verify(companyProfileRepository, never()).save(any(CompanyProfile.class));
    }

    @Test
    void saveLogo_rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("logo", "logo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> companyProfileService.saveLogo(empty))
                .isInstanceOf(IllegalArgumentException.class);
        verify(companyProfileRepository, never()).findFirstByOrderByIdAsc();
    }

    @Test
    void deleteLogo_removesFileAndClearsProfile() throws Exception {
        Path file = logoDir.resolve("gone.png");
        Files.writeString(file, "bytes");
        profile.setLogoFileName("gone.png");
        when(companyProfileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(i -> i.getArgument(0));

        CompanyProfile result = companyProfileService.deleteLogo();

        assertThat(result.getLogoFileName()).isNull();
        assertThat(file).doesNotExist();
    }

    @Test
    void deleteLogo_withoutFileStillSucceeds() {
        when(companyProfileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(i -> i.getArgument(0));

        CompanyProfile result = companyProfileService.deleteLogo();

        assertThat(result.getLogoFileName()).isNull();
    }

    @Test
    void readLogo_returnsContentAndContentType() throws Exception {
        Files.writeString(logoDir.resolve("a.png"), "image-bytes");

        CompanyProfileService.LogoFile logo = companyProfileService.readLogo("a.png");

        assertThat(logo).isNotNull();
        assertThat(logo.contentType()).isEqualTo("image/png");
        assertThat(new String(logo.content())).isEqualTo("image-bytes");
    }

    @Test
    void readLogo_returnsNullForMissingFile() {
        assertThat(companyProfileService.readLogo("nope.png")).isNull();
    }

    @Test
    void readLogo_returnsNullForUnsafeOrBlankNames() throws Exception {
        Files.writeString(logoDir.resolve("secret.png"), "secret");

        assertThat(companyProfileService.readLogo(null)).isNull();
        assertThat(companyProfileService.readLogo("  ")).isNull();
        assertThat(companyProfileService.readLogo("../secret.png")).isNull();
        assertThat(companyProfileService.readLogo("..\\secret.png")).isNull();
    }

    @Test
    void readLogo_doesNotReadFilesOutsideLogoDir() throws Exception {
        Path outside = logoDir.getParent().resolve("outside.png");
        Files.writeString(outside, "outside");

        try {
            String relative = ".." + java.io.File.separator + "outside.png";
            assertThat(companyProfileService.readLogo(relative)).isNull();
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void saveLogo_createsDirectoryWhenMissing() throws Exception {
        Path missing = logoDir.resolve("nested/logos");
        companyProfileService = new CompanyProfileServiceImpl(companyProfileRepository, missing.toString());
        when(companyProfileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(i -> i.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("logo", "logo.svg", "image/svg+xml", "<svg/>".getBytes());

        CompanyProfile result = companyProfileService.saveLogo(file);

        assertThat(missing.resolve(result.getLogoFileName())).exists();
    }

    @Test
    void saveLogo_usesUuidNamesForSameOriginalName() throws Exception {
        when(companyProfileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(i -> i.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("logo", "logo.webp", "image/webp", "wb".getBytes());

        String firstName = companyProfileService.saveLogo(file).getLogoFileName();
        profile.setLogoFileName(firstName);
        String secondName = companyProfileService.saveLogo(file).getLogoFileName();

        assertThat(firstName).isNotEqualTo(secondName);
        try (Stream<Path> files = Files.list(logoDir)) {
            assertThat(files.count()).isEqualTo(1); // first file replaced by the second upload
        }
    }
}
