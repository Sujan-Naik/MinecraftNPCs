package com.sereneoasis.entity.AI.goal;

import com.sereneoasis.entity.HumanEntity;

public abstract class BaseGoal {

    private String name;

    protected boolean finished;

    protected boolean inProgress;

    protected HumanEntity npc;

    private int priority;

    public BaseGoal(String name, HumanEntity npc){
        this.name = name;
        this.npc = npc;
    }

    public abstract void tick();

    public String getName() {
        return name;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public HumanEntity getNpc() {
        return npc;
    }

    public int getPriority() {
        return priority;
    }
}
