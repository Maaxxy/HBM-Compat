package io.github.hbmcompat.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import appeng.client.gui.AEBaseGui;
import appeng.core.localization.ColorUtils;
import codechicken.nei.api.API;
import codechicken.nei.api.INEIGuiAdapter;
import io.github.hbmcompat.ae2.ContainerModularAnvil;
import io.github.hbmcompat.ae2.TileModularAnvil;

public final class GuiModularAnvil extends AEBaseGui {

    private static final String TEXTURE = "guis/mac.png";
    private static final int PANEL_X = 179;
    private static final int PANEL_WIDTH = 32;
    private static final int PANEL_HEIGHT = 14 + TileModularAnvil.UPGRADE_SLOTS * 18;

    private final ContainerModularAnvil container;
    private final TileModularAnvil tile;

    public GuiModularAnvil(InventoryPlayer player, TileModularAnvil tile) {
        this(new ContainerModularAnvil(player, tile), tile);
    }

    private GuiModularAnvil(ContainerModularAnvil container, TileModularAnvil tile) {
        super(container);
        this.container = container;
        this.tile = tile;
        xSize = 176;
        ySize = ContainerModularAnvil.PLAYER_INVENTORY_Y + 83;
    }

    public static void registerNeiExclusion() {
        API.registerNEIGuiHandler(new INEIGuiAdapter() {
            @Override
            public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
                return gui instanceof GuiModularAnvil && ((GuiModularAnvil) gui).overlapsCards(x, y, w, h);
            }
        });
    }

    private boolean overlapsCards(int x, int y, int w, int h) {
        int left = guiLeft + PANEL_X;
        return x < left + PANEL_WIDTH && x + w > left && y < guiTop + PANEL_HEIGHT && y + h > guiTop;
    }

    // Vanilla tests xSize when handling clicks and releases, while NEI uses it to lay out both panels.
    @Override
    protected void mouseClicked(int x, int y, int button) {
        int width = xSize;
        if (overlapsCards(x, y, 1, 1)) xSize = PANEL_X + PANEL_WIDTH;
        try {
            super.mouseClicked(x, y, button);
        } finally {
            xSize = width;
        }
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        int width = xSize;
        if (overlapsCards(x, y, 1, 1)) xSize = PANEL_X + PANEL_WIDTH;
        try {
            super.mouseMovedOrUp(x, y, button);
        } finally {
            xSize = width;
        }
    }

    @Override
    public void drawFG(int x, int y, int mouseX, int mouseY) {
        int tier = tile.getTier();
        text(getGuiDisplayName(StatCollector.translateToLocal("tile.modular_anvil.name")), 8, 7);
        int labelY = ContainerModularAnvil.MODULE_Y + 4;
        text(tier < 0 ? StatCollector.translateToLocal("hbmcompat.anvil.module")
                : StatCollector.translateToLocalFormatted("hbmcompat.anvil.tier", tier),
                ContainerModularAnvil.ANVIL_X + 21, labelY);
        text(StatCollector.translateToLocal("hbmcompat.anvil.pattern"), ContainerModularAnvil.PATTERN_X + 21, labelY);
        text(StatCollector.translateToLocal("container.inventory"), 8, ContainerModularAnvil.PLAYER_INVENTORY_Y - 12);
    }

    private void text(String text, int x, int y) {
        fontRendererObj.drawString(text, x, y, ColorUtils.guiTextColorGray.getColor());
    }

    @Override
    public void drawBG(int x, int y, int mouseX, int mouseY) {
        bindTexture(TEXTURE);
        int inventoryTop = ySize - 97;
        drawTexturedModalRect(x, y, 0, 0, 176, 15);
        func_152125_a(x, y + 15, 0, 84, 176, 16, 176, inventoryTop - 15, 256, 256);
        drawTexturedModalRect(x, y + inventoryTop, 0, 100, 176, 97);
        drawTexturedModalRect(x + PANEL_X, y, PANEL_X, 0, PANEL_WIDTH, PANEL_HEIGHT);
        for (Object entry : container.inventorySlots) {
            Slot slot = (Slot) entry;
            if (slot.inventory == tile) {
                drawTexturedModalRect(x + slot.xDisplayPosition - 1, y + slot.yDisplayPosition - 1,
                        125, 15, 18, 18);
            }
        }
        drawTexturedModalRect(x + 88, y + 71, 82, 47, 37, 18);
        drawProgress(x + 88, y + 94);
    }

    private void drawProgress(int x, int y) {
        // Reuse the assembler's vertical meter as a horizontal bar.
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x + 36, y, 0);
            GL11.glScalef(2, 1, 1);
            GL11.glRotatef(90, 0, 0, 1);
            drawTexturedModalRect(0, 0, 148, 48, 6, 18);
            int fill = 18 * container.progress / 100;
            drawTexturedModalRect(0, 18 - fill, 148, 219 - fill, 6, fill);
        } finally {
            GL11.glPopMatrix();
        }
    }
}
