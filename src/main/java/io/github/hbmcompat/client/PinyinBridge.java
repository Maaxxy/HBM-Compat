package io.github.hbmcompat.client;

import java.lang.reflect.Method;

/**
 * Bridges HBM's search boxes to Not Enough Characters' pinyin matching, without ever
 * hard-linking to nechar.
 *
 * <p>Why reflection instead of a plain {@code Match.contains} call: HBM Compat must keep
 * working when nechar is not installed. A direct call would put a
 * {@code net.vfyjxf.nechar.utils.Match} reference in this class's constant pool, and the
 * JVM would throw {@link NoClassDefFoundError} the first time HBM's search box runs on a
 * client without nechar - even though the mod loaded fine and FML reported no missing
 * dependency. Resolving the method reflectively defers that linkage to runtime, where a
 * failure is just a cached {@code null} and we fall back to plain {@code String.contains}.
 *
 * <p>The fallback is deliberate and important: with nechar absent, behaviour must be
 * byte-for-byte what HBM did before. Pinyin is an enhancement, never a change in
 * semantics for players who have not opted in.
 */
public final class PinyinBridge {

    private PinyinBridge() {}

    /**
     * nechar's {@code Match.contains(String, CharSequence)}, or {@code null} when nechar
     * is absent. Resolved once: a failed lookup is cached as {@code null} so the
     * (relatively expensive) class lookup does not repeat on every keystroke.
     */
    private static Method necharContains;
    private static boolean necharResolved;

    /**
     * Replacement for {@code String.contains(CharSequence)} in HBM's search predicates.
     *
     * <p>Signature and return value deliberately mirror {@code String.contains} so that
     * the Mixin can swap one INVOKEVIRTUAL for one INVOKESTATIC without touching the
     * operand stack - {@code haystack} stays the receiver and {@code needle} stays the
     * single argument.
     *
     * @param haystack the string being searched (HBM's localized name, fluid name, ...)
     * @param needle   the text typed into the search box
     * @return the same result {@code haystack.contains(needle)} would give, or a pinyin
     *         match when nechar is present and the literal match failed
     */
    public static boolean contains(String haystack, CharSequence needle) {
        if (haystack == null) return false;

        // Cheap literal check first. Besides matching vanilla semantics exactly, this
        // keeps the common (non-pinyin) case free of any pinin work.
        if (haystack.contains(needle)) return true;

        Method match = necharContains();
        if (match == null) return false;

        try {
            Object result = match.invoke(null, haystack, needle);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable t) {
            // A nechar version that changed the signature, or pinin failing on odd input.
            // Never let a search box throw: degrade to "no match" and stop retrying.
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
                // nechar not installed, or a release whose API moved. Either way: fallback.
                necharContains = null;
            }
            necharResolved = true;
            return necharContains;
        }
    }

    /** True when nechar is present and usable. Used only for one-time startup logging. */
    public static boolean isActive() {
        return necharContains() != null;
    }
}
