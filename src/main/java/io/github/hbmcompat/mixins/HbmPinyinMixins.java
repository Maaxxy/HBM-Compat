package io.github.hbmcompat.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.hbm.inventory.container.ContainerPneumoStorageAccess;
import com.hbm.inventory.gui.GUIAnvil;
import com.hbm.inventory.gui.GUIMachineAmmoPress;
import com.hbm.inventory.gui.GUIScreenFluid;
import com.hbm.inventory.gui.GUITransporterLinker;
import com.hbm.inventory.recipes.loader.GenericRecipe;

import io.github.hbmcompat.client.PinyinBridge;

/**
 * Adds pinyin search to HBM's own search boxes by rerouting their {@code String.contains}
 * calls through {@link PinyinBridge}.
 *
 * <p>Why {@code @Redirect} on the call site rather than {@code @Overwrite} or an
 * {@code @Inject} into each method: HBM's searches are plain filters
 * ({@code name.toLowerCase(Locale.US).contains(query.toLowerCase(Locale.US))}), and the
 * only thing that needs to change is what "contains" means. Redirecting the single
 * INVOKEVIRTUAL keeps every other line of HBM's logic - the lowercasing, the locale, the
 * list rebuilding, the paging reset - exactly as shipped. There is no HBM code copied or
 * maintained here, so an HBM update that rewrites these methods' bodies still works as
 * long as it keeps calling {@code String.contains} with a String receiver.
 *
 * <p>Targets are named via {@code value = X.class} rather than a {@code targets} string
 * wherever the class is public: HBM is a hard dependency, so the reference costs nothing
 * and buys a compile error if HBM ever renames or moves a target. Only the one
 * package-private target below has to fall back to a string name.
 *
 * <p>All targets live in {@code com.hbm.inventory.*}, which nechar never touches (its
 * default list covers AE2, ExtraCells, ProjectRed, LogisticsPipes, ... but no HBM class).
 * The two therefore coexist without fighting over the same call sites.
 *
 * <p>Server safety: these are client-only GUI classes, and the Mixin config lists every
 * entry under {@code "client"}, so a dedicated server never loads them.
 */
public final class HbmPinyinMixins {

    private HbmPinyinMixins() {}

    /** {@code GenericRecipe.matchesSearch} - the recipe picker shared by the assembler,
     *  large assembler, chemical plant, large chemical plant, arc welder, soldering
     *  station and acidizer. This one Mixin is what makes recipe names searchable. */
    @Mixin(remap = false, value = GenericRecipe.class)
    public static class GenericRecipeMixin {

        @Redirect(remap = false,
                method = "matchesSearch",
                at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
        private boolean hbmcompat$pinyinSearch(String haystack, CharSequence needle) {
            return PinyinBridge.contains(haystack, needle);
        }
    }

    /** Anvil template search. */
    @Mixin(remap = false, value = GUIAnvil.class)
    public static class GuiAnvilMixin {

        @Redirect(remap = false,
                method = "search",
                at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
        private boolean hbmcompat$pinyinSearch(String haystack, CharSequence needle) {
            return PinyinBridge.contains(haystack, needle);
        }
    }

    /** Ammo press (ammo assembly machine) search. */
    @Mixin(remap = false, value = GUIMachineAmmoPress.class)
    public static class GuiMachineAmmoPressMixin {

        @Redirect(remap = false,
                method = "search",
                at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
        private boolean hbmcompat$pinyinSearch(String haystack, CharSequence needle) {
            return PinyinBridge.contains(haystack, needle);
        }
    }

    /** Fluid identifier / fluid screen search. */
    @Mixin(remap = false, value = GUIScreenFluid.class)
    public static class GuiScreenFluidMixin {

        @Redirect(remap = false,
                method = "updateSearch",
                at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
        private boolean hbmcompat$pinyinSearch(String haystack, CharSequence needle) {
            return PinyinBridge.contains(haystack, needle);
        }
    }

    /** Transporter linker destination search. */
    @Mixin(remap = false, value = GUITransporterLinker.class)
    public static class GuiTransporterLinkerMixin {

        @Redirect(remap = false,
                method = "updateSearch",
                at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
        private boolean hbmcompat$pinyinSearch(String haystack, CharSequence needle) {
            return PinyinBridge.contains(haystack, needle);
        }
    }

    /** Pneumatic storage access terminal search. The filtering happens in two synthetic
     *  lambda methods rather than a named search method, hence the {@code lambda$} names. */
    @Mixin(remap = false, value = ContainerPneumoStorageAccess.class)
    public static class ContainerPneumoStorageAccessMixin {

        @Redirect(remap = false,
                method = "lambda$rebuildClientIndex$1",
                at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
        private boolean hbmcompat$pinyinSearch1(String haystack, CharSequence needle) {
            return PinyinBridge.contains(haystack, needle);
        }

        @Redirect(remap = false,
                method = "lambda$rebuildClientIndex$2",
                at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
        private boolean hbmcompat$pinyinSearch2(String haystack, CharSequence needle) {
            return PinyinBridge.contains(haystack, needle);
        }
    }

    /**
     * Structure wand's "load structure" file browser.
     *
     * <p>The filter is an anonymous {@link java.io.FileFilter} compiled as
     * {@code BlockWandStructure$GuiStructureLoad$1}, so the method to hit is its
     * {@code accept(File)}. Only the file *name* is matched, never the path, so pinyin here
     * searches structure names the player saved in Chinese.
     *
     * <p>This is the one target that cannot use {@code value = ...}: the class is an
     * anonymous inner class and therefore package-private, so Java source has no way to
     * name it. A string target is the only option here.
     *
     * <p>Note the target is the synthetic inner class, not {@code GuiStructureLoad} itself:
     * the outer class only owns the {@code textName} field, the comparison lives in the
     * filter. Targeting the outer class would silently do nothing.
     */
    @Mixin(remap = false, targets = "com.hbm.blocks.generic.BlockWandStructure$GuiStructureLoad$1")
    public static class GuiStructureLoadFilterMixin {

        @Redirect(remap = false,
                method = "accept",
                at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
        private boolean hbmcompat$pinyinSearch(String haystack, CharSequence needle) {
            return PinyinBridge.contains(haystack, needle);
        }
    }
}
