package mods.immibis.redlogic.wires;

import java.util.HashSet;
import java.util.Set;

import mods.immibis.core.api.util.XYZ;

class RedAlloyUpdateOperation {
	final RedAlloyTile first;
	final Set<XYZ> queuedBlockUpdates = new HashSet<XYZ>();
	
	RedAlloyUpdateOperation(RedAlloyTile first) {
		this.first = first;
	}
}
