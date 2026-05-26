package com.auction.util;

import java.util.Locale;
import java.util.Scanner;

public class FormatUtils {
    // Hàm phụ trợ bóc tách số double an toàn, ép sử dụng dấu chấm thập phân
    public static double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            try (Scanner scanner = new Scanner(value)) {
                scanner.useLocale(Locale.US);
                if (scanner.hasNextDouble()) {
                    return scanner.nextDouble();
                }
            }
            return Double.parseDouble(value.replace(",", "."));
        } catch (Exception e) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                return 0.0; // Trả về 0 nếu hoàn toàn không phải là số
            }
        }
    }
}