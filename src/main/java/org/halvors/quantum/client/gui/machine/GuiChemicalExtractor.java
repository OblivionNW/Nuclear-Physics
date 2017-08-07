package org.halvors.quantum.client.gui.machine;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.halvors.quantum.client.gui.GuiContainerBase;
import org.halvors.quantum.common.block.machine.BlockMachineModel.EnumMachineModel;
import org.halvors.quantum.common.container.machine.ContainerChemicalExtractor;
import org.halvors.quantum.common.tile.machine.TileChemicalExtractor;
import org.halvors.quantum.common.utility.LanguageUtility;
import org.halvors.quantum.common.utility.energy.UnitDisplay;

@SideOnly(Side.CLIENT)
public class GuiChemicalExtractor extends GuiContainerBase {
    private TileChemicalExtractor tile;

    public GuiChemicalExtractor(InventoryPlayer inventoryPlayer, TileChemicalExtractor tile) {
        super(new ContainerChemicalExtractor(inventoryPlayer, tile));

        this.tile = tile;
    }

    /** Draw the foreground layer for the GuiContainer (everything in front of the items) */
    @Override
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = LanguageUtility.transelate("tile.machine_model." + EnumMachineModel.CHEMICAL_EXTRACTOR.ordinal() + ".name");

        fontRendererObj.drawString(name, (xSize / 2) - (fontRendererObj.getStringWidth(name) / 2), 6, 0x404040);

        renderUniversalDisplay(8, 112, TileChemicalExtractor.energy * 20, mouseX, mouseY, UnitDisplay.Unit.WATT);
        //renderUniversalDisplay(100, 112, tile.getVoltageInput(null), mouseX, mouseY, UnitDisplay.Unit.VOLTAGE);

        fontRendererObj.drawString("The extractor can extract", 8, 75, 0x404040);
        fontRendererObj.drawString("uranium, deuterium and tritium.", 8, 85, 0x404040);
        fontRendererObj.drawString("Place them in the input slot.", 8, 95, 0x404040);

        fontRendererObj.drawString(LanguageUtility.transelate("container.inventory"), 8, ySize - 96 + 2, 0x404040);

        if (isPointInRegion(8, 18, meterWidth, meterHeight, mouseX, mouseY)) {
            if (tile.getInputTank().getFluid() != null) {
                drawTooltip(mouseX - guiLeft, mouseY - guiTop + 10, tile.getInputTank().getFluid().getLocalizedName(), tile.getInputTank().getFluid().amount + " L");
            } else {
                drawTooltip(mouseX - guiLeft, mouseY - guiTop + 10, "No Fluid"); // TODO: Localize this.
            }
        }

        if (isPointInRegion(154, 18, meterWidth, meterHeight, mouseX, mouseY)) {
            if (tile.getOutputTank().getFluid() != null) {
                drawTooltip(mouseX - guiLeft, mouseY - guiTop + 10, tile.getOutputTank().getFluid().getLocalizedName(), tile.getOutputTank().getFluid().amount + " L");
            } else {
                drawTooltip(mouseX - guiLeft, mouseY - guiTop + 10, "No Fluid"); // TODO: Localize this.
            }
        }

        /*
        if (isPointInRegion(134, 49, 18, 18, mouseX, mouseY)) {
            if (tile.getInventory().getStackInSlot(4) == null) {
                // drawTooltip(x - guiLeft, y - guiTop + 10, "Place empty cells.");
            }
        }

        if (isPointInRegion(52, 24, 18, 18, mouseX, mouseY)) {
            if (tile.getOutputTank().getFluidAmount() > 0 && tile.getInventory().getStackInSlot(3) == null) {
                drawTooltip(mouseX - guiLeft, mouseY - guiTop + 10, "Input slot");
            }
        }
        */
    }

    /** Draw the background layer for the GuiContainer (everything behind the items) */
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);

        drawSlot(79, 49, SlotType.BATTERY);
        drawSlot(52, 24);
        drawSlot(106, 24);
        drawBar(75, 24, (float) tile.timer / (float) TileChemicalExtractor.tickTime);
        drawMeter(8, 18, tile.getInputTank().getFluidAmount() / tile.getInputTank().getCapacity(), tile.getInputTank().getFluid());
        drawSlot(24, 18, SlotType.LIQUID);
        drawSlot(24, 49, SlotType.LIQUID);
        drawMeter(154, 18, tile.getOutputTank().getFluidAmount() / tile.getOutputTank().getCapacity(), tile.getOutputTank().getFluid());
        drawSlot(134, 18, SlotType.LIQUID);
        drawSlot(134, 49, SlotType.LIQUID);
    }
}