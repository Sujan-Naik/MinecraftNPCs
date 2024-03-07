package com.sereneoasis.entity.AI.goal.complex;

import com.sereneoasis.entity.AI.goal.BaseGoal;
import com.sereneoasis.entity.AI.goal.GoalSelector;
import com.sereneoasis.entity.AI.goal.NPCStates;
import com.sereneoasis.entity.AI.goal.basic.BasicGoal;
import com.sereneoasis.entity.HumanEntity;

import java.util.PriorityQueue;

public abstract class MasterGoal extends BaseGoal {

    protected GoalSelector goalSelector;

    protected NPCStates state;

    public MasterGoal(String name, HumanEntity npc) {
        super(name, npc);

        this.goalSelector = new GoalSelector();
        this.state = NPCStates.RELAXED;
    }


    @Override
    public void tick() {
        goalSelector.tick();
    }
}
