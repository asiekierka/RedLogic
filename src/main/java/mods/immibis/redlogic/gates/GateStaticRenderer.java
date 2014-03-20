package mods.immibis.redlogic.gates;

import static org.lwjgl.opengl.GL11.*;
import mods.immibis.core.api.porting.PortableBlockRenderer;
import mods.immibis.core.api.util.Dir;
import mods.immibis.redlogic.RedLogicMod;
import mods.immibis.redlogic.RotatedTessellator;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GateStaticRenderer implements PortableBlockRenderer {
	
	private GateRendering defaultRendering = new GateRendering.Default();
	
	private RotatedTessellator rotatedTessellator = new RotatedTessellator();
	
	public static final GateStaticRenderer instance = new GateStaticRenderer();

	@Override
	public boolean renderWorldBlock(RenderBlocks render, IBlockAccess world, int x, int y, int z, Block block, int model) {
		GateTile te = (GateTile)world.getTileEntity(x, y, z);
		int side = te.getSide();
		int front = te.getFront();
		EnumGates type = te.getType();
		
		GateRendering rendering = type.getRendering();
		
		rendering.set(te.getRenderState());
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
		
		
		if(side < 0 || side > 5 || front < 0 || front > 5) {
			// invalid orientation, make it obvious
			side = front = 0;
		}
		
		switch(side) {
		case Dir.NX: render.setRenderBounds(0, 0, 0, GateBlock.THICKNESS, 1, 1); break;
		case Dir.NY: render.setRenderBounds(0, 0, 0, 1, GateBlock.THICKNESS, 1); break;
		case Dir.NZ: render.setRenderBounds(0, 0, 0, 1, 1, GateBlock.THICKNESS); break;
		case Dir.PX: render.setRenderBounds(1 - GateBlock.THICKNESS, 0, 0, 1, 1, 1); break;
		case Dir.PY: render.setRenderBounds(0, 1 - GateBlock.THICKNESS, 0, 1, 1, 1); break;
		case Dir.PZ: render.setRenderBounds(0, 0, 1 - GateBlock.THICKNESS, 1, 1, 1); break;
		}
		
		rotatedTessellator.base = Tessellator.instance;
		rotatedTessellator.front = front;
		rotatedTessellator.side = side;
		rotatedTessellator.x = x;
		rotatedTessellator.y = y;
		rotatedTessellator.z = z;
		rotatedTessellator.flipped = te.isFlipped();
		
		if((side & 6) == (front & 6)) {
			// invalid orientation, make it obvious
			render.setRenderBounds(0, 0, 0, 1, 1, 1);
		}
		
		//System.out.println(side+" "+front+" -> "+rotationLookup[side][front]);
		
		
		if(render.hasOverrideBlockTexture())
		{
			GateBlock.renderSide = -100;
			GateBlock.textureOverride = null;
			GateBlock.colourOverride = -1;
			render.renderStandardBlock(block, x, y, z);
			return true;
		}
		
		GateBlock.renderSide = -(side^1) - 1;
		GateBlock.textureOverride = null;
		GateBlock.colourOverride = -1;
		render.renderStandardBlock(block, x, y, z);
		
		Tessellator.instance.setBrightness(world.getLightBrightnessForSkyBlocks(x, y, z, 0));
		
		for(int k = 0; k < rendering.segmentTex.length; k++) {
			Tessellator.instance.setColorOpaque_I(rendering.segmentCol[k]);
			renderGateSurface(block, side, front, rendering.segmentIcons[k]);
		}
		
		Tessellator.instance.setBrightness(world.getLightBrightnessForSkyBlocks(x, y, z, 0));
		Tessellator.instance.setColorRGBA(255, 255, 255, 255);
		
		rotatedTessellator.base = Tessellator.instance;
		for(int k = 0; k < rendering.torchState.length; k++) {
			float tx = rendering.torchX[k]/16f;
			float ty = rendering.torchY[k]/16f;
			boolean on = rendering.torchState[k];
			
			renderTorchAtAngle(render, on ? rendering.torchTexOn : rendering.torchTexOff, tx, ty, 3/16f);
		}
		
		for(int k = 0; k < rendering.pointerX.length; k++) {
			float tx = rendering.pointerX[k]/16f;
			float ty = rendering.pointerY[k]/16f;
			
			renderTorchAtAngle(render, rendering.torchTexOn, tx, ty, 0f);
		}
		
		rendering.customRender(rotatedTessellator, render);
		
		return true;
	}

	@Override
	public void renderInvBlock(RenderBlocks render, Block block, int meta, int model) {
		EnumGates type = EnumGates.VALUES[meta];
		GateRendering rendering = (type == null ? defaultRendering : type.getRendering());
		
		render.setRenderBounds(0, 0, 0, 1, GateBlock.THICKNESS, 1);
		
		rendering.setItemRender();
		
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
		
		Tessellator t = Tessellator.instance;
		
		GateBlock.textureOverride = null;
		GateBlock.renderTypeOverride = 0;
		
		glAlphaFunc(GL_GREATER, 0);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		rotatedTessellator.base = Tessellator.instance;
		rotatedTessellator.front = Dir.NZ;
		rotatedTessellator.side = Dir.NY;
		rotatedTessellator.x = -0.5;
		rotatedTessellator.y = -0.2;
		rotatedTessellator.z = -0.5;
		rotatedTessellator.flipped = false;
		
		GL11.glColor3f(1, 1, 1);
		t.startDrawingQuads();
		//t.setBrightness(0x00F000F0);
		t.setColorRGBA(255, 255, 255, 255);
		
		t.setColorOpaque_I(0xFFFFFF);
		IIcon tex = RedLogicMod.gates.getIcon(0, 0);
		t.setNormal(-1.0F, 0.0F, 0.0F);
        render.renderFaceXNeg(RedLogicMod.gates, -0.5, rotatedTessellator.y, -0.5, tex);
        t.setNormal(1.0F, 0.0F, 0.0F);
        render.renderFaceXPos(RedLogicMod.gates, -0.5, rotatedTessellator.y, -0.5, tex);
		t.setNormal(0.0F, 0.0F, -1.0F);
        render.renderFaceZNeg(RedLogicMod.gates, -0.5, rotatedTessellator.y, -0.5, tex);
        t.setNormal(0.0F, 0.0F, 1.0F);
        render.renderFaceZPos(RedLogicMod.gates, -0.5, rotatedTessellator.y, -0.5, tex);
        t.setNormal(0.0F, -1.0F, 0.0F);
        render.renderFaceYNeg(RedLogicMod.gates, -0.5, rotatedTessellator.y, -0.5, tex);
		t.setNormal(0.0F, 1.0F, 0.0F);
		
		for(int k = 0; k < rendering.segmentIcons.length; k++) {
			t.setColorOpaque_I(rendering.segmentCol[k]);
			renderGateSurface(RedLogicMod.gates, rotatedTessellator.side, rotatedTessellator.front, rendering.segmentIcons[k]);
		}
		
		Tessellator.instance.setColorRGBA(255, 255, 255, 255);
		
		rendering.customRender(rotatedTessellator, render);
		
		t.draw();
		
		glDisable(GL_BLEND);
		glEnable(GL_LIGHTING);
		
		GateBlock.renderTypeOverride = -1;
		
		glDisable(GL_LIGHTING);
		Tessellator.instance.startDrawingQuads();
		//Tessellator.instance.setBrightness(0x00F000F0);
		Tessellator.instance.setColorRGBA(255, 255, 255, 255);
		
		for(int k = 0; k < rendering.torchState.length; k++) {
			float tx = rendering.torchX[k]/16f;
			float ty = rendering.torchY[k]/16f;
			boolean on = rendering.torchState[k];
			
			renderTorchAtAngle(render, on ? rendering.torchTexOn : rendering.torchTexOff, tx, ty, 3/16f);
		}
		
		for(int k = 0; k < rendering.pointerX.length; k++) {
			float tx = rendering.pointerX[k]/16f;
			float ty = rendering.pointerY[k]/16f;
			
			renderTorchAtAngle(render, rendering.torchTexOn, tx, ty, 0f);
		}
		
		Tessellator.instance.draw();
		
		render.uvRotateTop = 0;
		glEnable(GL_LIGHTING);
		glDisable(GL_BLEND);
	}
	
	public void renderTorchAtAngle(RenderBlocks render, IIcon texture, double x, double z, float Y_INSET)
    {
        RotatedTessellator var12 = rotatedTessellator;

        if (render.overrideBlockTexture != null)
        {
            texture = render.overrideBlockTexture;
        }

        float var16 = texture.getMinU();
        float var17 = texture.getMaxU();
        float var18 = texture.getMinV();
        float var19 = texture.getInterpolatedV(16 - Y_INSET * 16);
        double var20 = texture.getInterpolatedU(7);
        double var22 = texture.getInterpolatedV(6);
        double var24 = texture.getInterpolatedU(9);
        double var26 = texture.getInterpolatedV(8);
        double var44 = 0.0625D;
        double var46 = 0.625D - Y_INSET;
        
        var12.addVertexWithUV(x - var44, var46, z - var44, var20, var22);
        var12.addVertexWithUV(x - var44, var46, z + var44, var20, var26);
        var12.addVertexWithUV(x + var44, var46, z + var44, var24, var26);
        var12.addVertexWithUV(x + var44, var46, z - var44, var24, var22);
        
        /*
        double var28 = texture.getInterpolatedU(7);
        double var30 = texture.getInterpolatedV(13);
        double var32 = texture.getInterpolatedU(9);
        double var34 = texture.getInterpolatedV(15);
        var12.addVertexWithUV(x + var44, renderBottomY, z - var44, var32, var30);
        var12.addVertexWithUV(x + var44, renderBottomY, z + var44, var32, var34);
        var12.addVertexWithUV(x - var44, renderBottomY, z + var44, var28, var34);
        var12.addVertexWithUV(x - var44, renderBottomY, z - var44, var28, var30);*/
        
        var12.addVertexWithUV(x - var44, 1 - Y_INSET, z - 0.5, (double)var16, (double)var18);
        var12.addVertexWithUV(x - var44, 0, z - 0.5, (double)var16, (double)var19);
        var12.addVertexWithUV(x - var44, 0, z + 0.5, (double)var17, (double)var19);
        var12.addVertexWithUV(x - var44, 1 - Y_INSET, z + 0.5, (double)var17, (double)var18);
        
        var12.addVertexWithUV(x + var44, 1 - Y_INSET, z + 0.5, (double)var16, (double)var18);
        var12.addVertexWithUV(x + var44, 0, z + 0.5, (double)var16, (double)var19);
        var12.addVertexWithUV(x + var44, 0, z - 0.5, (double)var17, (double)var19);
        var12.addVertexWithUV(x + var44, 1 - Y_INSET, z - 0.50, (double)var17, (double)var18);
        
        
        var12.addVertexWithUV(x - 0.5, 1 - Y_INSET, z + var44, (double)var16, (double)var18);
        var12.addVertexWithUV(x - 0.5, 0, z + var44, (double)var16, (double)var19);
        var12.addVertexWithUV(x + 0.5, 0, z + var44, (double)var17, (double)var19);
        var12.addVertexWithUV(x + 0.5, 1 - Y_INSET, z + var44, (double)var17, (double)var18);
        
        var12.addVertexWithUV(x + 0.5, 1 - Y_INSET, z - var44, (double)var16, (double)var18);
        var12.addVertexWithUV(x + 0.5, 0, z - var44, (double)var16, (double)var19);
        var12.addVertexWithUV(x - 0.5, 0, z - var44, (double)var17, (double)var19);
        var12.addVertexWithUV(x - 0.5, 1 - Y_INSET, z - var44, (double)var17, (double)var18);
    }
	
	private void renderGateSurface(Block block, int side, int front, IIcon tex) {
		final double u1 = tex.getMinU();
		final double v1 = tex.getMinV();
		final double u2 = tex.getMaxU();
		final double v2 = v1;
		final double u3 = u2;
		final double v3 = tex.getMaxV();
		final double u4 = u1;
		final double v4 = v3;
		final int x = 0;
		final int z = 0;
		final double y = GateBlock.THICKNESS;
		
		rotatedTessellator.addVertexWithUV(x, y, z, u1, v1);
		rotatedTessellator.addVertexWithUV(x, y, z+1, u4, v4);
		rotatedTessellator.addVertexWithUV(x+1, y, z+1, u3, v3);
		rotatedTessellator.addVertexWithUV(x+1, y, z, u2, v2);
		
	}

	

}
