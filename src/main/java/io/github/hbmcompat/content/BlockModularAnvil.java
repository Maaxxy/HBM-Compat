package io.github.hbmcompat.content;

import java.util.EnumSet;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import appeng.block.AEBaseTileBlock;
import appeng.core.features.AEFeature;
import appeng.core.features.ActivityState;
import appeng.core.features.BlockStackSrc;
import appeng.tile.AEBaseTile;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.hbmcompat.HbmCompat;
import io.github.hbmcompat.ae2.AdapterGuiHandler;
import io.github.hbmcompat.ae2.TileModularAnvil;
import io.github.hbmcompat.client.RenderModularAnvil;

public final class BlockModularAnvil extends AEBaseTileBlock {

    public static final String REGISTRY_NAME = "modular_anvil";

    public BlockModularAnvil() {
        super(Material.iron);
        setBlockName(REGISTRY_NAME);
        setBlockTextureName(HbmCompat.MODID + ":" + REGISTRY_NAME);
        setHardness(4);
        setHarvestLevel("pickaxe", 1);
        isOpaque = false;
        isFullSize = false;
        setLightOpacity(0);
        setTileEntity(TileModularAnvil.class);
        AEBaseTile.registerTileItem(TileModularAnvil.class, new BlockStackSrc(this, 0, ActivityState.Enabled));
        setFeature(EnumSet.of(AEFeature.Core));
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected RenderModularAnvil getRenderer() {
        return new RenderModularAnvil();
    }

    @Override
    public boolean onActivated(World world, int x, int y, int z, EntityPlayer player, int side,
            float hitX, float hitY, float hitZ) {
        TileModularAnvil tile = getTileEntity(world, x, y, z);
        if (tile == null || player.isSneaking()) return false;
        if (!world.isRemote && tile.isUseableByPlayer(player)) AdapterGuiHandler.open(player, tile);
        return true;
    }
}
