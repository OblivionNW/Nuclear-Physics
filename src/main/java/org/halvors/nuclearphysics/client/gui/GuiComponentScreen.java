package org.halvors.nuclearphysics.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import org.halvors.nuclearphysics.client.gui.component.IGuiComponent;
import org.halvors.nuclearphysics.client.utility.RenderUtility;
import org.halvors.nuclearphysics.common.type.Resource;
import org.halvors.nuclearphysics.common.utility.ResourceUtility;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class GuiComponentScreen extends GuiScreen implements IGuiWrapper {
    protected final ResourceLocation defaultResource = ResourceUtility.getResource(Resource.GUI, "empty.png");
    protected Set<IGuiComponent> components = new HashSet<>();

    /** The X size of the inventory window in pixels. */
    protected int xSize = 176;

    /** The Y size of the inventory window in pixels. */
    protected int ySize = 217;

    /** Starting X position for the Gui. Inconsistent use for Gui backgrounds. */
    protected int guiLeft;

    /** Starting Y position for the Gui. Inconsistent use for Gui backgrounds. */
    protected int guiTop;

    public GuiComponentScreen() {

    }

    @Override
    public void initGui() {
        super.initGui();

        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();

        GL11.glColor4d(1, 1, 1, 1);

        drawGuiScreenBackgroundLayer(partialTick, mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTick);

        GL11.glTranslated(guiLeft, guiTop, 0);

        drawGuiScreenForegroundLayer(mouseX, mouseY);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    protected void drawGuiScreenForegroundLayer(int mouseX, int mouseY) {
        int xAxis = (mouseX - (width - xSize) / 2);
        int yAxis = (mouseY - (height - ySize) / 2);

        for (IGuiComponent component : components) {
            component.renderForeground(xAxis, yAxis);
        }
    }

    protected void drawGuiScreenBackgroundLayer(float partialTick, int mouseX, int mouseY) {
        RenderUtility.bindTexture(defaultResource);

        GL11.glColor4d(1, 1, 1, 1);

        int guiWidth = (width - xSize) / 2;
        int guiHeight = (height - ySize) / 2;

        drawTexturedModalRect(guiWidth, guiHeight, 0, 0, xSize, ySize);

        int xAxis = mouseX - guiWidth;
        int yAxis = mouseY - guiHeight;

        for (IGuiComponent component : components) {
            component.renderBackground(xAxis, yAxis, guiWidth, guiHeight);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        int xAxis = (mouseX - (width - xSize) / 2);
        int yAxis = (mouseY - (height - ySize) / 2);

        for (IGuiComponent component : components) {
            component.preMouseClicked(xAxis, yAxis, button);
        }

        super.mouseClicked(mouseX, mouseY, button);

        for (IGuiComponent component : components) {
            component.mouseClicked(xAxis, yAxis, button);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long ticks) {
        super.mouseClickMove(mouseX, mouseY, button, ticks);

        int xAxis = (mouseX - (width - xSize) / 2);
        int yAxis = (mouseY - (height - ySize) / 2);

        for (IGuiComponent component : components) {
            component.mouseClickMove(xAxis, yAxis, button, ticks);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int type) {
        super.mouseReleased(mouseX, mouseY, type);

        int xAxis = (mouseX - (width - xSize) / 2);
        int yAxis = (mouseY - (height - ySize) / 2);

        for (IGuiComponent component : components) {
            component.mouseReleased(xAxis, yAxis, type);
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();

        int xAxis = Mouse.getEventX() * width / mc.displayWidth - getXPos();
        int yAxis = height - Mouse.getEventY() * height / mc.displayHeight - 1 - getYPos();
        int delta = Mouse.getEventDWheel();

        if (delta != 0) {
            mouseWheel(xAxis, yAxis, delta);
        }
    }

    public void mouseWheel(int xAxis, int yAxis, int delta) {
        for (IGuiComponent component : components) {
            component.mouseWheel(xAxis, yAxis, delta);
        }
    }

    public int getXPos() {
        return (width - xSize) / 2;
    }

    public int getYPos() {
        return (height - ySize) / 2;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int w, int h) {
        drawTexturedModalRect(x, y, u, v, w, h);
    }

    @Override
    public void drawTexturedRectFromIcon(int x, int y, IIcon icon, int w, int h) {
        drawTexturedModalRect(x, y, icon.getIconWidth(), icon.getIconHeight(), w, h);
    }

    @Override
    public void displayTooltip(String text, int xAxis, int yAxis) {
        drawCreativeTabHoveringText(text, xAxis, yAxis);
    }

    @Override
    public void displayTooltips(List<String> list, int xAxis, int yAxis) {
        drawHoveringText(list, xAxis, yAxis);
    }

    @Override
    public FontRenderer getFontRenderer() {
        return fontRendererObj;
    }
}