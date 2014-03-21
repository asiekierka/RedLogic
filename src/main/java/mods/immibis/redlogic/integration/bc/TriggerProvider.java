package mods.immibis.redlogic.integration.bc;

import java.util.LinkedList;

import mods.immibis.redlogic.api.wiring.IBundledWire;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import buildcraft.api.gates.ActionManager;
import buildcraft.api.gates.ITrigger;
import buildcraft.api.gates.ITriggerProvider;
import buildcraft.api.transport.IPipeTile;

public class TriggerProvider implements ITriggerProvider {
	private TriggerBundledCable bundledOn = new TriggerBundledCable(true);
	private TriggerBundledCable bundledOff = new TriggerBundledCable(false);

	public TriggerProvider() {
		ActionManager.registerTrigger(bundledOff);
		ActionManager.registerTrigger(bundledOn);
	}
	
	@Override
	public LinkedList<ITrigger> getPipeTriggers(IPipeTile pipe) {
		return null;
	}

	@Override
	public LinkedList<ITrigger> getNeighborTriggers(Block block, TileEntity tile) {
		LinkedList<ITrigger> triggers = new LinkedList<ITrigger>();
		if(tile instanceof IBundledWire) {
			triggers.add(bundledOn);
			triggers.add(bundledOff);
		}
		return triggers;
	}
}
