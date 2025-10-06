package com.careup.branch.common.util;

public final class PhoneUtils {
    private PhoneUtils() {}

    public static String normalize(String input) {
        if (input == null) return null;
        String digits = input.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return digits.substring(0,3) + "-" + digits.substring(3,7) + "-" + digits.substring(7);
        } else if (digits.length() == 10) {
            return digits.substring(0,3) + "-" + digits.substring(3,6) + "-" + digits.substring(6);
        }
        return input;
    }
}
