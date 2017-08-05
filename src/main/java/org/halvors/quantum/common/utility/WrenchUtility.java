package org.halvors.quantum.common.utility;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.halvors.quantum.common.ConfigurationManager.Integration;

public class WrenchUtility {
	/**
	 * Whether or not the player has a usable wrench for a block at the coordinates given.
	 * @param player - the player using the wrench
	 * @param x - the x coordinate of the block being wrenched
	 * @param y - the y coordinate of the block being wrenched
	 * @param z - the z coordinate of the block being wrenched
	 * @return if the player can use the wrench
	 */
	public static boolean hasUsableWrench(EntityPlayer player, BlockPos pos) {
		ItemStack itemStack = player.getHeldItemMainhand();

		if (itemStack != null) {
			Item item = itemStack.getItem();

			if (Integration.isMekanismEnabled) {
				// Check if item is a Mekanism wrench.
				/*
				if (item instanceof IMekWrench) {
					IMekWrench wrench = (IMekWrench) item;

					return wrench.canUseWrench(player, pos.getX(), pos.getY(), pos.getZ());
				}
				*/
			}
		}

		return false;
	}
}
