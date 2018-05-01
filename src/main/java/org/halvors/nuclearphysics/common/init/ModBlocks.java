package org.halvors.nuclearphysics.common.init;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.IForgeRegistry;
import net.minecraftforge.oredict.OreDictionary;
import org.halvors.nuclearphysics.common.Reference;
import org.halvors.nuclearphysics.common.block.BlockBase;
import org.halvors.nuclearphysics.common.block.debug.BlockCreativeBuilder;
import org.halvors.nuclearphysics.common.block.machine.BlockMachine;
import org.halvors.nuclearphysics.common.block.particle.BlockFulminationGenerator;
import org.halvors.nuclearphysics.common.block.reactor.*;
import org.halvors.nuclearphysics.common.block.reactor.fission.BlockControlRod;
import org.halvors.nuclearphysics.common.block.reactor.fission.BlockRadioactive;
import org.halvors.nuclearphysics.common.block.reactor.fusion.BlockElectromagnet;
import org.halvors.nuclearphysics.common.block.states.BlockStateMachine.EnumMachine;
import org.halvors.nuclearphysics.common.block.states.BlockStateRadioactive.EnumRadioactive;
import org.halvors.nuclearphysics.common.item.block.ItemBlockMetadata;
import org.halvors.nuclearphysics.common.item.block.ItemBlockTooltip;
import org.halvors.nuclearphysics.common.item.block.reactor.ItemBlockThermometer;
import org.halvors.nuclearphysics.common.tile.particle.TileElectromagnet;
import org.halvors.nuclearphysics.common.tile.particle.TileFulminationGenerator;
import org.halvors.nuclearphysics.common.tile.reactor.*;
import org.halvors.nuclearphysics.common.tile.reactor.fusion.TilePlasma;

import java.util.HashSet;
import java.util.Set;

public class ModBlocks {
    public static final Set<ItemBlock> itemBlocks = new HashSet<>();

    public static final Block blockControlRod = new BlockControlRod();
    public static final Block blockElectricTurbine = new BlockElectricTurbine();
    public static final Block blockElectromagnet = new BlockElectromagnet();
    public static final Block blockFulmination = new BlockFulminationGenerator();
    public static final Block blockGasFunnel = new BlockGasFunnel();
    public static final Block blockMachine = new BlockMachine();
    public static final Block blockSiren = new BlockSiren();
    public static final Block blockThermometer = new BlockThermometer();
    public static final Block blockRadioactive = new BlockRadioactive();
    public static final Block blockReactorCell = new BlockReactorCell();
    public static final Block blockCreativeBuilder = new BlockCreativeBuilder();

    @EventBusSubscriber
    public static class RegistrationHandler {
        /**
         * Register this mod's {@link Block}s.
         *
         * @param event The event
         */
        @SubscribeEvent
        public static void registerBlocks(final Register<Block> event) {
            final IForgeRegistry<Block> registry = event.getRegistry();

            final Block[] registerBlocks = {
                    blockControlRod,
                    blockElectricTurbine,
                    blockElectromagnet,
                    blockFulmination,
                    blockGasFunnel,
                    blockMachine,
                    blockSiren,
                    blockThermometer,
                    blockRadioactive,
                    blockReactorCell,
                    blockCreativeBuilder
            };

            registry.registerAll(registerBlocks);

            registerTileEntities();
        }

        /**
         * Register this mod's {@link ItemBlock}s.
         *
         * @param event The event
         */
        @SubscribeEvent
        public static void registerItemBlocks(final Register<Item> event) {
            final ItemBlock[] registerItems = {
                    new ItemBlockTooltip(blockControlRod),
                    new ItemBlockTooltip(blockElectricTurbine),
                    new ItemBlockMetadata(blockElectromagnet),
                    new ItemBlockTooltip(blockFulmination),
                    new ItemBlockTooltip(blockGasFunnel),
                    new ItemBlockMetadata(blockMachine),
                    new ItemBlockTooltip(blockSiren),
                    new ItemBlockThermometer(blockThermometer),
                    new ItemBlockMetadata(blockRadioactive),
                    new ItemBlockTooltip(blockReactorCell),
                    new ItemBlockTooltip(blockCreativeBuilder)
            };

            final IForgeRegistry<Item> registry = event.getRegistry();

            for (final ItemBlock item : registerItems) {
                final BlockBase block = (BlockBase) item.getBlock();
                //final ResourceLocation registryName = Preconditions.checkNotNull(block.getRegistryName(), "Block %s has null registry name", block);
                //registry.register(item.setRegistryName(registryName));
                registry.register(item);

                block.registerItemModel(item);
                block.registerBlockModel();

                itemBlocks.add(item);
            }

            OreDictionary.registerOre("blockRadioactiveGrass", new ItemStack(blockRadioactive, 1, EnumRadioactive.DIRT.ordinal()));
            OreDictionary.registerOre("blockRadioactiveGrass", new ItemStack(blockRadioactive, 1, EnumRadioactive.GRASS.ordinal()));
            OreDictionary.registerOre("oreUranium", new ItemStack(blockRadioactive, 1, EnumRadioactive.URANIUM_ORE.ordinal()));
        }
    }

    private static void registerTileEntities() {
        for (EnumMachine type : EnumMachine.values()) {
            registerTile(type.getTileClass());
        }

        registerTile(TileElectricTurbine.class);
        registerTile(TileElectromagnet.class);
        registerTile(TileFulminationGenerator.class);
        registerTile(TileGasFunnel.class);
        registerTile(TileSiren.class);
        registerTile(TileThermometer.class);
        registerTile(TilePlasma.class);
        registerTile(TileReactorCell.class);
    }

    private static void registerTile(final Class<? extends TileEntity> tileClass) {
        final String name = tileClass.getSimpleName().replaceAll("(.)(\\p{Lu})", "$1_$2").toLowerCase();

        GameRegistry.registerTileEntity(tileClass, Reference.PREFIX + name);
    }
}
