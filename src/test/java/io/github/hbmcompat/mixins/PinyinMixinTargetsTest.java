package io.github.hbmcompat.mixins;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import org.junit.Test;

import com.hbm.blocks.generic.BlockWandStructure;
import com.hbm.inventory.container.ContainerPneumoStorageAccess;
import com.hbm.inventory.gui.GUIAnvil;
import com.hbm.inventory.gui.GUIMachineAmmoPress;
import com.hbm.inventory.gui.GUIScreenFluid;
import com.hbm.inventory.gui.GUITransporterLinker;
import com.hbm.inventory.recipes.loader.GenericRecipe;

/**
 * Guards the mixin injection points: if HBM renames a target method the {@code @Redirect}
 * silently stops applying and the build would stay green. Targets are referenced directly
 * (HBM is a hard dependency) so a missing one fails here instead of being skipped.
 */
public class PinyinMixinTargetsTest {

    private static void requireMethod(Class<?> owner, String method, Class<?>... params) {
        try {
            Method m = owner.getDeclaredMethod(method, params);
            assertTrue("方法应存在: " + owner.getName() + "#" + method, m != null);
        } catch (NoSuchMethodException e) {
            fail("HBM 结构已变化，" + owner.getName() + "#" + method
                    + " 不存在了。HbmPinyinMixins 里对应的 @Redirect 会静默失效，请更新。");
        }
    }

    @Test
    public void genericRecipeMatchesSearchStillExists() {
        // 所有配方选择器共用的搜索入口。
        requireMethod(GenericRecipe.class, "matchesSearch", String.class);
    }

    @Test
    public void anvilSearchStillExists() {
        requireMethod(GUIAnvil.class, "search", String.class);
    }

    @Test
    public void ammoPressSearchStillExists() {
        requireMethod(GUIMachineAmmoPress.class, "search", String.class);
    }

    @Test
    public void fluidScreenUpdateSearchStillExists() {
        requireMethod(GUIScreenFluid.class, "updateSearch");
    }

    @Test
    public void transporterLinkerUpdateSearchStillExists() {
        requireMethod(GUITransporterLinker.class, "updateSearch");
    }

    @Test
    public void pneumaticStorageLambdasStillExist() {
        // 编译期生成的 lambda 方法，参数是内部类 CacheSlotDummy。
        requireMethod(ContainerPneumoStorageAccess.class, "lambda$rebuildClientIndex$1",
                ContainerPneumoStorageAccess.CacheSlotDummy.class);
        requireMethod(ContainerPneumoStorageAccess.class, "lambda$rebuildClientIndex$2",
                ContainerPneumoStorageAccess.CacheSlotDummy.class);
    }

    @Test
    public void structureFilterAcceptStillExists() throws Exception {
        // 匿名 FileFilter，只能按名字取。
        Class<?> filter = Class.forName("com.hbm.blocks.generic.BlockWandStructure$GuiStructureLoad$1");
        requireMethod(filter, "accept", java.io.File.class);
        assertTrue(BlockWandStructure.class.getName().endsWith("BlockWandStructure"));
    }

    @Test
    public void bridgeNeverThrowsOnOddInput() {
        // 没装 nechar 时必须完全等价于 String.contains，不能抛异常。
        assertTrue(io.github.hbmcompat.client.PinyinBridge.contains("abc", "b"));
        assertTrue(io.github.hbmcompat.client.PinyinBridge.contains("abc", ""));
        assertFalse(io.github.hbmcompat.client.PinyinBridge.contains("abc", "z"));
        assertFalse(io.github.hbmcompat.client.PinyinBridge.contains(null, "z"));
    }
}
