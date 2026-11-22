package io.github.hbmcompat.machine;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.hbm.inventory.fluid.tank.FluidTank;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import io.github.hbmcompat.ae2.TileModularAnvil;
import io.github.hbmcompat.content.BlockModularAnvil;

final class ModularAnvilDriver implements IHbmMachineDriver {

    private static final FluidTank[] NO_TANKS = {};

    @Override
    public String getMachineId() {
        return BlockModularAnvil.REGISTRY_NAME;
    }

    @Override
    public boolean supports(TileEntity tile) {
        return tile instanceof TileModularAnvil;
    }

    @Override
    public HbmRecipeMatch match(ICraftingPatternDetails details) {
        return match(details, ModularAnvilRecipe.ANY_TIER);
    }

    @Override
    public HbmRecipeMatch match(TileEntity tile, ICraftingPatternDetails details) {
        int tier = ((TileModularAnvil) tile).getTier();
        return tier < 0 ? null : match(details, tier);
    }

    private HbmRecipeMatch match(ICraftingPatternDetails details, int tier) {
        ModularAnvilRecipe recipe = ModularAnvilRecipe.match(details, tier, false);
        return recipe == null ? null : new HbmRecipeMatch(this, recipe, "auto");
    }

    @Override
    public boolean isBusy(TileEntity tile) {
        return ((TileModularAnvil) tile).isBusy();
    }

    @Override
    public boolean isBusy(TileEntity tile, FeedingMode mode) {
        return isBusy(tile);
    }

    @Override
    public boolean push(TileEntity tile, HbmRecipeMatch match, PatternStacks suppliedInputs) {
        return ((TileModularAnvil) tile).push((ModularAnvilRecipe) match.getRecipe(), suppliedInputs,
                ForgeDirection.UNKNOWN);
    }

    @Override
    public int[] getOutputSlots(TileEntity tile) {
        return ((TileModularAnvil) tile).getAdapterOutputSlots();
    }

    @Override
    public FluidTank[] getInputTanks(TileEntity tile) {
        return NO_TANKS;
    }

    @Override
    public FluidTank[] getOutputTanks(TileEntity tile) {
        return NO_TANKS;
    }
}
