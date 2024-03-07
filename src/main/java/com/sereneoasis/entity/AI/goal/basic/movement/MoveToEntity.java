package com.sereneoasis.entity.AI.goal.basic.movement;

import com.sereneoasis.entity.AI.goal.interfaces.EntityInteraction;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class MoveToEntity extends Movement implements EntityInteraction {

    private LivingEntity targetEntity;

    public MoveToEntity(String name, HumanEntity npc, int priority, double requiredDistance, LivingEntity targetEntity) {
        super(name, npc, priority, targetEntity.getPosition(0), requiredDistance);
        this.targetEntity = targetEntity;
    }

    private void updatePos(){
        this.setGoalPos(targetEntity.getPosition(0));
    }

    @Override
    public LivingEntity getEntity() {
        return targetEntity;
    }

    @Override
    public void tick(){
        super.tick();
        this.updatePos();
    }
}
