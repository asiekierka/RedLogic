package mods.immibis.redlogic.interaction;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import mods.immibis.core.api.util.Dir;
import mods.immibis.redlogic.RedLogicMod;
import mods.immibis.redlogic.lamps.BlockLampCube;

public class RenderLumarButtonStatic {

	public static boolean renderWorldBlock(RenderBlocks render, IBlockAccess world, int x, int y, int z, Block block, int model) {
		TileLumarButton t = (TileLumarButton)world.getTileEntity(x, y, z);
		render.renderStandardBlock(block, x, y, z);
		if(!render.hasOverrideBlockTexture())
			render(render, t.getColour(), t.getSide(), x, y, z, t.isLit(), t.getModel());
		return true;
	}

	public static void renderInvBlock(RenderBlocks render, Block block, int meta, int _model) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
		
		LumarButtonModel model = LumarButtonModel.VALUES[((meta>>8) & 15) % LumarButtonModel.VALUES.length];
		
		AxisAlignedBB bb = TileLumarButton.getBoundingBox(Dir.NX, false, model);
		bb.maxX += (bb.maxX - bb.minX); // make it twice as thick, like vanilla buttons
		GL11.glTranslated(-(bb.minX + bb.maxX)/2, -0.5, -0.5);
		render.setRenderBounds(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
		
		Tessellator.instance.startDrawingQuads();
		Tessellator.instance.setColorOpaque_I(0xFFFFFF);
		Tessellator.instance.setBrightness(0x00F000F0);
		Tessellator.instance.setNormal(0.0F, -1.0F, 0.0F);
		render.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, render.getBlockIconFromSide(block, 0));
        Tessellator.instance.setNormal(0.0F, 1.0F, 0.0F);
        render.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, render.getBlockIconFromSide(block, 1));
        Tessellator.instance.setNormal(0.0F, 0.0F, -1.0F);
        render.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, render.getBlockIconFromSide(block, 2));
        Tessellator.instance.setNormal(0.0F, 0.0F, 1.0F);
        render.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, render.getBlockIconFromSide(block, 3));
        Tessellator.instance.setNormal(-1.0F, 0.0F, 0.0F);
        render.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, render.getBlockIconFromSide(block, 4));
        Tessellator.instance.setNormal(1.0F, 0.0F, 0.0F);
        render.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, render.getBlockIconFromSide(block, 5));
        render(render, meta & 15, Dir.NX, 0, 0, 0, true, model);
		Tessellator.instance.draw();
	}

	private static void render(RenderBlocks rb, int colour, int side, double x, double y, double z, boolean lit, LumarButtonModel model) {
		if(lit) {
			Tessellator.instance.setBrightness(0x00F000F0);
			Tessellator.instance.setColorOpaque_I(BlockLampCube.COLOURS[colour]);
		} else {
			if(rb.blockAccess != null)
				Tessellator.instance.setBrightness(RedLogicMod.lumarButton.getMixedBrightnessForBlock(rb.blockAccess, (int)x, (int)y, (int)z));
			Tessellator.instance.setColorOpaque_I(BlockLampCube.getOffColour(BlockLampCube.COLOURS[colour]));
		}
		
		IIcon iGlowPart = model.glowIcon;
		
		switch(side) {
		case Dir.PX: rb.renderFaceXNeg(RedLogicMod.lumarButton, x, y, z, iGlowPart); break;
		case Dir.NX: rb.renderFaceXPos(RedLogicMod.lumarButton, x, y, z, iGlowPart); break;
		case Dir.PY: rb.renderFaceYNeg(RedLogicMod.lumarButton, x, y, z, iGlowPart); break;
		case Dir.NY: rb.renderFaceYPos(RedLogicMod.lumarButton, x, y, z, iGlowPart); break;
		case Dir.PZ: rb.renderFaceZNeg(RedLogicMod.lumarButton, x, y, z, iGlowPart); break;
		case Dir.NZ: rb.renderFaceZPos(RedLogicMod.lumarButton, x, y, z, iGlowPart); break;
		}
	}
	
}
