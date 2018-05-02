package org.halvors.nuclearphysics.client.gui.configuration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Set;

@SideOnly(Side.CLIENT)
public class GuiConfiguationFactory implements IModGuiFactory {
    @Override
    public void initialize(final Minecraft instance) {

    }

    @Override
    public boolean hasConfigGui() {
        return false;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiConfiguration(parentScreen);
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return GuiConfiguration.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public RuntimeOptionGuiHandler getHandlerFor(final RuntimeOptionCategoryElement element) {
        return null;
    }
}