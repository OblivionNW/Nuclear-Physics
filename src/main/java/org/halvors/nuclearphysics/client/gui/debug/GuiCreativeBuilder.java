package org.halvors.nuclearphysics.client.gui.debug;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.halvors.nuclearphysics.client.gui.GuiContainerBase;
import org.halvors.nuclearphysics.client.utility.RenderUtility;
import org.halvors.nuclearphysics.common.NuclearPhysics;
import org.halvors.nuclearphysics.common.block.debug.BlockCreativeBuilder;
import org.halvors.nuclearphysics.common.container.ContainerDummy;
import org.halvors.nuclearphysics.common.network.packet.PacketCreativeBuilder;
import org.halvors.nuclearphysics.common.utility.LanguageUtility;
import org.halvors.nuclearphysics.common.utility.ResourceUtility;
import org.halvors.nuclearphysics.common.utility.type.Color;
import org.halvors.nuclearphysics.common.utility.type.ResourceType;

import java.io.IOException;

@SideOnly(Side.CLIENT)
public class GuiCreativeBuilder extends GuiContainerBase {
    private GuiTextField textFieldSize;
    private int mode = 0;
    private BlockPos pos;

    public ResourceLocation baseTexture;
    protected int containerWidth;
    protected int containerHeight;

    public GuiCreativeBuilder(BlockPos pos) {
        super(new ContainerDummy());

        this.pos = pos;
        this.baseTexture = ResourceUtility.getResource(ResourceType.GUI, "gui_empty.png");
    }

    @Override
    public void initGui() {
        super.initGui();

        textFieldSize = new GuiTextField(0, fontRendererObj, 45, 58, 50, 12);

        buttonList.add(new GuiButton(0, width / 2 - 80, height / 2 - 10, 58, 20, "Build"));
        buttonList.add(new GuiButton(1, width / 2 - 50, height / 2 - 35, 120, 20, "Mode"));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = LanguageUtility.transelate("tile.creative_builder.name"); //"Creative Builder";

        fontRendererObj.drawString(name, (xSize / 2) - (fontRendererObj.getStringWidth(name) / 2), 6, 0x404040);

        fontRendererObj.drawString("This is a creative only cheat", (xSize / 2) - 80, 20, 0x404040);
        fontRendererObj.drawString("which allows you to auto build", (xSize / 2) - 80, 30, 0x404040);
        fontRendererObj.drawString("structures for testing.", (xSize / 2) - 80, 40, 0x404040);

        fontRendererObj.drawString("Size: ", (xSize / 2) - 80, 60, 0x404040);
        textFieldSize.drawTextBox();

        (buttonList.get(1)).displayString = LanguageUtility.transelate(BlockCreativeBuilder.getSchematic(mode).getName());
        fontRendererObj.drawString("Mode: ", (xSize / 2) - 80, 80, 0x404040);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        fontRendererObj.drawString("Warning!", (xSize / 2) - 80, 130, Color.DARK_RED.getHex());
        fontRendererObj.drawString("This will replace blocks without", (xSize / 2) - 80, 140, Color.DARK_RED.getHex());
        fontRendererObj.drawString("dropping it! You may lose items.", (xSize / 2) - 80, 150, Color.DARK_RED.getHex());
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
        containerWidth = (width - xSize) / 2;
        containerHeight = (height - ySize) / 2;

        RenderUtility.bindTexture(baseTexture);
        GlStateManager.color(1, 1, 1, 1);

        drawTexturedModalRect(containerWidth, containerHeight, 0, 0, xSize, ySize);
    }

    @Override
    protected void keyTyped(char par1, int par2) throws IOException {
        super.keyTyped(par1, par2);

        textFieldSize.textboxKeyTyped(par1, par2);
    }

    @Override
    protected void mouseClicked(int x, int y, int par3) throws IOException {
        super.mouseClicked(x, y, par3);

        textFieldSize.mouseClicked(x - containerWidth, y - containerHeight, par3);
    }

    @Override
    protected void actionPerformed(GuiButton par1GuiButton) throws IOException {
        super.actionPerformed(par1GuiButton);

        if (par1GuiButton.id == 0) {
            int radius = 0;

            try {
                radius = Integer.parseInt(textFieldSize.getText());
            } catch (Exception e) {

            }

            if (radius > 0) {
                NuclearPhysics.getPacketHandler().sendToServer(new PacketCreativeBuilder(pos, mode, radius));
                mc.player.closeScreen();
            }
        } else if (par1GuiButton.id == 1) {
            mode = (mode + 1) % (BlockCreativeBuilder.getSchematicCount());
        }
    }

}