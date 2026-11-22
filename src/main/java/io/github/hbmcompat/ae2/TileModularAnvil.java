package io.github.hbmcompat.ae2;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.IntStream;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.ForgeDirection;

import com.hbm.blocks.machine.NTMAnvil;

import appeng.api.config.InsertionMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Upgrades;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.parts.automation.StackUpgradeInventory;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.tile.grid.AENetworkInvTile;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.InventoryAdaptor;
import appeng.util.inv.AdaptorIInventory;
import appeng.util.inv.WrapperInventoryRange;
import io.github.hbmcompat.content.ModContent;
import io.github.hbmcompat.machine.ModularAnvilRecipe;
import io.github.hbmcompat.machine.PatternStacks;
import io.netty.buffer.ByteBuf;

public final class TileModularAnvil extends AENetworkInvTile implements ICraftingMachine {

    public static final int INPUTS = 16;
    public static final int OUTPUTS = 8;
    public static final int ANVIL_SLOT = INPUTS + OUTPUTS;
    public static final int PATTERN_SLOT = ANVIL_SLOT + 1;
    public static final int UPGRADE_SLOTS = 5;

    private static final int DONE = 100;
    private static final int[] SPEED = {10, 13, 17, 20, 25, 50};
    private static final int[] SIDED_SLOTS = IntStream.range(0, ANVIL_SLOT).toArray();
    private static final int[] OUTPUT_SLOTS = IntStream.range(INPUTS, ANVIL_SLOT).toArray();
    private static final int[] NO_SLOTS = {};

    private final AppEngInternalInventory inventory = new AppEngInternalInventory(this, PATTERN_SLOT + 1);
    private final StackUpgradeInventory upgrades =
            new StackUpgradeInventory(new ItemStack(ModContent.modularAnvil), this, UPGRADE_SLOTS);
    private final InventoryAdaptor outputBuffer =
            new AdaptorIInventory(new WrapperInventoryRange(inventory, OUTPUT_SLOTS, false));
    private final Deque<ItemStack> pending = new ArrayDeque<>();
    private ForgeDirection returnSide = ForgeDirection.UNKNOWN;
    private int progress;
    private boolean inputsChanged = true;
    private volatile Block displayAnvil;

