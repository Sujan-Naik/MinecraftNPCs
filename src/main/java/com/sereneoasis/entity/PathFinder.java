//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.sereneoasis.entity;

import com.google.common.collect.Lists;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;

public class PathFinder {
    private static final float FUDGING = 1.5F;
    private final Node[] neighbors = new Node[32];
    private final int maxVisitedNodes;
    public final NodeEvaluator nodeEvaluator;
    private static final boolean DEBUG = false;
    private final BinaryHeap openSet = new BinaryHeap();

    public PathFinder(NodeEvaluator pathNodeMaker, int range) {
        this.nodeEvaluator = pathNodeMaker;
        this.maxVisitedNodes = range;
    }

    @Nullable
    public Path findPath(PathNavigationRegion world, HumanEntity mob, Set<BlockPos> positions, float followRange, int distance, float rangeMultiplier) {
        this.openSet.clear();
        this.nodeEvaluator.prepare(world, mob);
        Node node = this.nodeEvaluator.getStart();
        if (node == null) {
            return null;
        } else {
            List<Map.Entry<Target, BlockPos>> map = Lists.newArrayList();
            Iterator var9 = positions.iterator();

            while(var9.hasNext()) {
                BlockPos pos = (BlockPos)var9.next();
                map.add(new AbstractMap.SimpleEntry(this.nodeEvaluator.getGoal((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()), pos));
            }

            Path path = this.findPath((ProfilerFiller)world.getProfiler(), (Node)node, (List)map, followRange, distance, rangeMultiplier);
            this.nodeEvaluator.done();
            return path;
        }
    }

    @Nullable
    private Path findPath(ProfilerFiller profiler, Node startNode, List<Map.Entry<Target, BlockPos>> positions, float followRange, int distance, float rangeMultiplier) {
        profiler.push("find_path");
        profiler.markForCharting(MetricCategory.PATH_FINDING);
        startNode.g = 0.0F;
        startNode.h = this.getBestH(startNode, positions);
        startNode.f = startNode.h;
        this.openSet.clear();
        this.openSet.insert(startNode);
        int i = 0;
        List<Map.Entry<Target, BlockPos>> entryList = Lists.newArrayListWithExpectedSize(positions.size());
        int j = (int)((float)this.maxVisitedNodes * rangeMultiplier);

        while(!this.openSet.isEmpty()) {
            ++i;
            if (i >= j) {
                break;
            }

            Node node = this.openSet.pop();
            node.closed = true;

            int k;
            for(k = 0; k < positions.size(); ++k) {
                Map.Entry<Target, BlockPos> entry = (Map.Entry)positions.get(k);
                Target target = (Target)entry.getKey();
                if (node.distanceManhattan(target) <= (float)distance) {
                    target.setReached();
                    entryList.add(entry);
                }
            }

            if (!entryList.isEmpty()) {
                break;
            }

            if (!(node.distanceTo(startNode) >= followRange)) {
                k = this.nodeEvaluator.getNeighbors(this.neighbors, node);

                for(int l = 0; l < k; ++l) {
                    Node node2 = this.neighbors[l];
                    float f = this.distance(node, node2);
                    node2.walkedDistance = node.walkedDistance + f;
                    float g = node.g + f + node2.costMalus;
                    if (node2.walkedDistance < followRange && (!node2.inOpenSet() || g < node2.g)) {
                        node2.cameFrom = node;
                        node2.g = g;
                        node2.h = this.getBestH(node2, positions) * 1.5F;
                        if (node2.inOpenSet()) {
                            this.openSet.changeCost(node2, node2.g + node2.h);
                        } else {
                            node2.f = node2.g + node2.h;
                            this.openSet.insert(node2);
                        }
                    }
                }
            }
        }

        Path best = null;
        boolean entryListIsEmpty = entryList.isEmpty();
        Comparator<Path> comparator = entryListIsEmpty ? Comparator.comparingInt(Path::getNodeCount) : Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount);
        Iterator var21 = ((List)(entryListIsEmpty ? positions : entryList)).iterator();

        while(true) {
            Path path;
            do {
                if (!var21.hasNext()) {
                    return best;
                }

                Map.Entry<Target, BlockPos> entry = (Map.Entry)var21.next();
                path = this.reconstructPath(((Target)entry.getKey()).getBestNode(), (BlockPos)entry.getValue(), !entryListIsEmpty);
            } while(best != null && comparator.compare(path, best) >= 0);

            best = path;
        }
    }

    protected float distance(Node a, Node b) {
        return a.distanceTo(b);
    }

    private float getBestH(Node node, List<Map.Entry<Target, BlockPos>> targets) {
        float f = Float.MAX_VALUE;
        int i = 0;

        for(int targetsSize = targets.size(); i < targetsSize; ++i) {
            Target target = (Target)((Map.Entry)targets.get(i)).getKey();
            float g = node.distanceTo(target);
            target.updateBest(g, node);
            f = Math.min(g, f);
        }

        return f;
    }

    private Path reconstructPath(Node endNode, BlockPos target, boolean reachesTarget) {
        List<Node> list = Lists.newArrayList();
        Node node = endNode;
        list.add(0, endNode);

        while(node.cameFrom != null) {
            node = node.cameFrom;
            list.add(0, node);
        }

        return new Path(list, target, reachesTarget);
    }
}
