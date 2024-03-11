package com.sereneoasis.entity.AI.goal.basic.movement;

import com.sereneoasis.entity.AI.goal.basic.BasicGoal;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;

import java.util.Objects;

public abstract class Movement extends BasicGoal {

    private Vec3 goalPos;

    private double requiredDistance;

    public Movement(String name, HumanEntity npc, int priority, Vec3 goalPos, double requiredDistance) {
        super(name, npc, priority);

        this.goalPos = goalPos;
        this.requiredDistance = requiredDistance;
    }

    public Vec3 getGoalPos() {
        return goalPos;
    }

    public void setGoalPos(Vec3 goalPos) {
        this.goalPos = goalPos;
    }

    public double getDistance(){
        return goalPos.distanceTo(npc.getOnPos().getCenter());
    }

    @Override
    public void tick() {

//            if (npc.getNavigation().isStuck() ) {
//                npc.getNavigation().recomputePath();
//            }
    if (goalPos != null) {
        if (getDistance() > requiredDistance) {
            npc.getNavigation().moveTo(goalPos.x, goalPos.y, goalPos.z, 10);
            //  npc.getNavigation().createPath(BlockPos.containing(goalPos), 1000);
        } else {
            finished = true;
        }
    }
    }
}