    public TileModularAnvil() {
        getProxy().setIdlePowerUsage(0);
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection side) {
        return AECableType.COVERED;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public boolean canBeRotated() {
        return false;
    }

    @Override
    public IInventory getInternalInventory() {
        return inventory;
    }

    public IInventory getUpgrades() {
        return upgrades;
    }

    public int getProgress() {
        return progress;
    }

    public static int tier(ItemStack stack) {
        Block block = stack == null ? null : Block.getBlockFromItem(stack.getItem());
        return block instanceof NTMAnvil ? ((NTMAnvil) block).tier : -1;
    }

    public int getTier() {
        return tier(getStackInSlot(ANVIL_SLOT));
    }

    public Block getDisplayAnvil() {
        return displayAnvil;
    }

    private ICraftingPatternDetails processingPattern(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ICraftingPatternItem)) return null;
        ICraftingPatternDetails details =
                ((ICraftingPatternItem) stack.getItem()).getPatternForItem(stack, worldObj);
        return details != null && !details.isCraftable() && !details.isInputOnly() ? details : null;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack == null) return false;
        if (slot == ANVIL_SLOT) return tier(stack) >= 0;
        if (slot == PATTERN_SLOT) return processingPattern(stack) != null;
        return slot >= 0 && slot < INPUTS;
    }

    @Override
    public int[] getAccessibleSlotsBySide(ForgeDirection side) {
        return SIDED_SLOTS;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot >= INPUTS && slot < ANVIL_SLOT;
    }

    public int[] getAdapterOutputSlots() {
        return returnSide == ForgeDirection.UNKNOWN ? OUTPUT_SLOTS : NO_SLOTS;
    }

    @Override
    public void onChangeInventory(IInventory changed, int slot, InvOperation operation, ItemStack removed,
            ItemStack added) {
        if (changed == inventory && slot == ANVIL_SLOT && worldObj != null && !worldObj.isRemote
                && (removed != null || added != null)) {
            markForUpdate();
        }
        markDirty();
    }

    @Override
    public void markDirty() {
        inputsChanged = true;
        super.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (!super.isUseableByPlayer(player) || player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) > 64) {
            return false;
        }
        if (worldObj.isRemote) return true;
        if (!worldObj.canMineBlock(player, xCoord, yCoord, zCoord)) return false;
        IGridNode node = getActionableNode();
        return node == null || node.getGrid() == null
                || node.getGrid().<ISecurityGrid>getCache(ISecurityGrid.class)
                        .hasPermission(player, SecurityPermissions.BUILD);
    }

    private boolean idle() {
        return progress == 0 && returnSide == ForgeDirection.UNKNOWN;
    }

    public boolean isBusy() {
        return !idle() || !empty(0, ANVIL_SLOT) || getStackInSlot(PATTERN_SLOT) != null || getTier() < 0;
    }

    private boolean empty(int start, int end) {
        for (int slot = start; slot < end; slot++) {
            if (inventory.getStackInSlot(slot) != null) return false;
        }
        return true;
    }

    @Override
    public boolean acceptsPlans() {
        // Always claim the crafting-machine path; stock interfaces must not fall back to raw item insertion.
        return true;
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails details, InventoryCrafting table, ForgeDirection side) {
        if (isBusy()) return false;
        return push(ModularAnvilRecipe.match(details, getTier(), false), PatternStacks.fromInventory(table, true), side);
    }

    public boolean push(ModularAnvilRecipe recipe, PatternStacks supplied, ForgeDirection side) {
        if (isBusy() || recipe == null || !recipe.accepts(supplied)) return false;
        start(recipe, side);
        return true;
    }

    private void start(ModularAnvilRecipe recipe, ForgeDirection side) {
        pending.addAll(recipe.rollOutputs(worldObj.rand));
        returnSide = side;
        progress = 1;
        markDirty();
    }

    @TileEvent(TileEventType.TICK)
    public void tick() {
        if (worldObj.isRemote) return;
        if (progress > 0) {
            int speed = SPEED[Math.min(UPGRADE_SLOTS, upgrades.getInstalledUpgrades(Upgrades.SPEED))];
            progress = Math.min(DONE, progress + speed);
            if (progress < DONE) return;

            while (!pending.isEmpty()) {
                ItemStack remainder = outputBuffer.addItems(pending.peek().copy());
                if (remainder != null) {
                    pending.removeFirst();
                    pending.addFirst(remainder);
                    break;
                }
                pending.removeFirst();
            }
            if (pending.isEmpty()) {
                progress = 0;
                markDirty();
            }
        }
        if (returnSide != ForgeDirection.UNKNOWN) returnProducts();
        if (idle() && inputsChanged) {
            inputsChanged = false;
            startLocalRecipe();
        }
    }

    private void startLocalRecipe() {
        ItemStack pattern = getStackInSlot(PATTERN_SLOT);
        if (pattern == null || getTier() < 0 || empty(0, INPUTS)) return;
        ModularAnvilRecipe recipe = ModularAnvilRecipe.match(processingPattern(pattern), getTier(), true);
        if (recipe == null) return;
        ItemStack[] stock = new ItemStack[INPUTS];
        for (int slot = 0; slot < INPUTS; slot++) stock[slot] = inventory.getStackInSlot(slot);
        ItemStack[] remaining = recipe.remainingInputs(stock);
        if (remaining == null) return;
        for (int slot = 0; slot < INPUTS; slot++) {
            if (!ItemStack.areItemStacksEqual(stock[slot], remaining[slot])) {
                inventory.setInventorySlotContents(slot, remaining[slot]);
            }
        }
        start(recipe, ForgeDirection.UNKNOWN);
    }

    private void returnProducts() {
        if (empty(INPUTS, ANVIL_SLOT)) {
            if (progress == 0) {
                returnSide = ForgeDirection.UNKNOWN;
                markDirty();
            }
            return;
        }

        int x = xCoord + returnSide.offsetX;
        int y = yCoord + returnSide.offsetY;
        int z = zCoord + returnSide.offsetZ;
        if (!worldObj.blockExists(x, y, z)) return;
        TileEntity tile = worldObj.getTileEntity(x, y, z);
        InventoryAdaptor target = tile == null ? null : InventoryAdaptor.getAdaptor(tile, returnSide.getOpposite());
        if (target == null) return;
        for (int slot : OUTPUT_SLOTS) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null) continue;
            ItemStack remainder = target.addItems(stack.copy(), InsertionMode.DEFAULT);
            if (remainder == null || remainder.stackSize != stack.stackSize) {
                inventory.setInventorySlotContents(slot, remainder);
            }
        }
    }

    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void writeAnvil(NBTTagCompound data) {
        upgrades.writeToNBT(data, "upgrades");
        data.setInteger("progress", progress);
        data.setInteger("returnSide", returnSide.ordinal());
        NBTTagList items = new NBTTagList();
        for (ItemStack stack : pending) items.appendTag(stack.writeToNBT(new NBTTagCompound()));
        data.setTag("pending", items);
    }

    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readAnvil(NBTTagCompound data) {
        upgrades.readFromNBT(data, "upgrades");
        progress = data.getInteger("progress");
        returnSide = data.hasKey("returnSide") ? ForgeDirection.getOrientation(data.getInteger("returnSide"))
                : ForgeDirection.UNKNOWN;
        pending.clear();
        NBTTagList items = data.getTagList("pending", NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(items.getCompoundTagAt(i));
            if (stack != null) pending.add(stack);
        }
        inputsChanged = true;
    }

    @TileEvent(TileEventType.NETWORK_WRITE)
    public void writeDisplayAnvil(ByteBuf data) {
        ItemStack stack = getStackInSlot(ANVIL_SLOT);
        Block block = stack == null ? null : Block.getBlockFromItem(stack.getItem());
        data.writeShort(block instanceof NTMAnvil ? Block.getIdFromBlock(block) : 0);
    }

    @TileEvent(TileEventType.NETWORK_READ)
    public boolean readDisplayAnvil(ByteBuf data) {
        Block block = Block.getBlockById(data.readUnsignedShort());
        Block previous = displayAnvil;
        displayAnvil = block instanceof NTMAnvil ? block : null;
        return previous != displayAnvil;
    }

    @Override
    public void getDrops(World world, int x, int y, int z, List<ItemStack> drops) {
        super.getDrops(world, x, y, z, drops);
        for (int slot = 0; slot < UPGRADE_SLOTS; slot++) {
            ItemStack stack = upgrades.getStackInSlot(slot);
            if (stack != null) drops.add(stack);
        }
        drops.addAll(pending);
    }
}
