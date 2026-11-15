package io.github.hbmcompat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * Registers HBM Compat's mixins in the late phase.
 *
 * <p>{@code @LateMixin} is mandatory: GTNHMixins discovers late loaders by scanning ASMData
 * for it, so an {@link ILateMixinLoader} without the annotation is silently ignored.
 *
 * <p>Late loading is also required, not just preferred: manifest {@code MixinConfigs} are
 * queued in PREINIT, before FML discovers ordinary mods, so mixing into {@code com.hbm.*}
 * there fails target validation. This interface queues the config once candidate mods are known.
 */
@LateMixin
public class HbmCompatCorePlugin implements IFMLLoadingPlugin, ILateMixinLoader {

    private static final String MIXIN_CONFIG = "mixins.hbmcompat.late.json";

    /** Class names relative to the config's {@code package}. */
    private static final List<String> MIXINS = Collections.unmodifiableList(
            Arrays.asList(
                    "HbmPinyinMixins$GenericRecipeMixin",
                    "HbmPinyinMixins$GuiAnvilMixin",
                    "HbmPinyinMixins$GuiMachineAmmoPressMixin",
                    "HbmPinyinMixins$GuiScreenFluidMixin",
                    "HbmPinyinMixins$GuiTransporterLinkerMixin",
                    "HbmPinyinMixins$ContainerPneumoStorageAccessMixin",
                    "HbmPinyinMixins$GuiStructureLoadFilterMixin"));

    @Override
    public String getMixinConfig() {
        return MIXIN_CONFIG;
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        // Pinyin search is client-only, so a dedicated server gets no mixins at all rather
        // than risking GUI targets. If side detection fails, assume server.
        return isClient() ? MIXINS : Collections.<String>emptyList();
    }

    private static boolean isClient() {
        try {
            return FMLCommonHandler.instance().getSide().isClient();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(java.util.Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
