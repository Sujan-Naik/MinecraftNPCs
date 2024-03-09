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
    public MoveForward(String name, HumanEntity npc, int priority, double requiredDistance) {
        super(name, npc, priority, null, requiredDistance);
      //  this.lastGoalTicks = npc.tickCount;
    }

    @Override
    public void tick() {

      //  if (npc.tickCount - lastGoalTicks > 4) {
            Vec3 floorBlockLoc = npc.getOnPos().getCenter().add(npc.getForward().scale(2));
            if (possible(floorBlockLoc)) {
                isStuck = false;
                setGoalPos(getNextLoc(floorBlockLoc));
            //    Bukkit.broadcastMessage("why isn't this moving");
            }
            else {
                isStuck = true;
            }
       //     lastGoalTicks = npc.tickCount;
      //  }
        super.tick();
        // npc.travel(new Vec3(0,1,0));
    }

    private boolean possible(Vec3 floorBlockLoc){
        boolean hasValidLocation = false;
        for (BlockPos bp :   BlockPos.betweenClosed(BlockPos.containing(floorBlockLoc.add(0,1,0)), BlockPos.containing(floorBlockLoc.subtract(0,3,0)))){
            if (Vec3Utils.isBlockSolid(bp.getCenter(), npc.level()) ){
                hasValidLocation = true;
            }
        }
        for (BlockPos bp :   BlockPos.betweenClosed(BlockPos.containing(floorBlockLoc.add(0,2,0)), BlockPos.containing(floorBlockLoc.add(0,4,0)))){
            if (Vec3Utils.isBlockSolid(bp.getCenter(), npc.level()) ){
                hasValidLocation = false;
            }
        }
        return hasValidLocation;

    }

    private Vec3 getNextLoc(Vec3 floorBlockLoc) {

        BlockPos topLoc = BlockPos.containing(floorBlockLoc);
        for (BlockPos bp :   BlockPos.betweenClosed(BlockPos.containing(floorBlockLoc.subtract(0,1,0)), BlockPos.containing(floorBlockLoc.subtract(0,3,0)))){
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
}
