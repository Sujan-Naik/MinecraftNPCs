//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.sereneoasis.entity.AI.navigation;

import javax.annotation.Nullable;

import com.sereneoasis.entity.HumanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;


import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation extends PathNavigation {
    private boolean avoidSun;

    public GroundPathNavigation(HumanEntity entity, Level world) {
        super(entity, world);
    }

    protected PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, range);
    }

    protected boolean canUpdatePath() {
        return this.mob.onGround() || this.mob.isInLiquid() || this.mob.isPassenger();
    }

    protected Vec3 getTempHumanEntityPos() {
        return new Vec3(this.mob.getX(), (double)this.getSurfaceY(), this.mob.getZ());
    }

    public Path createPath(BlockPos target, @Nullable Entity entity, int distance) {
        LevelChunk levelChunk = this.level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(target.getX()), SectionPos.blockToSectionCoord(target.getZ()));
        if (levelChunk == null) {
            return null;
        } else {
            BlockPos blockPos2;
            if (levelChunk.getBlockState(target).isAir()) {
                for(blockPos2 = target.below(); blockPos2.getY() > this.level.getMinBuildHeight() && levelChunk.getBlockState(blockPos2).isAir(); blockPos2 = blockPos2.below()) {
                }

                if (blockPos2.getY() > this.level.getMinBuildHeight()) {
                    return super.createPath(blockPos2.above(), entity, distance);
                }

                while(blockPos2.getY() < this.level.getMaxBuildHeight() && levelChunk.getBlockState(blockPos2).isAir()) {
                    blockPos2 = blockPos2.above();
                }

                target = blockPos2;
            }

            if (!levelChunk.getBlockState(target).isSolid()) {
                return super.createPath(target, entity, distance);
            } else {
                for(blockPos2 = target.above(); blockPos2.getY() < this.level.getMaxBuildHeight() && levelChunk.getBlockState(blockPos2).isSolid(); blockPos2 = blockPos2.above()) {
                }

                return super.createPath(blockPos2, entity, distance);
            }
        }
    }

    public Path createPath(Entity entity, int distance) {
        return this.createPath(entity.blockPosition(), entity, distance);
    }

    private int getSurfaceY() {
        if (this.mob.isInWater() && this.canFloat()) {
            int i = this.mob.getBlockY();
            BlockState blockState = this.level.getBlockState(BlockPos.containing(this.mob.getX(), (double)i, this.mob.getZ()));
            int j = 0;

            do {
                if (!blockState.is(Blocks.WATER)) {
                    return i;
                }

                ++i;
                blockState = this.level.getBlockState(BlockPos.containing(this.mob.getX(), (double)i, this.mob.getZ()));
                ++j;
            } while(j <= 16);

            return this.mob.getBlockY();
        } else {
            return Mth.floor(this.mob.getY() + 0.5);
        }
    }

    protected void trimPath() {
        super.trimPath();
        if (this.avoidSun) {
            if (this.level.canSeeSky(BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()))) {
                return;
            }

            for(int i = 0; i < this.path.getNodeCount(); ++i) {
                Node node = this.path.getNode(i);
                if (this.level.canSeeSky(new BlockPos(node.x, node.y, node.z))) {
                    this.path.truncateNodes(i);
                    return;
                }
            }
        }

    }

    protected boolean hasValidPathType(BlockPathTypes pathType) {
        if (pathType == BlockPathTypes.WATER) {
            return false;
        } else if (pathType == BlockPathTypes.LAVA) {
            return false;
        } else {
            return pathType != BlockPathTypes.OPEN;
        }
    }

    public void setCanOpenDoors(boolean canPathThroughDoors) {
        this.nodeEvaluator.setCanOpenDoors(canPathThroughDoors);
    }

    public boolean canPassDoors() {
        return this.nodeEvaluator.canPassDoors();
    }

    public void setCanPassDoors(boolean canEnterOpenDoors) {
        this.nodeEvaluator.setCanPassDoors(canEnterOpenDoors);
    }

    public boolean canOpenDoors() {
        return this.nodeEvaluator.canPassDoors();
    }

    public void setAvoidSun(boolean avoidSunlight) {
        this.avoidSun = avoidSunlight;
    }

    public void setCanWalkOverFences(boolean canWalkOverFences) {
        this.nodeEvaluator.setCanWalkOverFences(canWalkOverFences);
    }
}
