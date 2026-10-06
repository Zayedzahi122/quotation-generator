package com.riyalo.quotation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Selectable quotation layouts. Each value carries the presentation metadata used by the
 * shared rendering fragment, so the look of a layout is defined in exactly one place.
 */
@Getter
@AllArgsConstructor
public enum LayoutType {

    CLASSIC("banner", "bordered", "#2c3e50", false),
    MODERN("underline", "striped", "#2563eb", false),
    COMPACT("minimal", "bordered", "#475569", false),

    MINIMAL("minimal", "lines", "#111827", false),
    ELEGANT("underline", "lines", "#14532d", false),
    BOLD("banner", "bordered", "#dc2626", false),
    CORPORATE("boxed", "bordered", "#1e3a8a", false),
    CREATIVE("split", "striped", "#c026d3", false),
    MONO("minimal", "minimal", "#374151", false),
    LUXURY("boxed", "minimal", "#b45309", false),
    TECH("split", "striped", "#0891b2", false),
    WARM("underline", "striped", "#ea580c", false),
    SLATE("boxed", "bordered", "#334155", false),
    PAPER("minimal", "lines", "#78350f", false),
    OCEAN("banner", "striped", "#0369a1", false),
    FOREST("underline", "lines", "#166534", false),
    MIDNIGHT("banner", "minimal", "#1d4ed8", true),
    ROYAL("boxed", "bordered", "#6d28d9", false);

    /** How the document header is styled: banner, underline, boxed, split or minimal. */
    private final String headerStyle;

    /** How the items table is styled: bordered, striped, lines or minimal. */
    private final String tableStyle;

    /** Accent colour used for the header, total row and highlights. */
    private final String accent;

    /** True when the layout renders on a dark background. */
    private final boolean dark;
}