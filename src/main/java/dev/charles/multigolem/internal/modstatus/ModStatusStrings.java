package dev.charles.multigolem.internal.modstatus;

final class ModStatusStrings {
    private ModStatusStrings() {
    }

    static String requireText(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    static String requireIdentifierPart(String value, String name) {
        String trimmed = requireText(value, name);
        if (trimmed.contains(":")) {
            throw new IllegalArgumentException(name + " must not contain ':'");
        }
        return trimmed;
    }

    static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
