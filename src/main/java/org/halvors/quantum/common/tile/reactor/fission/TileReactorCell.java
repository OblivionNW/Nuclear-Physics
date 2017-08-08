package org.halvors.quantum.common.tile.reactor.fission;

import io.netty.buffer.ByteBuf;
import javafx.geometry.Pos;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.halvors.quantum.api.item.IReactorComponent;
import org.halvors.quantum.api.tile.IReactor;
import org.halvors.quantum.common.Quantum;
import org.halvors.quantum.common.block.reactor.fission.BlockReactorCell.EnumReactorCell;
import org.halvors.quantum.common.block.states.BlockStateReactorCell;
import org.halvors.quantum.common.effect.explosion.ReactorExplosion;
import org.halvors.quantum.common.effect.poison.PoisonRadiation;
import org.halvors.quantum.common.event.PlasmaEvent.PlasmaSpawnEvent;
import org.halvors.quantum.common.fluid.tank.FluidTankQuantum;
import org.halvors.quantum.common.grid.thermal.ThermalGrid;
import org.halvors.quantum.common.grid.thermal.ThermalPhysics;
import org.halvors.quantum.common.init.QuantumBlocks;
import org.halvors.quantum.common.init.QuantumFluids;
import org.halvors.quantum.common.init.QuantumSoundEvents;
import org.halvors.quantum.common.multiblock.IMultiBlockStructure;
import org.halvors.quantum.common.multiblock.MultiBlockHandler;
import org.halvors.quantum.common.network.packet.PacketTileEntity;
import org.halvors.quantum.common.tile.TileRotatable;
import org.halvors.quantum.common.tile.reactor.fusion.TilePlasma;
import org.halvors.quantum.common.utility.position.Position;
import org.halvors.quantum.common.utility.transform.vector.Vector3;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TileReactorCell extends TileRotatable implements ITickable, IMultiBlockStructure<TileReactorCell>, IReactor {
    public static final int radius = 2;
    public static final int meltingPoint = 2000;
    private final int specificHeatCapacity = 1000;
    private final float mass = ThermalPhysics.getMass(1000, 7);

    private float temperature = ThermalPhysics.roomTemperature; // Synced
    private float previousTemperature = temperature;

    private boolean shouldUpdate = false;

    private long internalEnergy = 0;
    private long previousInternalEnergy = 0;
    private int meltdownCounter = 0;
    private int meltdownCounterMaximum = 1000;

    private MultiBlockHandler<TileReactorCell> multiBlock;

    private final IItemHandlerModifiable inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            markDirty();
        }

        private boolean isItemValidForSlot(int slot, ItemStack itemStack) {
            MultiBlockHandler<TileReactorCell> multiBlock = getMultiBlock();

            return multiBlock.isPrimary() && multiBlock.get().getInventory().getStackInSlot(0) == null && itemStack.getItem() instanceof IReactorComponent;

        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValidForSlot(slot, stack)) {
                return stack;
            }

            return super.insertItem(slot, stack, simulate);
        }
    };

    private final FluidTankQuantum tank = new FluidTankQuantum(Fluid.BUCKET_VOLUME * 15) {
        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (resource.isFluidEqual(QuantumFluids.plasmaStack)) {
                return super.fill(resource, doFill);
            }

            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource.isFluidEqual(QuantumFluids.fluidStackToxicWaste)) {
                return super.drain(resource, doDrain);
            }

            return null;
        }
    };

    public TileReactorCell() {

    }

    public TileReactorCell(String name) {
        super(name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        if (getMultiBlock().isPrimary() && getMultiBlock().isConstructed()) {
            return INFINITE_EXTENT_AABB;
        }

        return super.getRenderBoundingBox();
    }

    @Override
    public void update() {
        if (world.getTotalWorldTime() == 0) {
            updatePositionStatus();
        }

        // Move fuel rod down into the primary cell block if possible.
        if (!getMultiBlock().isPrimary()) {
            if (inventory.getStackInSlot(0) != null) {
                if (getMultiBlock().get().inventory.getStackInSlot(0) == null) {
                    getMultiBlock().get().inventory.insertItem(0, inventory.getStackInSlot(0).copy(), false);
                    inventory.setStackInSlot(0, null);
                }
            }

            // Move fluid down into blocks below.
            if (tank.getFluidAmount() > 0) {
                getMultiBlock().get().tank.fillInternal(tank.drainInternal(tank.getCapacity(), true), true);
            }
        }

        if (getMultiBlock().isPrimary() && tank.getFluid() != null && tank.getFluid().getFluid() == QuantumFluids.plasma) {
            // Spawn plasma.
            FluidStack drain = tank.drainInternal(Fluid.BUCKET_VOLUME, false);

            if (drain != null && drain.amount >= Fluid.BUCKET_VOLUME) {
                EnumFacing spawnDir = EnumFacing.getFront(world.rand.nextInt(3) + 2);
                BlockPos spawnPos = pos.offset(spawnDir, 2).up(Math.max(world.rand.nextInt(getHeight()) - 1, 0));

                if (world.isAirBlock(spawnPos)) {
                    MinecraftForge.EVENT_BUS.post(new PlasmaSpawnEvent(world, spawnPos, TilePlasma.plasmaMaxTemperature));
                    tank.drainInternal(Fluid.BUCKET_VOLUME, true);
                }
            }
        } else {
            previousInternalEnergy = internalEnergy;

            // Handle cell rod interactions.
            ItemStack fuelRod = getMultiBlock().get().getInventory().getStackInSlot(0);

            if (fuelRod != null) {
                if (fuelRod.getItem() instanceof IReactorComponent) {
                    // Activate rods.
                    IReactorComponent reactorComponent = (IReactorComponent) fuelRod.getItem();
                    reactorComponent.onReact(fuelRod, this);

                    if (fuelRod.getMetadata() >= fuelRod.getMaxDamage()) {
                        getMultiBlock().get().getInventory().setStackInSlot(0, null);
                    }

                    // Emit radiation.
                    if (world.getTotalWorldTime() % 20 == 0) {
                        if (world.rand.nextFloat() > 0.65) {
                            List<EntityLiving> entities = world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(pos.getX() - radius * 2, pos.getY() - radius * 2, pos.getZ() - radius * 2, pos.getX() + radius * 2, pos.getY() + radius * 2, pos.getZ() + radius * 2));

                            for (EntityLiving entity : entities) {
                                PoisonRadiation.getInstance().poisonEntity(pos, entity);
                            }
                        }
                    }
                }
            }

            // Update the temperature from the thermal grid.
            temperature = ThermalGrid.getTemperature(world, pos);

            // Only a small percentage of the internal energy is used for temperature.
            if ((internalEnergy - previousInternalEnergy) > 0) {
                float deltaT = ThermalPhysics.getTemperatureForEnergy(mass, specificHeatCapacity, (long) ((internalEnergy - previousInternalEnergy) * 0.15));

                // Check control rods.
                for (EnumFacing side : EnumFacing.HORIZONTALS) {
                    BlockPos checkPos = pos.offset(side);

                    if (world.getBlockState(checkPos).getBlock() == QuantumBlocks.blockControlRod) {
                        deltaT /= 1.1;
                    }
                }

                // Add heat to surrounding blocks in the thermal grid.
                ThermalGrid.addTemperature(world, pos, deltaT);

                // Sound of lava flowing randomly plays when above temperature to boil water.
                if (world.rand.nextInt(80) == 0 && getTemperature() >= ThermalPhysics.waterBoilTemperature) {
                    // TODO: Only do this is there is a water block nearby.
                    world.playSound(null, pos, SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.BLOCKS, 0.5F, 2.1F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.85F);
                }

                // Sounds of lava popping randomly plays when above temperature to boil water.
                if (world.rand.nextInt(40) == 0 && getTemperature() >= ThermalPhysics.waterBoilTemperature) {
                    // TODO: Only do this is there is a water block nearby.
                    world.playSound(null, pos, SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
                }

                // Reactor cell plays random idle noises while operating and above temperature to boil water.
                if (world.getWorldTime() % 100 == 0 && getTemperature() >= ThermalPhysics.waterBoilTemperature) {
                    float percentage = Math.min(getTemperature() / meltingPoint, 1.0F);

                    world.playSound(null, pos, QuantumSoundEvents.REACTOR_CELL, SoundCategory.BLOCKS, percentage, 1);
                }

                if (previousTemperature != temperature && !shouldUpdate) {
                    shouldUpdate = true;
                    previousTemperature = temperature;
                }

                // If temperature is over the melting point of the reactor, either increase counter or melt down.
                if (previousTemperature >= meltingPoint) {
                    if (meltdownCounter < meltdownCounterMaximum) {
                        shouldUpdate = true;
                        meltdownCounter++;
                    } else if (meltdownCounter >= meltdownCounterMaximum) {
                        meltdownCounter = 0;
                        meltDown();

                        return;
                    }
                }

                // If reactor temperature is below meltingPoint and meltdownCounter is over 0, decrease it.
                if (previousTemperature < meltingPoint && meltdownCounter > 0) {
                    meltdownCounter--;
                }
            }

            internalEnergy = 0;

            if (isOverToxic()) {
                // Randomly leak toxic waste when it is too toxic.
                BlockPos leakPos = pos.add(world.rand.nextInt(20) - 10, world.rand.nextInt(20) - 10, world.rand.nextInt(20) - 10);
                Block block = world.getBlockState(leakPos).getBlock();

                if (block == Blocks.GRASS) {
                    world.setBlockState(leakPos, QuantumBlocks.blockRadioactiveGrass.getDefaultState());
                    tank.drainInternal(Fluid.BUCKET_VOLUME, true);
                } else if (world.isAirBlock(leakPos) || block.isReplaceable(world, leakPos)) {
                    if (tank.getFluid() != null) {
                        world.setBlockState(leakPos, tank.getFluid().getFluid().getBlock().getDefaultState());
                        tank.drainInternal(Fluid.BUCKET_VOLUME, true);
                    }
                }
            }

            if (world.getTotalWorldTime() % 60 == 0 || shouldUpdate) {
                shouldUpdate = false;
                world.notifyNeighborsOfStateChange(pos, blockType);

                Quantum.getPacketHandler().sendToReceivers(new PacketTileEntity(this), this);
            }

            if (world.isRemote) {
                // Particles of white smoke will rise from above the reactor chamber when above water boiling temperature.
                if (world.rand.nextInt(5) == 0 && getTemperature() >= ThermalPhysics.waterBoilTemperature) {
                    world.spawnParticle(EnumParticleTypes.CLOUD, pos.getX() + world.rand.nextInt(2), pos.getY() + 1, pos.getZ() + world.rand.nextInt(2), 0.0D, 0.1D, 0.0D);
                    world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, pos.getX() + world.rand.nextInt(5), pos.getY(), pos.getZ() + world.rand.nextInt(5), 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        temperature = tag.getFloat("temperature");
        getMultiBlock().readFromNBT(tag);

        if (tag.getTagId("Inventory") == Constants.NBT.TAG_LIST) {
            NBTTagList tagList = tag.getTagList("Inventory", Constants.NBT.TAG_COMPOUND);

            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound slotTag = (NBTTagCompound) tagList.get(i);
                byte slot = slotTag.getByte("Slot");

                if (slot < inventory.getSlots()) {
                    inventory.setStackInSlot(slot, ItemStack.loadItemStackFromNBT(slotTag));
                }
            }

            return;
        }

        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.readNBT(inventory, null, tag.getTag("Slots"));
        CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.readNBT(tank, null, tag.getTag("tank"));
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag = super.writeToNBT(tag);

        tag.setFloat("temperature", temperature);
        tag = getMultiBlock().writeToNBT(tag);
        tag.setTag("Slots", CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.writeNBT(inventory, null));
        tag.setTag("tank", CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.writeNBT(tank, null));

        return tag;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Position[] getMultiBlockVectors() {
        List<Position> positions = new ArrayList<>();
        Position checkPosition = new Position(this);

        while (true) {
            TileEntity tileEntity = checkPosition.getTileEntity(world);

            if (tileEntity instanceof TileReactorCell) {
                positions.add(checkPosition.clone().subtract(getPosition()));
            } else {
                break;
            }

            checkPosition = checkPosition.offset(EnumFacing.UP);
        }

        return positions.toArray(new Position[0]);
    }

    @Override
    public World getWorldObject() {
        return world;
    }

    @Override
    public void onMultiBlockChanged() {

    }

    @Override
    public Position getPosition() {
        return new Position(this);
    }

    @Override
    public MultiBlockHandler<TileReactorCell> getMultiBlock() {
        if (multiBlock == null) {
            multiBlock = new MultiBlockHandler<>(this);
        }

        return multiBlock;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        if (world.isRemote) {
            temperature = dataStream.readFloat();
            tank.handlePacketData(dataStream);
        }
    }

    @Override
    public List<Object> getPacketData(List<Object> objects) {
        objects.add(temperature);
        tank.getPacketData(objects);

        return objects;
    }

    @Override
    public void heat(long energy) {
        internalEnergy = Math.max(internalEnergy + energy, 0);
    }

    @Override
    public float getTemperature() {
        return temperature;
    }

    @Override
    public boolean isOverToxic() {
        return tank.getFluid() != null && tank.getFluid().getFluid() == QuantumFluids.fluidToxicWaste && tank.getFluid().amount >= tank.getCapacity();
    }

    @Override
    public FluidTank getTank() {
        return tank;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return world.getTileEntity(pos) == this && player.getDistanceSq(pos.add(0.5, 0.5, 0.5)) <= 64;
    }
    */

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public int getHeight() {
        int height = 0;
        TileEntity tile = this;
        Position checkPosition = new Position(this);
        //Vector3 checkPosition = new Vector3(this);

        while (tile instanceof TileReactorCell) {
            height++;
            tile = checkPosition.offset(EnumFacing.UP).getTileEntity(world);
        }

        return height;
    }

    public TileReactorCell getLowest() {
        TileReactorCell lowest = this;
        Position checkPosition = new Position(this);

        while (true) {
            TileEntity tile = checkPosition.getTileEntity(world);

            if (tile instanceof TileReactorCell) {
                lowest = (TileReactorCell) tile;
            } else {
                break;
            }

            checkPosition = checkPosition.offset(EnumFacing.DOWN);
        }

        return lowest;
    }

    public void updatePositionStatus() {
        TileReactorCell tile = getLowest();
        tile.getMultiBlock().deconstruct();
        tile.getMultiBlock().construct();

        boolean top = world.getTileEntity(pos.up()) instanceof TileReactorCell;
        boolean bottom = world.getTileEntity(pos.down()) instanceof TileReactorCell;
        IBlockState state = world.getBlockState(pos);

        if (top && bottom) {
            world.setBlockState(pos, state.withProperty(BlockStateReactorCell.TYPE, EnumReactorCell.MIDDLE));
        } else if (top) {
            world.setBlockState(pos, state.withProperty(BlockStateReactorCell.TYPE, EnumReactorCell.BOTTOM));
        } else {
            world.setBlockState(pos, state.withProperty(BlockStateReactorCell.TYPE, EnumReactorCell.TOP));
        }
    }

    private void meltDown() {
        // Make sure the reactor block is destroyed.
        world.setBlockToAir(pos);

        // No need to destroy reactor cell since explosion will do that for us.
        ReactorExplosion reactorExplosion = new ReactorExplosion(world, null, pos, 9);
        reactorExplosion.explode();
    }

    public IItemHandlerModifiable getInventory() {
        return inventory;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nonnull
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) getMultiBlock().get().getInventory();
        } else if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) getMultiBlock().get().getTank();
        }

        return super.getCapability(capability, facing);
    }
}
