package mods.immibis.redlogic.chips.ingame;

import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler;
import buildcraft.api.power.PowerHandler.PowerReceiver;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import mods.immibis.core.TileBasicInventory;
import mods.immibis.core.api.MultiInterfaceClass;
import mods.immibis.core.api.MultiInterfaceClass.Interface;


class TilePoweredBaseBase extends TileBasicInventory {
	public TilePoweredBaseBase(int size, String name) {
		super(size, name);
	}
	
	// 1 EU = 2 units
	// 1 MJ = 5 units
	protected int powerStorage;
	protected int maxPowerStorage = 2000;
	
	protected boolean havePowerSystem = false; // if false, no power system is installed, so pretend to have infinite power
	
	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		powerStorage = nbttagcompound.getInteger("power");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		nbttagcompound.setInteger("power", powerStorage);
	}
}

@MultiInterfaceClass(base="mods.immibis.redlogic.chips.ingame.TilePoweredBaseBase",
	interfaces = {
		@Interface(check="buildcraft.api.power.IPowerReceptor", impl="mods.immibis.redlogic.chips.ingame.TilePoweredBase$BC"),
		@Interface(check="ic2.api.energy.tile.IEnergySink", impl="mods.immibis.redlogic.chips.ingame.TilePoweredBase$IC2")
	}
)
public class TilePoweredBase extends TilePoweredBaseBase {
	public TilePoweredBase(int size, String name) {
		super(size, name);
	}
	
	public static class BC extends TilePoweredBaseBase implements IPowerReceptor {
		private PowerHandler provider;
		
		public BC(int size, String name) {
			super(size, name);
			provider = new PowerHandler(this, PowerHandler.Type.MACHINE);
			provider.configure(1, 2000, 1, 1500);
			provider.configurePowerPerdition(0, 0);
			havePowerSystem = true;
		}
		
		@Override
		public void doWork(PowerHandler workProvider) {
			int mjToUse = Math.min((int)workProvider.getEnergyStored(), (maxPowerStorage - powerStorage) / 5); 
			if(mjToUse <= 0)
				return;
			
			powerStorage += (int)(workProvider.useEnergy(0, mjToUse, true) * 5);
		}
		
		@Override
		public PowerReceiver getPowerReceiver(ForgeDirection side) {
			return provider.getPowerReceiver();
		}
		
		@Override
		public World getWorld() {
			return worldObj;
		}
		
		@Override
		public void updateEntity() {
			super.updateEntity();
			
			if(provider != null)
				provider.update();
		}
		
	}
	
	public static class IC2 extends TilePoweredBaseBase implements IEnergySink {
		
		public IC2(int size, String name) {
			super(size, name);
			havePowerSystem = true;
		}
		
		private boolean addedToEnet = false;
		
		@Override
		public void invalidate() {
			super.invalidate();
			if(addedToEnet) {
				MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
				addedToEnet = false;
			}
		}
		
		@Override
		public void onChunkUnload() {
			super.onChunkUnload();
			if(addedToEnet) {
				MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
				addedToEnet = false;
			}
		}
		
		@Override
		public void updateEntity() {
			if(!addedToEnet && !worldObj.isRemote) {
				MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
				addedToEnet = true;
			}
			super.updateEntity();
		}

		@Override
		public boolean acceptsEnergyFrom(TileEntity arg0, ForgeDirection arg1) {
			return true;
		}

		@Override
		public double demandedEnergyUnits() {
			return (maxPowerStorage - powerStorage) / 2;
		}

		@Override
		public double injectEnergyUnits(ForgeDirection arg0, double arg1) {
			if(powerStorage >= maxPowerStorage)
				return arg1;
			int added = (int)arg1 * 2;
			powerStorage += added;
			return arg1 - added*0.5;
		}

		@Override
		public int getMaxSafeInput() {
			return Integer.MAX_VALUE;
		}
		
	}
}
