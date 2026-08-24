package com.marketplace.product.images.service;

import com.marketplace.product.config.StorageProperties;
import com.marketplace.product.constant.ExceptionMessages;
import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductImage;
import com.marketplace.product.images.exception.ImageStorageException;
import com.marketplace.product.mapper.ProductImageMapper;
import com.marketplace.product.products.dto.ProductSummary;
import com.marketplace.product.products.exception.ProductNotFoundException;
import com.marketplace.product.repository.ProductImageRepository;
import com.marketplace.product.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import static com.marketplace.product.constant.ExceptionMessages.PRODUCT_NOT_FOUND;

/**
 * Filesystem-backed image storage. Writes uploads under
 * {@code app.storage.base-path}/{productId}/{uuid}.{ext} and exposes the
 * file via {@code /uploads/**} (mapped in {@code WebConfig}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final StorageProperties storageProperties;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductImageMapper productImageMapper;

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png");
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");

    @PostConstruct
    void initDirectory() {
        try {
            Path base = Paths.get(storageProperties.getBasePath());
            Files.createDirectories(base);
            log.info("Image storage base path ready: {}", base.toAbsolutePath());
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Cannot create image storage base path: " + storageProperties.getBasePath(), ex);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductImage> list(Long productId) {
        return productImageRepository.findAllByProductIdOrderByPositionAscIdAsc(productId);
    }

    @Transactional
    public ProductImage upload(Long productId, MultipartFile file) {
        validate(file);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));

        String extension = extractExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + extension;

        Path productDir = Paths.get(storageProperties.getBasePath(), String.valueOf(product.getId()));
        Path target = productDir.resolve(storedName);

        try {
            Files.createDirectories(productDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("Failed to store uploaded image for product {}", productId, ex);
            throw new ImageStorageException(ExceptionMessages.IMAGE_STORAGE_FAILED, ex);
        }

        String url = "/uploads/" + product.getId() + "/" + storedName;
        int nextPosition = productImageRepository
                .findAllByProductIdOrderByPositionAscIdAsc(productId)
                .size();

        ProductImage image = ProductImage.builder()
                .productId(product.getId())
                .url(url)
                .position(nextPosition)
                .build();
        ProductImage saved = productImageRepository.save(image);
        log.info("Image stored for product {}: imageId={}, url={}", productId, saved.getId(), url);
        return saved;
    }

    private static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(ExceptionMessages.IMAGE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(ExceptionMessages.INVALID_IMAGE_FORMAT);
        }
        String original = file.getOriginalFilename();
        String extension = extractExtension(original);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(ExceptionMessages.INVALID_IMAGE_FORMAT);
        }
    }

    private static String extractExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1);
    }
}