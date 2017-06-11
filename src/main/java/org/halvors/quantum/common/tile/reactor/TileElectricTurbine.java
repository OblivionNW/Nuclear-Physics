package org.halvors.quantum.common.tile.reactor;

import cofh.api.energy.EnergyStorage;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;
import org.halvors.quantum.common.ConfigurationManager;
import org.halvors.quantum.common.Reference;
import org.halvors.quantum.common.base.tile.ITileNetworkable;
import org.halvors.quantum.common.multiblock.TurbineMultiBlockHandler;
import org.halvors.quantum.common.network.NetworkHandler;
import org.halvors.quantum.common.network.packet.PacketTileEntity;
import org.halvors.quantum.common.transform.vector.Vector3;
import org.halvors.quantum.lib.multiblock.IMultiBlockStructure;
import org.halvors.quantum.lib.thermal.IBoilHandler;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** TileElectricTurbineX
 *
 * 1 cubic meter of steam = 338260 J of energy
 *
 * The front of the turbine is where the output is.
 */
public class TileElectricTurbine extends TileElectrical implements IMultiBlockStructure<TileElectricTurbine>, ITileNetworkable, IBoilHandler {
    // Amount of energy per liter of steam. Boil Water Energy = 327600 + 2260000 = 2587600
    protected final long energyPerSteam = 2647600 / 1000;
    protected final FluidTank tank = new FluidTank(FluidContainerRegistry.BUCKET_VOLUME * 100);
    protected final long defaultTorque = 5000;
    protected long torque = defaultTorque;

    // Radius of large turbine?
    public int multiBlockRadius = 1;

    // The power of the turbine this tick. In joules/tick
    public long power = 0;

    // Current rotation of the turbine in radians.
    public float rotation = 0;

    public int tier = 0; // Synced

    // Max power in watts.
    protected long maxPower = 5000000; // 13 RF average?
    protected float prevAngularVelocity = 0;
    protected float angularVelocity = 0; // Synced

    // MutliBlock methods.
    private TurbineMultiBlockHandler multiBlock;

    public TileElectricTurbine() {
        setEnergyStorage(new EnergyStorage((int) maxPower * 20));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.getBoundingBox(xCoord - multiBlockRadius, yCoord - multiBlockRadius, zCoord - multiBlockRadius, xCoord + 1 + multiBlockRadius, yCoord + 1 + multiBlockRadius, zCoord + 1 + multiBlockRadius);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (getMultiBlock().isConstructed()) {
            torque = defaultTorque * 500 * getArea();
        } else {
            torque = defaultTorque * 500;
        }

        getMultiBlock().update();

        if (getMultiBlock().isPrimary()) {
            if (!worldObj.isRemote) {
                // Increase spin rate and consume steam.
                if (tank.getFluidAmount() > 0 && power < maxPower) {
                    power += tank.drain((int) Math.ceil(Math.min(tank.getFluidAmount() * 0.1, getMaxPower() / energyPerSteam)), true).amount * energyPerSteam;
                }

                // Set angular velocity based on power and torque.
                angularVelocity = (power * 4) / torque;

                if (worldObj.getWorldTime() % 3 == 0 && prevAngularVelocity != angularVelocity) {
                    NetworkHandler.sendToReceivers(new PacketTileEntity(this), this);
                    prevAngularVelocity = angularVelocity;
                }

                if (power > 0) {
                    generateEnergy();
                }
            }

            if (angularVelocity != 0) {
                playSound();

                // Update rotation.
                rotation = (float) ((rotation + angularVelocity / 20) % (Math.PI * 2));
            }
        } else if (tank.getFluidAmount() > 0) {
            getMultiBlock().get().tank.fill(tank.drain(getMultiBlock().get().tank.fill(tank.getFluid(), false), true), true);
        }

        if (!worldObj.isRemote) {
            power = 0;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        tank.readFromNBT(nbt);
        multiBlockRadius = nbt.getInteger("multiBlockRadius");
        getMultiBlock().load(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        tank.writeToNBT(nbt);
        nbt.setInteger("multiBlockRadius", multiBlockRadius);
        getMultiBlock().save(nbt);
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return getMultiBlock().isPrimary() && from == ForgeDirection.UP;
    }

    @Override
    public EnumSet<ForgeDirection> getReceivingDirections() {
        return EnumSet.noneOf(ForgeDirection.class);
    }

    @Override
    public EnumSet<ForgeDirection> getExtractingDirections()
    {
        return EnumSet.of(ForgeDirection.UP);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Vector3[] getMultiBlockVectors() {
        Set<Vector3> vectors = new HashSet<>();

        ForgeDirection dir = getDirection();
        int xMulti = dir.offsetX != 0 ? 0 : 1;
        int yMulti = dir.offsetY != 0 ? 0 : 1;
        int zMulti = dir.offsetZ != 0 ? 0 : 1;

        for (int x = -multiBlockRadius; x <= multiBlockRadius; x++) {
            for (int y = -multiBlockRadius; y <= multiBlockRadius; y++) {
                for (int z = -multiBlockRadius; z <= multiBlockRadius; z++) {
                    vectors.add(new Vector3(x * xMulti, y * yMulti, z * zMulti));
                }
            }
        }

        return vectors.toArray(new Vector3[0]);
    }

    @Override
    public World getWorldObject() {
        return worldObj;
    }

    @Override
    public void onMultiBlockChanged() {
        worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType(), 0);
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public Vector3 getPosition()
    {
        return new Vector3(this);
    }

    @Override
    public TurbineMultiBlockHandler getMultiBlock() {
        if (multiBlock == null) {
            multiBlock = new TurbineMultiBlockHandler(this);
        }

        return multiBlock;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void handlePacketData(ByteBuf dataStream) throws Exception {
        if (worldObj.isRemote) {
            tier = dataStream.readInt();
            angularVelocity = dataStream.readFloat();
        }
    }

    @Override
    public List<Object> getPacketData(List<Object> objects) {
        objects.add(tier);
        objects.add(angularVelocity);

        return objects;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (resource != null && canFill(from, resource.getFluid())) {
            return getMultiBlock().get().tank.fill(resource, doFill);
        }

        return 0;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
    {
        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
    {
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return from == ForgeDirection.DOWN && fluid != null && fluid.getName().equals("steam");
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid)
    {
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from)
    {
        return new FluidTankInfo[] { tank.getInfo() };
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public ForgeDirection getDirection() {
        return ForgeDirection.getOrientation(getBlockMetadata());
    }

    protected long getMaxPower() {
        if (getMultiBlock().isConstructed()) {
            return maxPower * getArea();
        }

        return maxPower;
    }

    public int getArea()
    {
        return (int) (((multiBlockRadius + 0.5) * 2) * ((multiBlockRadius + 0.5) * 2));
    }


    public void generateEnergy() {
        if (power > 0) {
            energyStorage.receiveEnergy((int) (power * ConfigurationManager.General.turbineOutputMultiplier), false);
            produce();
        }
    }

    public void playSound() {
        if (worldObj.getWorldTime() % 26 == 0) {
            double maxVelocity = (getMaxPower() / torque) * 4;
            float percentage = angularVelocity * 4 / (float) maxVelocity;

            worldObj.playSoundEffect(xCoord, yCoord, zCoord, Reference.PREFIX + "turbine", percentage, 1);
        }
    }
}
