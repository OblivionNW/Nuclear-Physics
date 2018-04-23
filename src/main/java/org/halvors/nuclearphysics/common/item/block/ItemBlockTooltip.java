package org.halvors.nuclearphysics.common.item.block;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.halvors.nuclearphysics.common.Reference;
import org.halvors.nuclearphysics.common.type.Color;
import org.halvors.nuclearphysics.common.utility.LanguageUtility;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemBlockTooltip extends ItemBlockBase {
    public ItemBlockTooltip(final Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull final ItemStack itemStack, @Nonnull final EntityPlayer player, @Nonnull final List<String> list, final boolean flag) {
        final String tooltip = getUnlocalizedName(itemStack) + ".tooltip";

        if (LanguageUtility.canTranselate(tooltip)) {
            if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                list.add(LanguageUtility.transelate("tooltip." + Reference.ID + ".noShift", Color.AQUA.toString(), Color.GREY.toString()));
            } else {
                list.addAll(LanguageUtility.splitStringPerWord(LanguageUtility.transelate(tooltip), 5));
            }
        }
    }
}
