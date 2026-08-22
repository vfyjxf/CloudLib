package dev.vfyjxf.cloudlib.api.search;

public record SearchSyntaxSpan(
    int start,
    int end,
    SearchSyntaxKind kind
) {
    public SearchSyntaxSpan {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid search syntax span: " + start + ".." + end);
        }
        if (kind == null) {
            throw new NullPointerException("kind");
        }
    }
}
