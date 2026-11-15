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
 * Guards the Mixin injection points against silent breakage.
 *
 * <p>Why this test exists: if HBM renames {@code matchesSearch}, or moves the search into a
 * different method, a {@code @Redirect} simply does not apply. Mixin logs a debug line and
 * the build stays green, so pinyin search would quietly stop working with no error anywhere.
 * These assertions fail loudly at build time instead.
 *
 * <p>HBM is a hard dependency of this mod (see dependencies.gradle), so the targets are
 * referenced directly rather than reflectively. An earlier version loaded them by name and
 * returned early when absent, which meant a broken test classpath would silently turn this
 * whole guard into a no-op - exactly the failure mode it is supposed to catch. Direct
 * references cannot do that: they are resolved when this class is loaded, so a missing or
 * renamed target fails the test instead of skipping it.
 */
public class PinyinMixinTargetsTest {

    private static void requireMethod(Class<?> owner, String method, Class<?>... params) {
        try {
            Method m = owner.getDeclaredMethod(method, params);
            assertTrue("方法应存在: " + owner.getName() + "#" + method, m != null);
        } catch (NoSuchMethodException e) {
            fail("HBM 结构已变化，" + owner.getName() + "#" + method
                    + " 不存在了。mixins.hbmcompat.json 里的对应注入会静默失效，请更新 HbmPinyinMixins。");
        }
    }

    @Test
    public void genericRecipeMatchesSearchStillExists() {
        // 装配机 / 化工厂 / 电弧焊机等全部配方选择器共用的搜索入口。
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
        // 这两个是编译期生成的 lambda 方法，参数是内部类 CacheSlotDummy。
        requireMethod(ContainerPneumoStorageAccess.class, "lambda$rebuildClientIndex$1",
                ContainerPneumoStorageAccess.CacheSlotDummy.class);
        requireMethod(ContainerPneumoStorageAccess.class, "lambda$rebuildClientIndex$2",
                ContainerPneumoStorageAccess.CacheSlotDummy.class);
    }

    @Test
    public void structureFilterAcceptStillExists() throws Exception {
        // 结构方块"加载结构"界面的文件名过滤器是匿名 FileFilter，只能按名字取。
        Class<?> filter = Class.forName("com.hbm.blocks.generic.BlockWandStructure$GuiStructureLoad$1");
        requireMethod(filter, "accept", java.io.File.class);
        // 顺带固定住外层类的名字，Mixin 目标字符串是按它拼出来的。
        assertTrue(BlockWandStructure.class.getName().endsWith("BlockWandStructure"));
    }

    @Test
    public void bridgeNeverThrowsOnOddInput() {
        // 没装 nechar 时 contains 必须完全等价于 String.contains，不能抛异常。
        assertTrue(io.github.hbmcompat.client.PinyinBridge.contains("abc", "b"));
        assertTrue(io.github.hbmcompat.client.PinyinBridge.contains("abc", ""));
        assertFalse(io.github.hbmcompat.client.PinyinBridge.contains("abc", "z"));
        assertFalse(io.github.hbmcompat.client.PinyinBridge.contains(null, "z"));
    }
}
