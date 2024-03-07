package com.sereneoasis.entity.AI.goal.complex.combat;

import com.sereneoasis.entity.AI.goal.NPCStates;
import com.sereneoasis.entity.AI.goal.basic.combat.BowRangedAttackEntity;
import com.sereneoasis.entity.AI.goal.basic.combat.PunchEntity;
import com.sereneoasis.entity.AI.goal.basic.movement.MoveToEntity;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class KillTargetEntity extends MasterCombat{

    private int lastShotBowTicks;

    public KillTargetEntity(String name, HumanEntity npc) {
        super(name, npc);
        entity = npc.getTargetSelector().retrieveTopPlayer();
        if (entity == null){
            finished = true;
        }

        state = NPCStates.AGGRESSIVE;
        this.lastShotBowTicks = npc.tickCount;
    }

    @Override
    public void tick() {
        super.tick();
        double distance = npc.distanceToSqr(entity);

        if (state != NPCStates.RANGED) {
            if (distance >= 4) {
                goalSelector.addGoal(new MoveToEntity("Chase", npc, 3, 2, entity));
                state = NPCStates.PURSUIT;
            } else if (state == NPCStates.PURSUIT || state == NPCStates.MELEE) {
                goalSelector.addGoal(new PunchEntity("Punch", npc, 2, entity));
                state = NPCStates.MELEE;
            }
        } else {
            if (npc.tickCount - lastShotBowTicks > 20) {
                state = NPCStates.PURSUIT;
            }
        }

        if (distance >= 100 && state == NPCStates.PURSUIT && npc.tickCount - lastShotBowTicks > 100) {
            state = NPCStates.RANGED;
            goalSelector.addGoal(new BowRangedAttackEntity("Bow", npc, 1, entity));
            this.lastShotBowTicks = npc.tickCount;
        }

    }
}
