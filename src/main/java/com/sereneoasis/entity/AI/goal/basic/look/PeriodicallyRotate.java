package com.sereneoasis.entity.AI.goal.basic.look;

import com.sereneoasis.entity.AI.goal.basic.BasicGoal;
import com.sereneoasis.entity.HumanEntity;
import org.bukkit.Bukkit;

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

    public void prematureRotate(double temporaryMaxRotateRange){
        npc.setYRot((float) (npc.getBukkitYaw() +  (Math.random()-0.5) * temporaryMaxRotateRange));
        sinceLastRotate = npc.tickCount;
    }

    @Override
    public void tick() {
        if (npc.tickCount - sinceLastRotate > rotateCounter) {
            //Bukkit.broadcastMessage("rotating head");
            npc.setYRot((float) (npc.getBukkitYaw() +  (Math.random()-0.5) * maxRotateRange));
            sinceLastRotate = npc.tickCount;
        }
    }
}
