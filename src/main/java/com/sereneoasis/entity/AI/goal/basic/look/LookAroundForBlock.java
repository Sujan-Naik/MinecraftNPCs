package com.sereneoasis.entity.AI.goal.basic.look;

import com.sereneoasis.entity.AI.goal.basic.BasicGoal;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;

import java.util.function.Predicate;

public class LookAroundForBlock extends BasicGoal {

    private Predicate<BlockPos>condition;

    private BlockPos finishedBlockPos;

    public LookAroundForBlock(String name, HumanEntity npc, int priority, Predicate<BlockPos>condition) {
        super(name, npc, priority);
        if (condition == null) {
            finished = true;
        }
        this.condition = condition;
    }

    @Override
    public void tick() {
        if (npc.getRayTrace(100, ClipContext.Fluid.NONE) instanceof BlockHitResult blockHitResult) {
            BlockPos lookingPos = blockHitResult.getBlockPos();

            if ( condition.test(lookingPos) && npc.getNavigation().isStableDestination(lookingPos)) {
                finished = true;
                finishedBlockPos = lookingPos;
            }
        }

        if (!finished) {
            npc.setYRot(npc.getBukkitYaw() + 10);
        }
    }

    public BlockPos getFinishedBlockPos() {
        return finishedBlockPos;
    }
}
