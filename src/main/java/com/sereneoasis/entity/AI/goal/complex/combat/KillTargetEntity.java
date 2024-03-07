package com.sereneoasis.entity.AI.goal.complex.combat;

import com.sereneoasis.entity.AI.goal.NPCStates;
import com.sereneoasis.entity.AI.goal.basic.combat.BowRangedAttackEntity;
import com.sereneoasis.entity.AI.goal.basic.combat.PunchEntity;
import com.sereneoasis.entity.AI.goal.basic.movement.MoveToEntity;
import com.sereneoasis.entity.HumanEntity;

public class KillTargetEntity extends MasterCombat{

    private int lastShotBowTicks;

    private int lastPunchTicks;

    public KillTargetEntity(String name, HumanEntity npc) {
        super(name, npc);
        entity = npc.getTargetSelector().retrieveTopPlayer();
        if (entity == null){
            finished = true;
        }

        state = NPCStates.AGGRESSIVE;
        this.lastShotBowTicks = npc.tickCount;
        this.lastPunchTicks = npc.tickCount;
    }

    @Override
    public void tick() {
        super.tick();
        double distance = npc.distanceToSqr(entity);

        if (!movementGoalSelector.doingGoal("Chase")) {
            movementGoalSelector.addGoal(new MoveToEntity("Chase", npc, 1, 1, entity));
        }
        if (distance <= 4 && npc.tickCount - lastPunchTicks > 5 ) {
            actionGoalSelector.addGoal(new PunchEntity("Punch", npc, 1, entity));
            this.lastPunchTicks = npc.tickCount;
        }

        if (distance >= 100 && npc.tickCount - lastShotBowTicks > 200)
        {
            actionGoalSelector.addGoal(new BowRangedAttackEntity("Bow", npc, 1, entity));
            this.lastShotBowTicks = npc.tickCount;
        }

    }
}
