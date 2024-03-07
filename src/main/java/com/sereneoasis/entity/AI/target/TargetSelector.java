package com.sereneoasis.entity.AI.target;

import com.sereneoasis.entity.HumanEntity;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TargetSelector {

    private PriorityQueue<Entity> hostileEntityStack;

    private PriorityQueue<Entity> peacefulEntityStack;

    private PriorityQueue<Player> players;

    private HumanEntity npc;

    public TargetSelector(HumanEntity owner) {
        this.npc = owner;
        this.hostileEntityStack = new PriorityQueue<>((a,b ) -> (int) (npc.distanceToSqr(a) - npc.distanceToSqr(b)));
        this.peacefulEntityStack = new PriorityQueue<>((a,b ) -> (int) (npc.distanceToSqr(a) - npc.distanceToSqr(b)));
        this.players = new PriorityQueue<>((a,b ) -> (int) (npc.distanceToSqr(a) - npc.distanceToSqr(b)));
    }

    public Entity retrieveTopHostile(){
        return hostileEntityStack.poll();
    }

    public Entity retrieveTopPeaceful(){
        return peacefulEntityStack.poll();
    }

    public Player retrieveTopPlayer(){
        return players.poll();
    }


    public void tick(){

        for (Entity entity : npc.level().getEntities(npc,new AABB(npc.getOnPos()).inflate(20))) {
            if (entity instanceof Player player){
                players.add(player);
            }
            else if (entity instanceof Monster monster){
                hostileEntityStack.add(monster);
            }
            else if (entity instanceof Animal animal) {
                peacefulEntityStack.add(animal);
            }
        }
    }
}