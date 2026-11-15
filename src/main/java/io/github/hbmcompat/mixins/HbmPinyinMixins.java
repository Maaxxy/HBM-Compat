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
 * Reroutes HBM's own {@code String.contains} search calls through {@link PinyinBridge}.
 *
 * <p>{@code @Redirect} on the call site rather than {@code @Overwrite}: HBM's searches are
 * plain filters, and only the meaning of "contains" needs to change, so all surrounding
 * logic (lowercasing, locale, list rebuilding, paging) stays exactly as shipped and an HBM
 * update that rewrites a method body still works.
 *
 * <p>Targets use {@code value = X.class} where the class is public, so renaming one becomes
 * a compile error instead of a silently dead injection. One anonymous inner class has to use
 * a string name because source cannot reference it.
 *
 * <p>nechar never touches these classes (its default list covers AE2, ExtraCells,
 * ProjectRed, LogisticsPipes, but no HBM class), so the two coexisting is safe.
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

    /** Pneumatic storage access terminal search. Two synthetic lambda methods, hence the
     *  {@code lambda$} names. */
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
     * Structure wand's "load structure" file browser. The comparison lives in an anonymous
     * {@link java.io.FileFilter}'s {@code accept(File)}, not in {@code GuiStructureLoad}
     * itself, and only the file name is matched. It stays a string target because source
     * cannot name the anonymous class.
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
