package com.sereneoasis.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class Vec3Utils {

    public static boolean isBlockSolid(Position pos, Level level) {
        return !level.getBlockState(BlockPos.containing(pos)).is(Blocks.AIR);
    }

    public static boolean isTopBlock(Position pos, Level level){
        return !isBlockSolid( BlockPos.containing(pos).above().getCenter(), level) && isBlockSolid( BlockPos.containing(pos).below().getCenter(), level);
    }
}
