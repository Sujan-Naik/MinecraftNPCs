package com.sereneoasis.entity.AI.goal.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

public interface BlockInteraction {

    Block getBlock();
    BlockPos getBlockPos();

}
