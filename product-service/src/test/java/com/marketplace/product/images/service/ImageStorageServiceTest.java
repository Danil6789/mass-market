package com.marketplace.product.images.service;

import com.marketplace.product.config.StorageProperties;
import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductImage;
import com.marketplace.product.entity.ProductStatus;
import com.marketplace.product.mapper.ProductImageMapper;
import com.marketplace.product.repository.ProductImageRepository;
import com.marketplace.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ImageStorageService}. Storage directory is
 * overridden via JUnit's {@link TempDir}; collaborator interactions are
 * mocked.
 */
@ExtendWith(MockitoExtension.class)
class ImageStorageServiceTest {

    @TempDir Path tempDir;

    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductImageMapper productImageMapper;

    private StorageProperties storageProperties;
    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setBasePath(tempDir.toString());
        imageStorageService = new ImageStorageService(
                storageProperties, productRepository,
                productImageRepository, productImageMapper);
        // Trigger @PostConstruct manually
        try {
            var initMethod = ImageStorageService.class.getDeclaredMethod("initDirectory");
            initMethod.setAccessible(true);
            initMethod.invoke(imageStorageService);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to init storage directory", ex);
        }
    }

    @Test
    void upload_storesFileOnFilesystem() {
        Product product = Product.builder()
                .id(7L)
                .sellerId(42L)
                .categoryId(1L)
                .title("X")
                .price(new BigDecimal("10.00"))
                .status(ProductStatus.ACTIVE)
                .build();

        MultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "fake-png-bytes".getBytes());

        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(productImageRepository.findAllByProductIdOrderByPositionAscIdAsc(7L))
                .thenReturn(List.of());
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage img = inv.getArgument(0);
            img.setId(99L);
            return img;
        });

        ProductImage saved = imageStorageService.upload(7L, file);

        assertThat(saved.getId()).isEqualTo(99L);
        assertThat(saved.getUrl()).startsWith("/uploads/7/");
        assertThat(saved.getUrl()).endsWith(".png");

        Path written = tempDir.resolve("7").resolve(
                saved.getUrl().substring(saved.getUrl().lastIndexOf('/') + 1));
        assertThat(Files.exists(written)).isTrue();
    }

    @Test
    void upload_throwsIllegalArgument_whenContentTypeInvalid() {
        MultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> imageStorageService.upload(1L, file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upload_throwsIllegalArgument_whenFileTooLarge() {
        byte[] huge = new byte[6 * 1024 * 1024]; // 6 MB > 5 MB cap
        MultipartFile file = new MockMultipartFile(
                "file", "huge.jpg", "image/jpeg", huge);

        assertThatThrownBy(() -> imageStorageService.upload(1L, file))
                .isInstanceOf(IllegalArgumentException.class);
    }
}