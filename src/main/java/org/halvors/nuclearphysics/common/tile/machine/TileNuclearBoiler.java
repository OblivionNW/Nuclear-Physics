package org.halvors.nuclearphysics.common.tile.machine;

import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;
import org.halvors.nuclearphysics.common.ConfigurationManager;
import org.halvors.nuclearphysics.common.NuclearPhysics;
import org.halvors.nuclearphysics.common.capabilities.fluid.LiquidTank;
import org.halvors.nuclearphysics.common.init.ModFluids;
import org.halvors.nuclearphysics.common.network.packet.PacketTileEntity;
import org.halvors.nuclearphysics.common.utility.EnergyUtility;
import org.halvors.nuclearphysics.common.utility.InventoryUtility;
import org.halvors.nuclearphysics.common.utility.OreDictionaryHelper;

public class TileNuclearBoiler extends TileProcess {
    public static final int tickTime = 20 * 15;
    public static final int energy = 20000;

    public TileNuclearBoiler() {
        energyStorage = new EnergyStorage(energy * 2);
        inventory = new ItemStackHandler(5) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                markDirty();
            }

            private boolean isItemValidForSlot(int slot, ItemStack itemStack) {
                switch (slot) {
                    case 0: // Battery input slot.
                        return EnergyUtility.canBeDischarged(itemStack);

                    case 1: // Item input slot.
                        return OreDictionaryHelper.isUraniumOre(itemStack) || OreDictionaryHelper.isYellowCake(itemStack);

                    case 2: // Input tank fill slot.
                    case 3: // Input tank drain slot.
                        return OreDictionaryHelper.isEmptyCell(itemStack) || OreDictionaryHelper.isWaterCell(itemStack);

                    case 4: // Output tank drain slot.
                        return OreDictionaryHelper.isEmptyCell(itemStack); // TODO: Add uranium hexaflouride container here.
                }

                return false;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!isItemValidForSlot(slot, stack)) {
                    return stack;
                }

                return super.insertItem(slot, stack, simulate);
            }
        };

        tankInput = new LiquidTank(ModFluids.fluidStackWater.copy(),Fluid.BUCKET_VOLUME * 5) {
            @Override
            public int fill(FluidStack resource, boolean doFill) {
                if (resource.isFluidEqual(ModFluids.fluidStackWater)) {
                    return super.fill(resource, doFill);
                }

                return 0;
            }

            // TODO: Only allow internal draining?
            /*
            @Override
            public boolean canDrain() {
                return true;
            }
            */
        };

        tankOutput = new LiquidTank(ModFluids.fluidStackUraniumHexaflouride.copy(), Fluid.BUCKET_VOLUME * 5) {
            // TODO: Only allow internal filling?
            /*
            @Override
            public boolean canFill() {
                return false;
            }
            */

            @Override
            public FluidStack drain(FluidStack resource, boolean doDrain) {
                if (resource.isFluidEqual(ModFluids.fluidStackUraniumHexaflouride)) {
                    return drain(resource.amount, doDrain);
                }

                return null;
            }
        };

        inputSlot = 1;

        tankInputFillSlot = 2;
        tankInputDrainSlot = 3;
        tankOutputDrainSlot = 4;
    }

    @Override
    public void update() {
        super.update();

        if (timer > 0) {
            rotation += 0.1;
        } else {
            rotation = 0;
        }

        if (!world.isRemote) {
            if (canProcess()) {
                EnergyUtility.discharge(0, this);

                if (energyStorage.extractEnergy(energy, true) >= energy) {
                    if (timer == 0) {
                        timer = tickTime;
                    }

                    if (timer > 0) {
                        timer--;

                        if (timer < 1) {
                            doProcess();
                            timer = 0;
                        }
                    } else {
                        timer = 0;
                    }

                    energyStorage.extractEnergy(energy, false);
                } else {
                    timer = 0;
                }
            } else {
                timer = 0;
            }

            if (world.getWorldTime() % 10 == 0) {
                if (!world.isRemote) {
                    NuclearPhysics.getPacketHandler().sendToReceivers(new PacketTileEntity(this), this);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return side == EnumFacing.DOWN ? new int[] { 2 } : new int[] { 1, 3 };
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemstack, EnumFacing side) {
        return slot == 2;
    }
    */

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Check all conditions and see if we can start processing
    public boolean canProcess() {
        FluidStack inputFluidStack = tankInput.getFluid();

        if (inputFluidStack != null && inputFluidStack.amount >= Fluid.BUCKET_VOLUME) {
            ItemStack itemStack = inventory.getStackInSlot(inputSlot);

            if (itemStack != null) {
                if (OreDictionaryHelper.isUraniumOre(itemStack) || OreDictionaryHelper.isYellowCake(itemStack)) {
                    FluidStack outputFluidStack = tankOutput.getFluid();

                    if (outputFluidStack != null && outputFluidStack.amount < tankOutput.getCapacity()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Turn one item from the furnace source stack into the appropriate smelted item in the furnace result stack.
    public void doProcess() {
        if (canProcess()) {
            tankInput.drainInternal(Fluid.BUCKET_VOLUME, true);
            FluidStack liquid = ModFluids.fluidStackUraniumHexaflouride.copy();
            liquid.amount = ConfigurationManager.General.uraniumHexaflourideRatio * 2;
            tankOutput.fillInternal(liquid, true);
            InventoryUtility.decrStackSize(inventory, inputSlot);
        }
    }
}
