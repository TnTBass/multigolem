package dev.charles.multigolem.customizations;

import dev.charles.multigolem.GolemVariant;

import java.util.List;
import java.util.Objects;

public record VariantCustomizationSummary(GolemVariant variant, List<String> lines) {
    public VariantCustomizationSummary {
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
    }
}
