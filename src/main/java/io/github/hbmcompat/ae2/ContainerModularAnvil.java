package io.github.hbmcompat.ae2;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.SlotNormal;
import appeng.container.slot.SlotOutput;
import appeng.container.slot.SlotRestrictedInput;
import appeng.container.slot.SlotRestrictedInput.PlacableItemType;

public final class ContainerModularAnvil extends AEBaseContainer {

    public static final int ANVIL_X = 10;
    public static final int PATTERN_X = 104;
    public static final int MODULE_Y = 21;
    public static final int PLAYER_INVENTORY_Y = 133;

    private final TileModularAnvil tile;

    @GuiSync(0)
    public int progress;

    public ContainerModularAnvil(InventoryPlayer player, TileModularAnvil tile) {
        super(player, tile, null);
        this.tile = tile;
        addModuleSlot(TileModularAnvil.ANVIL_SLOT, ANVIL_X);
        addModuleSlot(TileModularAnvil.PATTERN_SLOT, PATTERN_X);
        for (int i = 0; i < TileModularAnvil.UPGRADE_SLOTS; i++) {
            addSlotToContainer(new SlotRestrictedInput(PlacableItemType.UPGRADES, tile.getUpgrades(),
                    i, 187, 8 + i * 18, player).setNotDraggable());
        }
        for (int i = 0; i < TileModularAnvil.INPUTS; i++) {
            addSlotToContainer(new SlotNormal(tile, i, 10 + (i % 4) * 18, 44 + (i / 4) * 18));
        }
        for (int i = 0; i < TileModularAnvil.OUTPUTS; i++) {
            addSlotToContainer(new SlotOutput(tile, TileModularAnvil.INPUTS + i,
                    129 + (i % 2) * 18, 44 + (i / 2) * 18, -1));
        }
        bindPlayerInventory(player, 0, PLAYER_INVENTORY_Y);
    }

    private void addModuleSlot(int slot, int x) {
        addSlotToContainer(new SlotNormal(tile, slot, x, MODULE_Y) {
            @Override
            public int getSlotStackLimit() {
                return 1;
            }

            @Override
            public boolean isItemValid(ItemStack stack) {
                return super.isItemValid(stack) && tile.isItemValidForSlot(slot, stack);
            }
        });
    }

    @Override
    public void detectAndSendChanges() {
        progress = tile.getProgress();
        super.detectAndSendChanges();
    }
}
