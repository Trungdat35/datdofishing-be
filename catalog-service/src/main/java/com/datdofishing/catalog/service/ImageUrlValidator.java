package com.datdofishing.catalog.service;

import com.datdofishing.common.error.BusinessException;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/** Chỉ nhận link ảnh https có đuôi jpg, jpeg, png, webp, gif (bỏ qua phần ?v=... phía sau). */
public final class ImageUrlValidator {

    private static final Set<String> EXT = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private ImageUrlValidator() {
    }

    public static String validate(String url) {
        String trimmed = url == null ? "" : url.trim();
        try {
            URI uri = URI.create(trimmed);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            boolean okExt = EXT.stream().anyMatch(path::endsWith);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || !okExt) {
                throw invalid(trimmed);
            }
            return trimmed;
        } catch (IllegalArgumentException e) {
            throw invalid(trimmed);
        }
    }

    private static BusinessException invalid(String url) {
        return BusinessException.badRequest("INVALID_IMAGE_URL",
                "Link ảnh không hợp lệ (cần https và đuôi .jpg/.png/.webp): " + url);
    }
}
