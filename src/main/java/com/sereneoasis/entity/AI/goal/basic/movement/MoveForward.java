package com.sereneoasis.entity.AI.goal.basic.movement;

import com.sereneoasis.entity.HumanEntity;
import com.sereneoasis.util.Vec3Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class MoveForward extends Movement{

    //private int lastGoalTicks;

    private boolean isStuck = true;
    private boolean isFucked = false;

    public MoveForward(String name, HumanEntity npc, int priority, double requiredDistance) {
        super(name, npc, priority, null, requiredDistance);
      //  this.lastGoalTicks = npc.tickCount;
    }

    @Override
    public void tick() {
     //   if (!isFucked) {
            //  if (npc.tickCount - lastGoalTicks > 4) {
            BlockPos floorBlock = npc.getOnPos();

            Vec3 currentFloorBlock = floorBlock.getCenter();
            Vec3 dir = npc.getForward().scale(1);
            Vec3 nextFloorBlock = currentFloorBlock.add(dir);
            if (possible(nextFloorBlock)) {
                isStuck = false;
                Vec3 targetLoc = getNextLoc(nextFloorBlock);
                npc.getMoveControl().setWantedPosition(targetLoc.x, targetLoc.y, targetLoc.z, 10);
            } else {
                isStuck = true;
                boolean hasAnyValidLocation = false;
                for (int i = 90; i < 360; i += 90) {
                    if (possible(currentFloorBlock.add(dir.yRot(i)))) {
                        hasAnyValidLocation = true;
                        break;
                    }
                }
                if (!hasAnyValidLocation) {
                    isFucked = true;
                } else {
                    isFucked = false;
                }
            }
            //     lastGoalTicks = npc.tickCount;
            //  }
            //super.tick();
            // npc.travel(new Vec3(0,1,0));
      //  }
    }

    private boolean possible(Vec3 floorBlockLoc){
        boolean hasValidLocation = false;
        BlockPos highestBottom = null;
        for (BlockPos bp :   BlockPos.betweenClosed(BlockPos.containing(floorBlockLoc.add(0,1,0)), BlockPos.containing(floorBlockLoc.subtract(0,3,0)))){
            if (Vec3Utils.isBlockSolid(bp.getCenter(), npc.level()) ){
//                Bukkit.broadcastMessage("there is a floor");
                if (highestBottom == null || bp.getY() > highestBottom.getY()){
                    highestBottom = bp;
                }
                hasValidLocation = true;
            }
        }
        if (hasValidLocation) {
            for (BlockPos bp : BlockPos.betweenClosed(highestBottom.above(), highestBottom.above(3))) {
                if (Vec3Utils.isBlockSolid(bp.getCenter(), npc.level())) {
                    hasValidLocation = false;
                }
            }
        }
        return hasValidLocation;


    }

    private Vec3 getNextLoc(Vec3 floorBlockLoc) {

        BlockPos topLoc = BlockPos.containing(floorBlockLoc.subtract(0,3,0));
        for (BlockPos bp :   BlockPos.betweenClosed(BlockPos.containing(floorBlockLoc.add(0,1,0)), BlockPos.containing(floorBlockLoc.subtract(0,3,0)))){
            if (Vec3Utils.isBlockSolid(bp.getCenter(), npc.level()) && bp.getY() > topLoc.getY() ){
                topLoc = bp;
            }
        }
        return topLoc.getCenter();
    }

    public boolean isStuck() {
        return isStuck;
    }

    public void setStuck(boolean stuck) {
        isStuck = stuck;
    }

    public boolean isFucked() {
        return isFucked;
    }

    public void setFucked(boolean fucked) {
        isFucked = fucked;
    }
}
