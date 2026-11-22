package io.github.hbmcompat.client;

import java.util.EnumSet;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.model.obj.TextureCoordinate;
import net.minecraftforge.client.model.obj.Vertex;
import net.minecraftforge.common.util.ForgeDirection;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.S_Face;
import com.hbm.render.loader.S_GroupObject;

import appeng.client.render.BaseBlockRender;
import io.github.hbmcompat.ae2.TileModularAnvil;
import io.github.hbmcompat.content.BlockModularAnvil;

public final class RenderModularAnvil extends BaseBlockRender<BlockModularAnvil, TileModularAnvil> {

    private static final EnumSet<ForgeDirection> FACES = EnumSet.range(ForgeDirection.DOWN, ForgeDirection.EAST);
    private static final int[][] FRAME = {
            {0, 0, 0, 3, 16, 3}, {13, 0, 0, 16, 16, 3},
            {0, 0, 13, 3, 16, 16}, {13, 0, 13, 16, 16, 16},
            {3, 0, 0, 13, 2, 2}, {3, 0, 14, 13, 2, 16},
            {0, 0, 3, 2, 2, 13}, {14, 0, 3, 16, 2, 13},
            {3, 14, 0, 13, 16, 2}, {3, 14, 14, 13, 16, 16},
            {0, 14, 3, 2, 16, 13}, {14, 14, 3, 16, 16, 13},
            {2, 1, 6, 14, 2, 10}
    };

    public RenderModularAnvil() {
        super(false, 20);
    }

    @Override
    public void renderInventory(BlockModularAnvil block, ItemStack stack, RenderBlocks renderer,
            ItemRenderType type, Object[] data) {
        for (int[] rail : FRAME) {
            setBounds(renderer, rail);
            renderInvBlock(FACES, block, stack, Tessellator.instance, 0xffffff, renderer);
        }
        renderer.setRenderBoundsFromBlock(block);
    }

    @Override
    public boolean renderInWorld(BlockModularAnvil block, IBlockAccess world, int x, int y, int z,
            RenderBlocks renderer) {
        boolean allFaces = renderer.renderAllFaces;
        renderer.renderAllFaces = true;
        try {
            for (int[] rail : FRAME) {
                setBounds(renderer, rail);
                renderer.renderStandardBlock(block, x, y, z);
            }
            TileModularAnvil tile = block.getTileEntity(world, x, y, z);
            Block module = tile == null ? null : tile.getDisplayAnvil();
            if (module != null && !renderer.hasOverrideBlockTexture()) {
                renderModule(module, renderer, x, y, z, block.getMixedBrightnessForBlock(world, x, y, z));
            }
        } finally {
            renderer.renderAllFaces = allFaces;
            renderer.setRenderBoundsFromBlock(block);
        }
        return true;
    }

    private void setBounds(RenderBlocks renderer, int[] box) {
        setInvRenderBounds(renderer, box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    // Append the scaled HBM model to the chunk's existing block-atlas batch.
    private static void renderModule(Block module, RenderBlocks renderer, int x, int y, int z, int brightness) {
        IIcon top = renderer.getBlockIconFromSide(module, 1);
        IIcon body = renderer.getBlockIconFromSide(module, 0);
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(brightness);
        for (S_GroupObject part : ResourceManager.anvil.groupObjects) {
            IIcon icon = "Top".equals(part.name) ? top : body;
            for (S_Face face : part.faces) {
                Vertex normal = face.faceNormal;
                tessellator.setNormal(normal.x, normal.y, normal.z);
                float shade = 0.7F + 0.3F * normal.y;
                tessellator.setColorOpaque_F(shade, shade, shade);
                for (int i = 0; i < 4; i++) {
                    int vertexIndex = Math.min(i, face.vertices.length - 1);
                    Vertex vertex = face.vertices[vertexIndex];
                    TextureCoordinate uv = face.textureCoordinates[vertexIndex];
                    tessellator.addVertexWithUV(x + 0.5 + vertex.x * 0.65, y + 0.125 + vertex.y * 0.65,
                            z + 0.5 + vertex.z * 0.65, icon.getInterpolatedU(16 * uv.u),
                            icon.getInterpolatedV(16 * uv.v));
                }
            }
        }
    }
}
