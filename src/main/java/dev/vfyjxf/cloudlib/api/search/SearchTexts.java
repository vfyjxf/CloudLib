package dev.vfyjxf.cloudlib.api.search;

import dev.vfyjxf.cloudlib.util.SearchReference;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The single text pipeline every shared-search implementation must route
 * through: normalization (lowercase + optional {@link Transformer}), matching
 * (optional {@link TextMatcher}, otherwise the {@link SearchReference} seam
 * that Just Enough Characters bytecode-replaces) and word tokenization.
 *
 * <p>Replacement points, in order of preference for CJK/pinyin integration:
 * <ol>
 *   <li>{@link #setTransformer} — a pure {@code String -> String} mapping
 *       applied to both document terms and query values during normalization
 *       (safe with every accelerated index path);</li>
 *   <li>{@link #setMatcher} — a custom containment predicate; accelerations
 *       that assume substring semantics are disabled while one is installed
 *       (see {@link #accelerated()});</li>
 *   <li>no registration at all — plain {@code String.contains} dispatched via
 *       {@link SearchReference}, whose call sites Just Enough Characters
 *       rewrites directly.</li>
 * </ol>
 */
public final class SearchTexts {
    private static volatile @Nullable Transformer transformer;
    private static volatile @Nullable TextMatcher matcher;
    private static volatile SearchTextTokenizer tokenizer = SearchTextTokenizer.words();

    private SearchTexts() {
    }

    /** Pure string-to-string transform applied after lowercasing, on both index and query side. */
    @FunctionalInterface
    public interface Transformer {
        String apply(String value);
    }

    /** Replacement containment predicate; disables substring-assuming fast paths while installed. */
    @FunctionalInterface
    public interface TextMatcher {
        boolean contains(String haystack, String needle);
    }

    public static void setTransformer(@Nullable Transformer value) {
        transformer = value;
    }

    public static @Nullable Transformer transformer() {
        return transformer;
    }

    public static void setMatcher(@Nullable TextMatcher value) {
        matcher = value;
    }

    public static @Nullable TextMatcher matcher() {
        return matcher;
    }

    public static void setTokenizer(SearchTextTokenizer value) {
        tokenizer = value == null ? SearchTextTokenizer.words() : value;
    }

    public static SearchTextTokenizer tokenizer() {
        return tokenizer;
    }

    /**
     * Whether accelerated index paths (dictionary-exact hits, token postings)
     * may be used: true only when no custom matcher is installed, because
     * those paths assume substring semantics.
     */
    public static boolean accelerated() {
        return matcher == null;
    }

    /**
     * Lowercases (returning the original instance when already lowercase) and applies the {@link Transformer}.
     * Mixed-case results are interned in a bounded cache: display names repeat
     * across hundreds of thousands of indexed values.
     */
    public static String normalize(String value) {
        Transformer transform = transformer;
        if (transform != null) {
            // the transformer must see every input: CJK and already-lowercase
            // text never allocates in lowercase(), so a fast path taken first
            // would silently skip it exactly where it matters most
            return transform.apply(lowercase(value));
        }
        String lowered = lowercase(value);
        return lowered == value ? value : intern(lowered);
    }

    private static final int internLimit = 1 << 16;
    private static final java.util.concurrent.ConcurrentHashMap<String, String> internCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static String intern(String lowered) {
        String cached = internCache.get(lowered);
        if (cached != null) {
            return cached;
        }
        if (internCache.size() >= internLimit) {
            internCache.clear();
        }
        internCache.putIfAbsent(lowered, lowered);
        return lowered;
    }

    /** Case-fold only, without the transformer; for callers that pre-transform. */
    public static String lowercase(String value) {
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (c >= 'A' && c <= 'Z'
                || c >= 0xD800 && c <= 0xDFFF // surrogate pairs: code-point case mappings escape char checks
                || c >= 0x80 && Character.toLowerCase(c) != c) {
                return value.toLowerCase(Locale.ROOT);
            }
        }
        return value;
    }

    /** Containment through the replaceable seam: custom matcher if installed, else {@link SearchReference}. */
    public static boolean contains(String haystack, String needle) {
        TextMatcher custom = matcher;
        if (custom != null) {
            return custom.contains(haystack, needle);
        }
        return SearchReference.contains(haystack, needle);
    }

    public static List<String> tokenize(String text) {
        return tokenizer.tokenize(text);
    }
}
