package mods.immibis.redlogic;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

public class CommandDebug extends CommandBase {
	/**
	 * If true:
	 * Fire particles indicate block updates.
	 * Bonemeal particles indicate packets sent.
	 */
	public static boolean WIRE_LAG_PARTICLES;
	
	/**
	 * If true, bonemeal particles indicate wire strength updates.
	 */
	public static boolean WIRE_DEBUG_PARTICLES;
	
	/**
	 * If true, right-click a wire for signal strength.
	 * Will not work properly in SMP.
	 */
	public static boolean WIRE_READING;
	
	/**
	 * If true, chip scanning is much faster.
	 */
	public static boolean FAST_SCAN;

	@Override
	public String getCommandName() {
		return "rldebug";
	}
	
	@Override
	public String getCommandUsage(ICommandSender par1iCommandSender) {
		return "/"+getCommandName()+" {wire-lag-particles|wire-debug-particles|wire-reading|fast-scan} {on|off}";
	}

	@Override
	public void processCommand(ICommandSender icommandsender, String[] astring) {
		if(astring.length != 2)
			throw new WrongUsageException(getCommandUsage(icommandsender));
		
		String thing = astring[0];
		boolean on;
		
		if(astring[1].equals("on"))
			on = true;
		else if(astring[1].equals("off"))
			on = false;
		else
			throw new WrongUsageException(getCommandUsage(icommandsender));
		
		if(thing.equals("wire-lag-particles"))
			WIRE_LAG_PARTICLES = on;
		else if(thing.equals("wire-debug-particles"))
			WIRE_DEBUG_PARTICLES = on;
		else if(thing.equals("wire-reading"))
			WIRE_READING = on;
		else if(thing.equals("fast-scan"))
			FAST_SCAN = on;
		else
			throw new WrongUsageException(getCommandUsage(icommandsender));
		
		// TODO right function?
		MinecraftServer.getServer().getConfigurationManager().sendChatMsg(new ChatComponentText("RedLogic debug feature '" + thing + "' is now " + astring[1]));
	}
	
	@Override
	public boolean canCommandSenderUseCommand(ICommandSender par1iCommandSender) {
		// TODO Auto-generated method stub
		return super.canCommandSenderUseCommand(par1iCommandSender);
	}
	
    public int compareTo(Object par1Obj)
    {
        return ((ICommand)par1Obj).getCommandName().compareTo(this.getCommandName());
    }
}
