package com.stufy.fragmc.redempt.utils;

import java.util.Random;

public class PromoCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    public static String generateCode() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                code.append("-");
            }

            for (int j = 0; j < 4; j++) {
                int index = RANDOM.nextInt(CHARACTERS.length());
                code.append(CHARACTERS.charAt(index));
            }
        }

        return code.toString();
    }

    public static boolean isValidFormat(String code) {
        return code.matches("^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$");
    }
}