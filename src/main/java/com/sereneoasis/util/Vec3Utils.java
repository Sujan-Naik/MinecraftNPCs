package com.sereneoasis.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class Vec3Utils {

    public static boolean isBlockSolid(Position pos, Level level) {
        BlockState blockState = level.getBlockState(BlockPos.containing(pos));
        return ! ( blockState.is(Blocks.AIR)  || blockState.is(Blocks.WATER)) ;
    }

    public static boolean isTopBlock(Position pos, Level level){
        return !isBlockSolid( BlockPos.containing(pos).above().getCenter(), level) && isBlockSolid( BlockPos.containing(pos).below().getCenter(), level);
    }

    public static boolean isObstructed(Position pos1, Position pos2, Level level) {
        boolean obstructed = false;
        for (BlockPos bp :   BlockPos.betweenClosed(BlockPos.containing(pos1), BlockPos.containing(pos2))){
            if (isBlockSolid(bp.getCenter(), level)) {
                obstructed = true;
            }
        }
        return obstructed;
    }
}
