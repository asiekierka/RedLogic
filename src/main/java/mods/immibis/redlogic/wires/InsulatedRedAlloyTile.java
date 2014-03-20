package mods.immibis.redlogic.wires;

import mods.immibis.redlogic.Utils;
import mods.immibis.redlogic.api.wiring.IBundledEmitter;
import mods.immibis.redlogic.api.wiring.IBundledUpdatable;
import mods.immibis.redlogic.api.wiring.IInsulatedRedstoneWire;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class InsulatedRedAlloyTile extends RedAlloyTile implements IBundledUpdatable, IInsulatedRedstoneWire {
	@Override
	protected boolean canConnectToWire(WireTile wire) {
		if(wire.getType() == EnumWireType.RED_ALLOY || wire instanceof BundledTile)
			return true;
		return wire.getType() == getType();
	}
	
	@Override
	public boolean canProvideWeakPowerInDirection(int dir) {
		//return connectsInDirection(dir) && super.canProvideWeakPowerInDirection(dir);
		return false;
	}
	
	@Override
	public boolean canProvideStrongPowerInDirection(int dir) {
		return false;
	}
	
	@Override
	protected int getInputPowerStrength(World worldObj, int x, int y, int z, int dir, int side, boolean countWires) {
		int rv = Utils.getPowerStrength(worldObj, x, y, z, dir, side, countWires);
		
		if(rv > 0) return rv; // no block emits bundled and normal power from the same side
		
		TileEntity te = worldObj.getTileEntity(x, y, z);
		if(te instanceof IBundledEmitter) {
			int colour = getInsulatedWireColour();
			byte[] bcStrengthArray = ((IBundledEmitter)te).getBundledCableStrength(side, dir);
			if(bcStrengthArray != null) {
				int bcStrength = bcStrengthArray[colour] & 0xFF;
				rv = Math.max(rv, bcStrength);
			}
		}
		
		return rv;
	}

	@Override
	public void onBundledInputChanged() {
		updateSignal(null);
	}

	@Override
	public int getInsulatedWireColour() {
		if(getType() == null)
			return 0;
		return getType().ordinal() - EnumWireType.INSULATED_0.ordinal();
	}
}
