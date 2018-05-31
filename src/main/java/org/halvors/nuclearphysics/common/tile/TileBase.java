package org.halvors.nuclearphysics.common.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class TileBase extends TileEntity {
    private final Set<EntityPlayer> playersUsing = new HashSet<>();

    public void open(final EntityPlayer player) {
        playersUsing.add(player);
    }

    public void close(final EntityPlayer player) {
        playersUsing.remove(player);
    }

    public Set<EntityPlayer> getPlayersUsing() {
        return playersUsing;
    }

    public TileBase() {

    }

    @Override
    @Nonnull
    public NBTTagCompound getUpdateTag() {
        // Forge writes only x/y/z/id info to a new NBT Tag Compound. This is fine, we have a custom network storage
        // to send other data so we don't use this one (yet).
        return super.getUpdateTag();
    }

    @Override
    public void handleUpdateTag(@Nonnull final NBTTagCompound tag) {
        // The super implementation of handleUpdateTag is to call this readFromNBT. But, the given TagCompound
        // only has x/y/z/id data, so our readFromNBT will set a bunch of default values which are wrong.
        // So simply call the super's readFromNBT, to let Forge do whatever it wants, but don't treat this like
        // a full NBT object, don't pass it to our custom read methods.
        super.readFromNBT(tag);
    }
}
