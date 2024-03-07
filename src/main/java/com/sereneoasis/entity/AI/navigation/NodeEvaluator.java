//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.sereneoasis.entity.AI.navigation;

import com.sereneoasis.entity.HumanEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Target;

public abstract class NodeEvaluator {
    protected PathNavigationRegion level;
    protected HumanEntity mob;
    protected final Int2ObjectMap<Node> nodes = new Int2ObjectOpenHashMap();
    protected int entityWidth;
    protected int entityHeight;
    protected int entityDepth;
    protected boolean canPassDoors;
    protected boolean canOpenDoors;
    protected boolean canFloat;
    protected boolean canWalkOverFences;

    public NodeEvaluator() {
    }

    public void prepare(PathNavigationRegion cachedWorld, HumanEntity entity) {
        this.level = cachedWorld;
        this.mob = entity;
        this.nodes.clear();
        this.entityWidth = Mth.floor(entity.getBbWidth() + 1.0F);
        this.entityHeight = Mth.floor(entity.getBbHeight() + 1.0F);
        this.entityDepth = Mth.floor(entity.getBbWidth() + 1.0F);
    }

    public void done() {
        this.level = null;
        this.mob = null;
    }

    protected Node getNode(BlockPos pos) {
        return this.getNode(pos.getX(), pos.getY(), pos.getZ());
    }

    protected Node getNode(int x, int y, int z) {
        return (Node)this.nodes.computeIfAbsent(Node.createHash(x, y, z), (l) -> {
            return new Node(x, y, z);
        });
    }

    public abstract Node getStart();

    public abstract Target getGoal(double x, double y, double z);

    protected Target getTargetFromNode(Node node) {
        return new Target(node);
    }

    public abstract int getNeighbors(Node[] successors, Node node);

    public abstract BlockPathTypes getBlockPathType(BlockGetter world, int x, int y, int z, HumanEntity mob);

    public abstract BlockPathTypes getBlockPathType(BlockGetter world, int x, int y, int z);

    public void setCanPassDoors(boolean canEnterOpenDoors) {
        this.canPassDoors = canEnterOpenDoors;
    }

    public void setCanOpenDoors(boolean canOpenDoors) {
        this.canOpenDoors = canOpenDoors;
    }

    public void setCanFloat(boolean canSwim) {
        this.canFloat = canSwim;
    }

    public void setCanWalkOverFences(boolean canWalkOverFences) {
        this.canWalkOverFences = canWalkOverFences;
    }

    public boolean canPassDoors() {
        return this.canPassDoors;
    }

    public boolean canOpenDoors() {
        return this.canOpenDoors;
    }

    public boolean canFloat() {
        return this.canFloat;
    }

    public boolean canWalkOverFences() {
        return this.canWalkOverFences;
    }
}
