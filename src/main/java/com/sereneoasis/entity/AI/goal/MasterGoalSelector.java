package com.sereneoasis.entity.AI.goal;

import com.sereneoasis.entity.AI.goal.complex.MasterGoal;
import com.sereneoasis.entity.HumanEntity;

import java.util.Stack;

public class MasterGoalSelector {

    public Stack<MasterGoal> goals;

    public MasterGoalSelector() {
        this.goals = new Stack<>();

    }

    public void addMasterGoal(MasterGoal newGoal){
        this.goals.push(newGoal);
    }

    public boolean hasGoal(){
        if (goals.isEmpty()){
            return false;
        }
        return true;
    }

    public boolean doingGoal(String name){
        if (hasGoal()){
            return goals.stream().anyMatch(masterGoal -> masterGoal.getName().equals(name));
        }
        return false;
    }

    public void tick(){
        if (hasGoal()) {
            if (goals.peek().finished) {
                goals.pop();
            } else {
                goals.peek().tick();
            }
        }
    }

}
