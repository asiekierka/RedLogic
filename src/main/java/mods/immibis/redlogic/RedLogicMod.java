package mods.immibis.redlogic;

import mods.immibis.cobaltite.AssignedBlock;
import mods.immibis.cobaltite.AssignedItem;
import mods.immibis.cobaltite.CobaltiteMod;
import mods.immibis.cobaltite.Configurable;
import mods.immibis.cobaltite.ModBase;
import mods.immibis.cobaltite.TileGUI;
import mods.immibis.cobaltite.CobaltiteMod.RegisteredTile;
import mods.immibis.core.api.FMLModInfo;
import mods.immibis.redlogic.array.ArrayCellBlock;
import mods.immibis.redlogic.array.ArrayCellItem;
import mods.immibis.redlogic.array.ArrayCellTile;
import mods.immibis.redlogic.chips.builtin.RegisterScannables;
import mods.immibis.redlogic.chips.generated.CCOFactory;
import mods.immibis.redlogic.chips.ingame.*;
import mods.immibis.redlogic.gates.*;
import mods.immibis.redlogic.interaction.BlockLumarButton;
import mods.immibis.redlogic.interaction.BlockLumarButtonItem;
import mods.immibis.redlogic.interaction.TileLumarButton;
import mods.immibis.redlogic.lamps.BlockLampCube;
import mods.immibis.redlogic.lamps.BlockLampNonCube;
import mods.immibis.redlogic.lamps.BlockLampNonCubeItem;
import mods.immibis.redlogic.lamps.ItemLampCube;
import mods.immibis.redlogic.lamps.TileLampNonCube;
import mods.immibis.redlogic.recipes.RecipesOriginal;
import mods.immibis.redlogic.wires.BundledTile;
import mods.immibis.redlogic.wires.InsulatedRedAlloyTile;
import mods.immibis.redlogic.wires.PlainRedAlloyTile;
import mods.immibis.redlogic.wires.WireBlock;
import mods.immibis.redlogic.wires.WireItem;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLogic;
import net.minecraft.world.storage.ISaveHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;

@Mod(modid="RedLogic", name="RedLogic", version="58.0.1", dependencies="required-after:ImmibisCore")
@CobaltiteMod(
		tiles = {
				@RegisteredTile(id="immibis.redlogic.gate", tile=GateTile.class, render="mods.immibis.redlogic.gates.GateDynamicRenderer"),
				@RegisteredTile(id="immibis.redlogic.wire.redalloy", tile=PlainRedAlloyTile.class),
				@RegisteredTile(id="immibis.redlogic.wire.insulated", tile=InsulatedRedAlloyTile.class),
				@RegisteredTile(id="immibis.redlogic.wire.bundled", tile=BundledTile.class),
				@RegisteredTile(id="immibis.redlogic.invalid", tile=InvalidTile.class),
				@RegisteredTile(id="immibis.redlogic.chipscanner", tile=TileChipScanner.class, render="mods.immibis.redlogic.chips.ingame.RenderTileChipScanner"),
				@RegisteredTile(id="immibis.redlogic.circuit", tile=TileCustomCircuit.class),
				@RegisteredTile(id="immibis.redlogic.chipiomarker", tile=TileIOMarker.class),
				@RegisteredTile(id="immibis.redlogic.chipcompiler", tile=TileChipCompiler.class, render="mods.immibis.redlogic.chips.ingame.RenderTileChipCompiler"),
				@RegisteredTile(id="immibis.redlogic.chipfabricator", tile=TileChipFabricator.class),
				@RegisteredTile(id="immibis.redlogic.lamp", tile=TileLampNonCube.class),
				@RegisteredTile(id="immibis.redlogic.button", tile=TileLumarButton.class),
				@RegisteredTile(id="immibis.redlogic.array", tile=ArrayCellTile.class),
		}
	)
@FMLModInfo(authors = "immibis", description = "Replacement for RP2 Wiring, Logic and Control",
	url="http://www.minecraftforum.net/topic/1852277-152-redlogic-wip-replacement-for-rp2-wiringlogiccontrollighting/")
