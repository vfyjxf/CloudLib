package dev.vfyjxf.cloudlib.api.search;

public record SearchMatch(
    boolean matched,
    int score
) {
    public static final SearchMatch NO = new SearchMatch(false, 0);
    public static final SearchMatch YES = new SearchMatch(true, 0);

    public SearchMatch {
        if (!matched) {
            score = 0;
        }
    }
}
