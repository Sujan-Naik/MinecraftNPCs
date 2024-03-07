package com.sereneoasis.entity.AI.goal.basic;

import com.sereneoasis.entity.AI.goal.BaseGoal;
import com.sereneoasis.entity.HumanEntity;

public abstract class BasicGoal extends BaseGoal {


    private int priority;

    public BasicGoal(String name, HumanEntity npc, int priority){
        super(name, npc);
        this.priority = priority;
    }


    public int getPriority() {
        return priority;
    }
}
