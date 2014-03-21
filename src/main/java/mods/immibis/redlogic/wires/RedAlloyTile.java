package mods.immibis.redlogic.wires;


import java.lang.reflect.Field;

import mods.immibis.core.api.util.XYZ;
import mods.immibis.redlogic.CommandDebug;
import mods.immibis.redlogic.RedLogicMod;
import mods.immibis.redlogic.Utils;
import mods.immibis.redlogic.api.wiring.IRedstoneEmitter;
import mods.immibis.redlogic.api.wiring.IRedstoneUpdatable;
import mods.immibis.redlogic.api.wiring.IRedstoneWire;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class RedAlloyTile extends WireTile implements IRedstoneEmitter, IRedstoneWire {
	private short MAX_STRENGTH = 255;
	
	private short strength;
	private short strengthFromNonWireBlocks; // this is synced to the client, not used by the server
	
	protected boolean syncSignalStrength;
	protected boolean connectToBlockBelow;
	
	private boolean isUpdatingStrength, recursiveUpdatePending;
	
	private static boolean dontEmitPower = false;
	
	public RedAlloyTile() {
	}
	
	private int updateStrengthFromBlock(int x, int y, int z, int odir, int testside, int newStrength) {
		int thisStrength = this.getInputPowerStrength(worldObj, x, y, z, odir, testside, true);
		newStrength = Math.max(newStrength, Math.min(thisStrength - 1, MAX_STRENGTH));
		
		if(!worldObj.isRemote) {
			thisStrength = this.getInputPowerStrength(worldObj, x, y, z, odir, testside, false);
			strengthFromNonWireBlocks = (short)Math.max(strengthFromNonWireBlocks, Math.min(thisStrength - 1, MAX_STRENGTH));
		}
		
		return newStrength;
	}
	
	private int getStrengthFromSurroundingBlocks() {
		
		if(!worldObj.isRemote)
			strengthFromNonWireBlocks = 0;
		
		int newStrength = 0;
		for(int side = 0; side < 6; side++) {
			
			if(connectsInDirectionByJacketedWire(side)) {
				ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[side];
				int x = xCoord + dir.offsetX;
				int y = yCoord + dir.offsetY;
				int z = zCoord + dir.offsetZ;
				newStrength = updateStrengthFromBlock(x, y, z, side ^ 1, -1, newStrength);
			}
			
			if(!isWirePresent(side))
				continue;
			
			if(connectToBlockBelow) {
				ForgeDirection wdir = ForgeDirection.VALID_DIRECTIONS[side];
				int x = xCoord + wdir.offsetX;
				int y = yCoord + wdir.offsetY;
				int z = zCoord + wdir.offsetZ;
				
				int thisStrength = Utils.getPowerStrength(worldObj, x, y, z, wdir.ordinal() ^ 1, -1, false);
				newStrength = Math.max(newStrength, Math.min(thisStrength - 1, MAX_STRENGTH));
				
				if(!worldObj.isRemote)
					strengthFromNonWireBlocks = (short)Math.max(strengthFromNonWireBlocks, Math.min(thisStrength - 1, MAX_STRENGTH));
			}
			
			//dontEmitPower = true;
			
			for(int dir = 0; dir < 6; dir++) {
				if(!connectsInDirection(side, dir))
					continue;
				
				ForgeDirection fdir = ForgeDirection.VALID_DIRECTIONS[dir];
				int x = xCoord + fdir.offsetX, y = yCoord + fdir.offsetY, z = zCoord + fdir.offsetZ;
				
				int odir = dir ^ 1;
				int testside = side;
				
				if(connectsInDirectionAroundCorner(side, dir)) {
					fdir = ForgeDirection.VALID_DIRECTIONS[side];
					x += fdir.offsetX;
					y += fdir.offsetY;
					z += fdir.offsetZ;
					
					odir = side ^ 1;
					testside = dir ^ 1;
				}
				
				newStrength = updateStrengthFromBlock(x, y, z, odir, testside, newStrength);
			}
			
			//dontEmitPower = false;
		}
		
		if(worldObj.isRemote)
			newStrength = Math.max(newStrength, strengthFromNonWireBlocks);
		
		return newStrength;
	}
	
	protected int getInputPowerStrength(World worldObj, int x, int y, int z, int dir, int side, boolean countWires) {
		return Utils.getPowerStrength(worldObj, x, y, z, dir, side, countWires);
	}

	private void updateConnectedWireSignal(RedAlloyUpdateOperation update) {
		int notifiedSides = 0;
		
		if(CommandDebug.WIRE_DEBUG_PARTICLES)
			debugEffect_bonemeal();
		
		for(int side = 0; side < 6; side++) {
			if(connectsInDirectionByJacketedWire(side)) {
				if((notifiedSides & (1 << side)) == 0) {
					notifiedSides |= 1 << side;
					
					ForgeDirection fd = ForgeDirection.VALID_DIRECTIONS[side];
					int x = xCoord + fd.offsetX, y = yCoord + fd.offsetY, z = zCoord + fd.offsetZ;
					
					TileEntity t = worldObj.getTileEntity(x, y, z);
					if(t instanceof RedAlloyTile)
						((RedAlloyTile)t).updateSignal(update);
				}
			}
			
			for(int dir = 0; dir < 6; dir++) {
				if(connectsInDirection(side, dir)) {
					ForgeDirection fd = ForgeDirection.VALID_DIRECTIONS[dir];
					int x = xCoord + fd.offsetX, y = yCoord + fd.offsetY, z = zCoord + fd.offsetZ;
					
					if(connectsInDirectionAroundCorner(side, dir)) {
						fd = ForgeDirection.VALID_DIRECTIONS[side];
						x += fd.offsetX;
						y += fd.offsetY;
						z += fd.offsetZ;
					
					} else {
						if((notifiedSides & (1 << dir)) != 0)
							continue;
						notifiedSides |= 1 << dir;
					}
					
					if(worldObj.getBlock(x, y, z).equals(RedLogicMod.wire)) {
						WireTile t = (WireTile)worldObj.getTileEntity(x, y, z);
						if(t instanceof RedAlloyTile) {
							((RedAlloyTile)t).updateSignal(update);
						}
					}
				}
			}
		}
	}
	
	protected void updateSignal(RedAlloyUpdateOperation update) {
		
		if(worldObj.isRemote && !syncSignalStrength)
			return; // doesn't make sense for unsynced wire types
		
		if(isUpdatingStrength) {
			recursiveUpdatePending = true;
			return;
		}
		
		// True if this is the first red alloy tile to update, which received an update from something else,
		// and is now causing a whole bunch of red alloy tiles to change.
		// Only this change will be sent to the client, which will then mirror the processing the server does,
		// to save bandwidth.
		// Note: if syncSignalStrength is false nothing is sent to the client.
		boolean wasFirstServerChange = !worldObj.isRemote && update == null;
		
		if(wasFirstServerChange)
			update = new RedAlloyUpdateOperation(this);
		
		int oldStrengthFromNonWireBlocks = strengthFromNonWireBlocks;
		
		isUpdatingStrength = true;
		
		int newStrength;
		int startStrength = strength;
		
		do {
			recursiveUpdatePending = false;
			
			int prevStrength = strength;
			strength = 0;
			newStrength = getStrengthFromSurroundingBlocks();
			
			//if(prevStrength != newStrength)
			//	System.out.println(xCoord+","+yCoord+","+zCoord+" red alloy update pass; "+prevStrength+" -> "+newStrength);
			
			if(newStrength < prevStrength) {
				// this is a huge optimization - it results in a "pulse" of 0 strength being sent down the wire
				// when turning off. if there is another source of power further down the wire, that one will block
				// the pulse and propagate backwards, turning the wires back on with the correct strength in 2 updates.
				updateConnectedWireSignal(update);
				newStrength = getStrengthFromSurroundingBlocks();
			}
			
			strength = (short)newStrength;
			
			if(strength != prevStrength)
				updateConnectedWireSignal(update);
			
		} while(recursiveUpdatePending);
		
		isUpdatingStrength = false;
		
		//if(startStrength != newStrength)
		//	System.out.println(xCoord+","+yCoord+","+zCoord+" red alloy update; "+startStrength+" -> "+newStrength);
		
		if(strength != startStrength) {
			if(!worldObj.isRemote) {
				notifyExtendedPowerableNeighbours(update);
			}
			
			//System.out.println((worldObj.isRemote ? "client " : wasFirstServerChange ? "was first " : "Not first ") + "change at: " + xCoord+","+yCoord+","+zCoord+", new strength: "+strength+", sfnwb: "+oldStrengthFromNonWireBlocks+" -> "+strengthFromNonWireBlocks);
			
			if(syncSignalStrength && (worldObj.isRemote || wasFirstServerChange || strengthFromNonWireBlocks != oldStrengthFromNonWireBlocks)) {
				if(!worldObj.isRemote && CommandDebug.WIRE_LAG_PARTICLES)
					debugEffect_bonemeal();
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			}
			
		} else if(syncSignalStrength && !worldObj.isRemote && oldStrengthFromNonWireBlocks != strengthFromNonWireBlocks) {
			//System.out.println("SFNWB change at: " + xCoord+","+yCoord+","+zCoord+", new strength: "+strength+", sfnwb: "+oldStrengthFromNonWireBlocks+" -> "+strengthFromNonWireBlocks);
			
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			if(CommandDebug.WIRE_LAG_PARTICLES)
				debugEffect_bonemeal();
		}
		
		if(update.first == this) {
			for(XYZ xyz : update.queuedBlockUpdates) {
				worldObj.notifyBlockOfNeighborChange(xyz.x, xyz.y, xyz.z, RedLogicMod.wire);
			}
		}
	}
	
	@Override
	void onNeighbourBlockChange() {
		super.onNeighbourBlockChange();
		
		updateSignal(null);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		
		strength = tag.getShort("strength");
		strengthFromNonWireBlocks = tag.getShort("strengthNWB");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		
		tag.setShort("strength", strength);
		tag.setShort("strengthNWB", strengthFromNonWireBlocks);
	}

	@Override
	public boolean canUpdate() {
		return false;
	}
	
	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		super.onDataPacket(net, pkt);
		
		if(syncSignalStrength) {
			NBTTagCompound data = pkt.func_148857_g();
			strength = data.getShort("_str");
			strengthFromNonWireBlocks = data.getShort("_snwb");
		}
		
		// The server will only send an update for the first piece of alloy wire that changed strength.
		// It will not send updates for any other pieces that changed as a result.
		// So we have to simulate that on the client as well.
		//notifyExtendedNeighbourWiresOnClient();
		//updateSignal(null);
		updateConnectedWireSignal(new RedAlloyUpdateOperation(this));
	}
	
	@Override
	public S35PacketUpdateTileEntity getDescriptionPacket() {
		NBTTagCompound data = getDescriptionPacketTag();
		if(syncSignalStrength) {
			data.setShort("_str", strength);
			data.setShort("_snwb", strengthFromNonWireBlocks);
		}
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, getDescriptionPacketTag());
	}
	
	// normal direction -> canConnectRedstone direction parameter
	// (-2 indicates this direction cannot be passed)
	private static int[] canConnectRedstoneDirectionMap = {-1, -2, 0, 2, 1, 3};
	
	@Override
	protected boolean connects(int x, int y, int z, int wireSide, int direction) {
		Block b = worldObj.getBlock(x, y, z);
		if(b == null)
			return false;
		if(b.canProvidePower())
			return true;
		if(direction >= 0 && direction < 6 && canConnectRedstoneDirectionMap[direction] != -2 && b.canConnectRedstone(worldObj, x, y, z, canConnectRedstoneDirectionMap[direction]))
			return true;
		return false;
	}

	/**
	 * Returns signal strength from 0 to 255.
	 */
	public short getRedstoneSignalStrength() {
		return dontEmitPower ? 0 : strength;
	}
	
	@Override
	public short getEmittedSignalStrength(int side, int dir) { // IRedstoneEmittingTile
		return connectsInDirection(side, dir) ? getRedstoneSignalStrength() : 0;
	}
	
	
	
	private static Field wiresProvidePower = BlockRedstoneWire.class.getDeclaredFields()[0];
	static {
		try {
			wiresProvidePower.setAccessible(true);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		if(wiresProvidePower.getType() != boolean.class)
			throw new AssertionError("field order changed; fix me");
	}

	public boolean canProvideStrongPowerInDirection(int dir) {
		try {
			return connectToBlockBelow && isWirePresent(dir) && wiresProvidePower.getBoolean(Blocks.redstone_wire);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void notifyExtendedPowerableNeighbours(RedAlloyUpdateOperation update) {
		boolean triggeredBlockUpdates = false;
		
		for(int k = 0; k < 6; k++) {
			ForgeDirection fd = ForgeDirection.VALID_DIRECTIONS[k];
			int x = xCoord + fd.offsetX;
			int y = yCoord + fd.offsetY;
			int z = zCoord + fd.offsetZ;
			
			// TODO this code isn't very understandable - if poweringAtAll is false why do we check for IRedstoneUpdatable?
			// (The reason is that insulated wires don't power weakly or strongly but IRedstoneUpdatable tiles
			// can still choose to connect to them)
			
			boolean poweringStrongly = canProvideStrongPowerInDirection(k);
			boolean poweringAtAll = poweringStrongly || canProvideWeakPowerInDirection(k);
			
			if(poweringAtAll) {
				XYZ here = new XYZ(x, y, z);
				update.queuedBlockUpdates.add(here);
				
				if(poweringStrongly) {
					for(int j = 0; j < 6; j++)
						update.queuedBlockUpdates.add(here.step(j));
				}
				
				triggeredBlockUpdates = true;
				
			} else {
				Block block = worldObj.getBlock(x, y, z);
				if(block != null && block.hasTileEntity(worldObj.getBlockMetadata(x, y, z))) {
					TileEntity te = worldObj.getTileEntity(x, y, z);
					if(te instanceof IRedstoneUpdatable)
						((IRedstoneUpdatable)te).onRedstoneInputChanged();
				}
			}
		}
		
		/*if(canProvideWeakPowerInDirection(Dir.NX)) {triggeredBlockUpdates = true; worldObj.notifyBlockOfNeighborChange(xCoord+1, yCoord, zCoord, RedLogicMod.wire.blockID);}
		if(canProvideWeakPowerInDirection(Dir.PX)) {triggeredBlockUpdates = true; worldObj.notifyBlockOfNeighborChange(xCoord-1, yCoord, zCoord, RedLogicMod.wire.blockID);}
		if(canProvideWeakPowerInDirection(Dir.NY)) {triggeredBlockUpdates = true; worldObj.notifyBlockOfNeighborChange(xCoord, yCoord-1, zCoord, RedLogicMod.wire.blockID);}
		if(canProvideWeakPowerInDirection(Dir.PY)) {triggeredBlockUpdates = true; worldObj.notifyBlockOfNeighborChange(xCoord, yCoord+1, zCoord, RedLogicMod.wire.blockID);}
		if(canProvideWeakPowerInDirection(Dir.NZ)) {triggeredBlockUpdates = true; worldObj.notifyBlockOfNeighborChange(xCoord, yCoord, zCoord-1, RedLogicMod.wire.blockID);}
		if(canProvideWeakPowerInDirection(Dir.PZ)) {triggeredBlockUpdates = true; worldObj.notifyBlockOfNeighborChange(xCoord, yCoord, zCoord+1, RedLogicMod.wire.blockID);}*/
		
		/*if(canProvideStrongPowerInDirection(Dir.NX)) {triggeredBlockUpdates = true; worldObj.notifyBlocksOfNeighborChange(xCoord+1, yCoord, zCoord, RedLogicMod.wire.blockID);}
		if(canProvideStrongPowerInDirection(Dir.PX)) {triggeredBlockUpdates = true; worldObj.notifyBlocksOfNeighborChange(xCoord-1, yCoord, zCoord, RedLogicMod.wire.blockID);}
		if(canProvideStrongPowerInDirection(Dir.NY)) {triggeredBlockUpdates = true; worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord-1, zCoord, RedLogicMod.wire.blockID);}
		if(canProvideStrongPowerInDirection(Dir.PY)) {triggeredBlockUpdates = true; worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord+1, zCoord, RedLogicMod.wire.blockID);}
		if(canProvideStrongPowerInDirection(Dir.NZ)) {triggeredBlockUpdates = true; worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord-1, RedLogicMod.wire.blockID);}
		if(canProvideStrongPowerInDirection(Dir.PZ)) {triggeredBlockUpdates = true; worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord+1, RedLogicMod.wire.blockID);}*/
		
		if(triggeredBlockUpdates && CommandDebug.WIRE_LAG_PARTICLES)
			debugEffect_fireburst();
	}

	public boolean canProvideWeakPowerInDirection(int dir) {
		try {
			//if(!isWirePresent(dir) && !connectsInDirection(dir))
			//	return false;
			
			// There must be a wire on any side except the opposite side, or a freestanding wire
			if((getSideMask() & ~(1 << (dir ^ 1))) == 0 && !hasJacketedWire())
				return false;
			
			if(!wiresProvidePower.getBoolean(Blocks.redstone_wire))
				return false;
			
			return true;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the vanilla redstone strength from 0 to 15.
	 */
	public byte getVanillaRedstoneStrength() {
		return (byte)(getRedstoneSignalStrength() / 17);
	}
	
	@Override
	protected boolean debug(EntityPlayer ply) {
		// TODO right function?
		ply.addChatMessage(new ChatComponentText((worldObj.isRemote?"Client":"Server")+" signal strength: " + strength + ", nwb: " + strengthFromNonWireBlocks));
		
		super.debug(ply);
		return true;
	}
	
	@Override
	protected boolean canConnectToWire(WireTile wire) {
		return wire instanceof RedAlloyTile; // overridden by InsulatedRedAlloyTile
	}

	@Override
	public void onRedstoneInputChanged() {
		updateSignal(null);
	}
}