public class RedLogicMod extends ModBase {

	@TileGUI(container=TimerContainer.class, gui=TimerGui.class)
	public static final int GUI_TIMER = 0;

	@TileGUI(container=CounterContainer.class, gui=CounterGui.class)
	public static final int GUI_COUNTER = 1;
	
	@TileGUI(container=ContainerChipFabricator.class, gui=GuiChipFabricator.class)
	public static final int GUI_CHIP_FABRICATOR = 2;

	public static final String CHANNEL = "RedLogic";

	@Instance("RedLogic")
	public static RedLogicMod instance;

	@AssignedBlock(id="gates", item=GateItem.class)
	public static GateBlock gates;

	@AssignedBlock(id="wire", item=WireItem.class)
	public static WireBlock wire;
	
	@AssignedBlock(id="lampCubeOn", item=ItemLampCube.class)
	public static BlockLampCube.On lampCubeOn;
	@AssignedBlock(id="lampCubeOff", item=ItemLampCube.class)
	public static BlockLampCube.Off lampCubeOff;
	@AssignedBlock(id="lampCubeDecorative", item=ItemLampCube.class)
	public static BlockLampCube.Decorative lampCubeDecorative;
	@AssignedBlock(id="lampCubeIndicatorOn", item=ItemLampCube.class)
	public static BlockLampCube.IndicatorOn lampCubeIndicatorOn;
	@AssignedBlock(id="lampCubeIndicatorOff", item=ItemLampCube.class)
	public static BlockLampCube.IndicatorOff lampCubeIndicatorOff;
	
	@AssignedBlock(id="button", item=BlockLumarButtonItem.class)
	public static BlockLumarButton lumarButton;
	
	@AssignedBlock(id="lampNonCube", item=BlockLampNonCubeItem.class)
	public static BlockLampNonCube lampNonCube;
	
	@AssignedBlock(id="plainBlock", item=RLNormalBlockItem.class)
	public static RLNormalBlock plainBlock;
	
	@AssignedBlock(id="machineBlock", item=RLMachineBlockItem.class)
	public static RLMachineBlock machineBlock;
	
	@AssignedBlock(id="customCircuitBlock", item=ItemCustomCircuit.class)
	public static BlockCustomCircuit customCircuitBlock;
	
	@AssignedBlock(id="arrayCells", item=ArrayCellItem.class)
	public static ArrayCellBlock arrayCells;

	@AssignedItem(id="screwdriver")
	public static ItemScrewdriver screwdriver;
	
	@AssignedItem(id="compiledCircuit")
	public static ItemPhotomask photomaskItem;
	
	@AssignedItem(id="schematic")
	public static ItemSchematic schematicItem;
	
	@AssignedItem(id="chip")
	public static ItemChip chipItem;
	
	@Configurable("minTimerTicks")
	public static int minTimerTicks = 4;
	
	@Configurable("defaultTimerTicks")
	public static int defaultTimerTicks = 20;

	public static Material circuitMaterial = new MaterialLogic(Material.circuits.getMaterialMapColor()) {
		@Override
		public boolean blocksMovement() {
			// required for water to not wash away things, but has other side effects...
			return true;
		}
	};

	@EventHandler public void init(FMLInitializationEvent evt) {super._init(evt);}
	@EventHandler public void preinit(FMLPreInitializationEvent evt) {super._preinit(evt);}

	@Override
	protected void initBlocksAndItems() {
		RegisterScannables.register();
	}
	
	@Override
	protected void addRecipes() throws Exception {
		RecipesOriginal.addRecipes();
	}
	
	@EventHandler
	public void onServerStarting(FMLServerStartingEvent evt) {
		evt.registerServerCommand(new CommandDebug());
		
		ISaveHandler saveHandler = evt.getServer().worldServerForDimension(0).getSaveHandler();
		CCOFactory.instance = new CCOFactory(saveHandler.getMapFileFromName("redlogic-compiled-circuit-cache"));
	}
	
	@EventHandler
	public void onServerStopped(FMLServerStoppedEvent evt) {
		CCOFactory.instance = null;
	}
}
