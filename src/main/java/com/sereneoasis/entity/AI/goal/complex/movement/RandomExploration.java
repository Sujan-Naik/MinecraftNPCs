package com.sereneoasis.entity.AI.goal.complex.movement;

import com.sereneoasis.entity.AI.goal.basic.look.PeriodicallyRotate;
import com.sereneoasis.entity.AI.goal.basic.movement.MoveForward;
import com.sereneoasis.entity.AI.goal.basic.movement.MoveToBlock;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.Optional;
import java.util.function.Predicate;

public class RandomExploration extends MasterMovement{

    private MoveForward moveForward;
    private PeriodicallyRotate periodicallyRotate;

    public RandomExploration(String name, HumanEntity npc, Predicate<BlockPos> condition) {
        super(name, npc, condition);

        this.moveForward = new MoveForward("move", npc, 1, 0);
        movementGoalSelector.addGoal(moveForward);

        this.periodicallyRotate = new PeriodicallyRotate("rotate", npc, 1, 20, 30);
        lookGoalSelector.addGoal(periodicallyRotate);
    }

    @Override
    public void tick() {
        super.tick();

        if (moveForward.isStuck()){
            periodicallyRotate.prematureRotate();
            moveForward.tick();
        }
    }
}
