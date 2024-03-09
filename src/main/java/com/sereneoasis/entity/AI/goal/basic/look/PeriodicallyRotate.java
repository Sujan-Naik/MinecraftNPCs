package com.sereneoasis.entity.AI.goal.basic.look;

import com.sereneoasis.entity.AI.goal.basic.BasicGoal;
import com.sereneoasis.entity.HumanEntity;

public class PeriodicallyRotate extends BasicGoal {

    private int sinceLastRotate;
    private int rotateCounter;

    private float maxRotateRange;

    public PeriodicallyRotate(String name, HumanEntity npc, int priority, int rotateCounter, float maxRotateRange) {
        super(name, npc, priority);

        this.sinceLastRotate = npc.tickCount;
        this.rotateCounter = rotateCounter;
        this.maxRotateRange = maxRotateRange;
    }

    public void prematureRotate(){
        npc.setYRot((float) (npc.getBukkitYaw() + (Math.random()-0.5 * maxRotateRange)));
        sinceLastRotate = npc.tickCount;
    }

    @Override
    public void tick() {
        if (npc.tickCount - sinceLastRotate > rotateCounter) {
            npc.setYRot((float) (npc.getBukkitYaw() + (Math.random()-0.5 * maxRotateRange)));
            sinceLastRotate = npc.tickCount;
        }
    }
}
