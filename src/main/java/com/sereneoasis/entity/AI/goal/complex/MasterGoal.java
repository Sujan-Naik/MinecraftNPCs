package com.sereneoasis.entity.AI.goal.complex;

import com.sereneoasis.entity.AI.goal.BaseGoal;
import com.sereneoasis.entity.AI.goal.GoalSelector;
import com.sereneoasis.entity.AI.goal.NPCStates;
import com.sereneoasis.entity.HumanEntity;

public abstract class MasterGoal extends BaseGoal {

    protected GoalSelector actionGoalSelector;
    protected GoalSelector movementGoalSelector;

    protected GoalSelector lookGoalSelector;

    protected NPCStates state;

    public MasterGoal(String name, HumanEntity npc) {
        super(name, npc);

        this.actionGoalSelector = new GoalSelector();
        this.movementGoalSelector = new GoalSelector();
        this.lookGoalSelector = new GoalSelector();
        this.state = NPCStates.RELAXED;
    }


    @Override
    public void tick() {
        actionGoalSelector.tick();
        movementGoalSelector.tick();
        lookGoalSelector.tick();
    }
}
