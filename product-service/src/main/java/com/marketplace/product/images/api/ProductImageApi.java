package com.marketplace.product.images.api;

import com.marketplace.product.images.dto.ImageUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.marketplace.product.constant.ApiPath.PRODUCTS_BASE;

/**
 * Image upload / list API contract.
 */
@Tag(name = "Product Images", description = "Загрузка и список изображений товара")
@RequestMapping(PRODUCTS_BASE + "/{id}/images")
public interface ProductImageApi {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Загрузить изображение",
            description = "Принимает JPEG/PNG до 5 МБ. Доступно владельцу или администратору."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Изображение загружено"),
            @ApiResponse(responseCode = "400", description = "Неверный формат или размер файла"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Нет прав на загрузку"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    ResponseEntity<ImageUploadResponse> upload(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Parameter(description = "ID товара") @PathVariable("id") Long productId,
            @Parameter(description = "Файл изображения (JPEG/PNG, до 5 МБ)")
                @RequestPart("file") MultipartFile file);

    @GetMapping
    @Operation(
            summary = "Список изображений товара",
            description = "Возвращает все изображения, отсортированные по позиции"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список получен"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    ResponseEntity<List<ImageUploadResponse>> list(
            @Parameter(description = "ID товара") @PathVariable("id") Long productId);
}