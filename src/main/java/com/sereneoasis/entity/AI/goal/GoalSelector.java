package com.sereneoasis.entity.AI.goal;

import com.sereneoasis.entity.AI.goal.basic.BasicGoal;

import java.util.Comparator;
import java.util.PriorityQueue;

public class GoalSelector {

    public PriorityQueue<BasicGoal> goals;

    public GoalSelector() {
        goals = new PriorityQueue<>(Comparator.comparingInt(BasicGoal::getPriority));
    }


    public void addGoal(BasicGoal goal){
        goals.add(goal);
    }

    public boolean hasGoal(){
        if (goals.isEmpty()){
            return false;
        }
        return true;
    }

    public void tick(){
        if (goals.peek() != null) {
            BasicGoal currentBasicGoal = goals.peek();
            currentBasicGoal.tick();
            //Bukkit.broadcastMessage(currentBasicGoal.getName());
            if (currentBasicGoal.isFinished()){
                removeCurrentGoal();
            }
        }
    }
    
    public boolean doingGoal(String name){
        if (hasGoal()) {
            return name.equals(goals.peek().getName());
        }
        return false;
    }

    public void removeCurrentGoal(){
        goals.remove();
    }

    public void removeAllGoals(){
        goals.removeIf(basicGoal -> true);
    }
}
