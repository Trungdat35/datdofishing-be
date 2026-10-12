package com.datdofishing.common.error;

import java.time.Instant;
import java.util.Map;

/** Định dạng lỗi chuẩn trả về cho mọi API. */
public record ApiError(Instant timestamp, int status, String code, String message, String path,
                       Map<String, String> fieldErrors) {
}
