package com.sereneoasis.entity.AI.goal.basic.movement;

import com.sereneoasis.entity.AI.goal.basic.BasicGoal;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.world.phys.Vec3;

public abstract class Movement extends BasicGoal {

    private Vec3 goalPos;

    private double requiredDistance;

    public Movement(String name, HumanEntity npc, int priority, Vec3 goalPos, double requiredDistance) {
        super(name, npc, priority);

        this.goalPos = goalPos;
        this.requiredDistance = 5;
    }

    public Vec3 getGoalPos() {
        return goalPos;
    }

    public void setGoalPos(Vec3 goalPos) {
        this.goalPos = goalPos;
    }

    public double getDistance(){
        return goalPos.distanceTo(npc.getPosition(0));
    }

    @Override
    public void tick() {
        if (getDistance() > requiredDistance) {
            npc.getNavigation().moveTo(goalPos.x, goalPos.y, goalPos.z, 10);
        } else {
            finished = true;
        }
    }
}
