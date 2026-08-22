package dev.vfyjxf.cloudlib.api.search;

public final class SearchParseException extends IllegalArgumentException {
    private final int position;

    public SearchParseException(String message, int position) {
        super(message + " at " + position);
        this.position = position;
    }

    public int position() {
        return position;
    }
}
