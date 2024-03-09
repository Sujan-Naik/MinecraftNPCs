package com.sereneoasis.entity.AI.goal.complex.interaction;

import com.sereneoasis.entity.AI.goal.basic.interaction.BreakBlock;
import com.sereneoasis.entity.AI.goal.basic.look.LookAroundForBlock;
import com.sereneoasis.entity.AI.goal.basic.movement.MoveToBlock;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.Bukkit;

import java.util.stream.Collectors;

public class GatherBlocks extends MasterInteraction{

    private int requiredAmount;
    private Block block;

    private BlockPos targetPos;

    private LookAroundForBlock lookAroundForBlock;

    private boolean goalAcquired = false;

    private boolean goalInReach = false;

    public GatherBlocks(String name, HumanEntity npc, Block block, int requiredAmount) {
        super(name, npc);
        this.requiredAmount = requiredAmount;
        this.block = block;
    }

    @Override
    public void tick() {

        if ( npc.getInventory().getContents().stream().filter(itemStack -> itemStack.is(block.asItem()))
                .map(ItemStack::getCount).mapToInt(Integer::intValue).sum() >= requiredAmount ){
            finished = true;
        }
        else {

            super.tick();

            if (goalAcquired) {
                if (goalInReach) {
                    lookGoalSelector.removeAllGoals();
                    actionGoalSelector.addGoal(new BreakBlock("break", npc, targetPos));
                    targetPos = null;
                    goalInReach = false;
                    goalAcquired = false;
                } else {
                    if (!movementGoalSelector.doingGoal("move")) {
                        movementGoalSelector.addGoal(new MoveToBlock("move", npc, targetPos));
                    }
                    if (npc.getPosition(0).distanceToSqr(targetPos.getCenter()) < 16) {
                        goalInReach = true;
                    }
                }
            } else {
                if (lookAroundForBlock != null && lookAroundForBlock.isFinished()) {
                    targetPos = lookAroundForBlock.getFinishedBlockPos();
                    goalAcquired = true;
                } else if (!lookGoalSelector.doingGoal("look around")) {
                    this.lookAroundForBlock = new LookAroundForBlock("look around", this.npc, 1, blockPos -> npc.level().getBlockState(blockPos).is(block));
                    lookGoalSelector.addGoal(lookAroundForBlock);
                }
            }
        }
    }
}
