package mods.immibis.redlogic.integration.bc;

import buildcraft.api.gates.ActionManager;

public class IntegrationBC {
	public IntegrationBC() {
		
	}
	
	public void load() {
		ActionManager.registerTriggerProvider(new TriggerProvider());
	}
}
