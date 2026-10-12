package com.datdofishing.catalog.service;

import java.text.Normalizer;
import java.util.Locale;

/** Tạo slug không dấu từ tiếng Việt: "Cần câu đài" -> "can-cau-dai". */
public final class Slugs {

    private Slugs() {
    }

    public static String of(String text) {
        String s = text.replace('đ', 'd').replace('Đ', 'D');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return s.isEmpty() ? "item" : s;
    }
}
