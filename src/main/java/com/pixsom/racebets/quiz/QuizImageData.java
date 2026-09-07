package com.pixsom.racebets.quiz;

import java.util.Base64;

record QuizImageData(String contentType, byte[] bytes) {

    static QuizImageData decode(String dataUrl) {
        return decode(dataUrl, Integer.MAX_VALUE);
    }

    static QuizImageData decode(String dataUrl, int maxBytes) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new IllegalArgumentException("Image absente");
        }

        int separator = dataUrl.indexOf(',');
        if (!dataUrl.startsWith("data:") || separator <= "data:;base64".length()
                || !dataUrl.substring(0, separator).endsWith(";base64")) {
            throw new IllegalArgumentException("Image invalide");
        }

        String contentType = dataUrl.substring("data:".length(), separator - ";base64".length());
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(separator + 1));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Image invalide", exception);
        }

        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException("Chaque image doit peser 5 Mo maximum");
        }
        if (!matchesSignature(contentType, bytes)) {
            throw new IllegalArgumentException("Le contenu de l'image ne correspond pas a son format");
        }
        return new QuizImageData(contentType, bytes);
    }

    private static boolean matchesSignature(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/png" -> bytes.length >= 8
                    && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                    && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
            case "image/jpeg" -> bytes.length >= 3
                    && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }
}
