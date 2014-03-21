package mods.immibis.redlogic.integration.bc;

import mods.immibis.redlogic.api.wiring.IBundledWire;
import mods.immibis.redlogic.interaction.RecipeDyeLumarButton;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;
import buildcraft.api.gates.ITileTrigger;
import buildcraft.api.gates.ITrigger;
import buildcraft.api.gates.ITriggerParameter;
import buildcraft.api.gates.TriggerParameter;

public class TriggerBundledCable implements ITileTrigger, ITrigger {
	private boolean isOnTrigger;
	private IIcon icon;
	
	public TriggerBundledCable(boolean on) {
		this.isOnTrigger = on;
	}
	@Override
	public String getUniqueTag() {
		return "RedLogic:bundled." + (isOnTrigger ? "on" : "off");
	}

	@Override
	public IIcon getIcon() {
		return icon;
	}

	@Override
	public void registerIcons(IIconRegister iconRegister) {
		icon = iconRegister.registerIcon("redlogic:buildcraft/trigger_bundled_" + (isOnTrigger ? "on" : "off"));
	}

	@Override
	public boolean hasParameter() {
		return true;
	}

	@Override
	public boolean requiresParameter() {
		return true;
	}

	@Override
	public String getDescription() {
		return "Bundled Cable Signal " + (isOnTrigger ? "Active" : "Inactive");
	}

	@Override
	public ITriggerParameter createParameter() {
		return new TriggerParameter();
	}
	
	public int getColor(ITriggerParameter par) {
		if(par == null) return -1;
		
		ItemStack stack = par.getItemStack();
		if(stack == null || stack.getItem() == null) return -1;
		
		if(stack.getItem() instanceof ItemDye) return 15 - stack.getItemDamage();
		
		int oreID = OreDictionary.getOreID(stack);
		for(int i = 0; i < 16; i++) {
			if(RecipeDyeLumarButton.dyeOreIDs[i] == oreID) {
				return 15 - i;
			}
		}
		return -1;
	}
	
	@Override
	public boolean isTriggerActive(ForgeDirection side, TileEntity tile,
			ITriggerParameter par) {
		int color = getColor(par);
		if(color < 0) return false;
		if(tile instanceof IBundledWire) {
			IBundledWire wire = (IBundledWire)tile;
			for(int face = -1; face < 6; face++) {
				if(wire.wireConnectsInDirection(face, side.getOpposite().ordinal())) {
					byte[] data = wire.getBundledCableStrength(face, side.getOpposite().ordinal());
					if((isOnTrigger && data[color] != 0) || (!isOnTrigger && data[color] == 0))
						return true;
				}
			}
		}
		return false;
	}
}
