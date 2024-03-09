package com.sereneoasis.entity.AI.goal.complex.movement;

import com.sereneoasis.entity.AI.goal.complex.MasterGoal;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.core.BlockPos;

import java.util.function.Predicate;

public class MasterMovement extends MasterGoal {

    protected Predicate<BlockPos>condition;
    public MasterMovement(String name, HumanEntity npc, Predicate<BlockPos> condition) {
        super(name, npc);

        this.condition = condition;
    }

    @Override
    public void tick() {
        if (condition != null && condition.test(npc.blockPosition())){
            finished = true;
        } else {
            super.tick();
        }


    }
}
