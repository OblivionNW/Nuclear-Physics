package org.halvors.nuclearphysics.common.block.reactor.fission;

import net.minecraft.block.material.Material;
import org.halvors.nuclearphysics.common.block.BlockRadioactive;

import java.util.Random;

public class BlockRadioactiveGrass extends BlockRadioactive {
    public BlockRadioactiveGrass() {
        super("radioactive_grass", Material.GRASS);

        setHardness(0.2F);

        canSpread = true;
        radius = 5;
        amplifier = 2;
        canWalkPoison = true;
        isRandomlyRadioactive = true;
        spawnParticle = true;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }
}
