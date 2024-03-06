package com.sereneoasis.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftHumanEntity;

public class HumanEntity extends ServerPlayer {

    private Entity owner;

    public HumanEntity(MinecraftServer server, ServerLevel world, GameProfile profile, ClientInformation clientOptions) {
        super(server, world, profile, clientOptions);

    }

    public void setOwner(Entity owner){
        this.owner = owner;
    }

    @Override
    public void tick() {
        super.tick();
        if (owner != null) {
            if (this.distanceToSqr(this.owner) <= 256.0) {
                this.lookAt(EntityAnchorArgument.Anchor.EYES, owner, EntityAnchorArgument.Anchor.EYES);

                this.moveTo(owner.getPosition(0));
            }
        }

        //this.setDeltaMovement(1,0,0);

        //this.setDeltaMovement(1,0,0);
        //this.setPlayerInput(1, 0,false, false);
//        if (this.onGround()) {
//            this.setJumping(true);
//        }


    }
}
