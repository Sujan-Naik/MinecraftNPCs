package com.sereneoasis.entity.AI.goal.basic.interaction;

import com.sereneoasis.entity.AI.goal.BaseGoal;
import com.sereneoasis.entity.AI.goal.basic.BasicGoal;
import com.sereneoasis.entity.AI.goal.interfaces.BlockInteraction;
import com.sereneoasis.entity.AI.goal.interfaces.PlayerInteraction;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import org.bukkit.Bukkit;

public class BreakBlock extends BasicGoal implements BlockInteraction {

    private BlockPos blockPos;
    private Level level;

    public BreakBlock(String name, HumanEntity npc, BlockPos blockPos) {
        super(name, npc, 1);
        level = npc.level();
        this.blockPos = blockPos;
    }

    @Override
    public void tick() {
        if ( ! level.getBlockState(blockPos).getBlock().isDestroyable()){
            finished = true;
        }
        else {
            //level.getBlockState(blockPos).getBlock().playerDestroy(level, npc, blockPos, level.getBlockState(blockPos),
        //            null, npc.getItemInHand(InteractionHand.MAIN_HAND));
            level.destroyBlock(blockPos, true);
            finished = true;
        }

    }

    @Override
    public Block getBlock() {
        return null;
    }

    @Override
    public BlockPos getBlockPos() {
        return null;
    }
}
