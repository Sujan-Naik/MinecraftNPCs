package com.sereneoasis.entity.AI.goal.basic.movement;


import com.sereneoasis.entity.AI.goal.interfaces.BlockInteraction;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class MoveToBlock extends Movement implements BlockInteraction {

    private Block targetBlock;

    public MoveToBlock(String name, HumanEntity npc, int priority, Vec3 goalPos, double requiredDistance, Block targetBlock) {
        super(name, npc, priority, goalPos, requiredDistance);
        this.targetBlock = targetBlock;
    }

    @Override
    public Block getBlock() {
        return targetBlock;
    }
}
