package io.github.hbmcompat.client;

import java.lang.reflect.Method;

/**
 * Bridges HBM's search boxes to Not Enough Characters' pinyin matching.
 *
 * <p>Reflection, not a direct {@code Match.contains} call: a constant-pool reference to
 * nechar would throw {@link NoClassDefFoundError} on the first search in a client without
 * nechar. Resolving reflectively defers linkage to runtime, where failure just caches
 * {@code null} and we fall back to plain {@code String.contains}.
 */
public final class PinyinBridge {

    private PinyinBridge() {}

    /** nechar's {@code Match.contains(String, CharSequence)}, or null when unavailable. */
    private static Method necharContains;
    private static boolean necharResolved;

    /**
     * Drop-in replacement for {@code String.contains(CharSequence)}. The signature mirrors it
     * so the mixin can swap INVOKEVIRTUAL for INVOKESTATIC without disturbing the stack.
     */
    public static boolean contains(String haystack, CharSequence needle) {
        if (haystack == null) return false;

        if (haystack.contains(needle)) return true;

        Method match = necharContains();
        if (match == null) return false;

        try {
            Object result = match.invoke(null, haystack, needle);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable t) {
            // Signature changed or pinin choked: stop retrying, treat as no match.
            necharContains = null;
            necharResolved = true;
            return false;
        }
    }

    private static Method necharContains() {
        if (necharResolved) return necharContains;
        synchronized (PinyinBridge.class) {
            if (necharResolved) return necharContains;
            try {
                Class<?> match = Class.forName("net.vfyjxf.nechar.utils.Match");
                necharContains = match.getMethod("contains", String.class, CharSequence.class);
            } catch (Throwable t) {
                necharContains = null;
            }
            necharResolved = true;
            return necharContains;
        }
    }

    /** True when nechar is present and usable. */
    public static boolean isActive() {
        return necharContains() != null;
    }
}
