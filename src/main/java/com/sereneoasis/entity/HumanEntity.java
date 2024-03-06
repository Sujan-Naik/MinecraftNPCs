package com.sereneoasis.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_20_R2.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

public class HumanEntity extends ServerPlayer {

    private LivingEntity owner;

    public HumanEntity(MinecraftServer server, ServerLevel world, GameProfile profile, ClientInformation clientOptions) {
        super(server, world, profile, clientOptions);
        Player player = this.getBukkitEntity().getPlayer();
        player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
        player.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));

    }

    public void setOwner(LivingEntity owner){
        this.owner = owner;
    }

    private int attackTime = -1;
    private int attackIntervalMin = 20;

    private int timeSinceBowDraw = -1;

    @Override
    public void tick() {
        super.tick();
        if (owner != null) {
            if (this.distanceToSqr(this.owner) <= 256.0) {
                this.lookAt(EntityAnchorArgument.Anchor.EYES, owner, EntityAnchorArgument.Anchor.EYES);

                if (this.distanceToSqr(this.owner) >= 100) {
                    this.moveTo(owner.getPosition(0));
                }

                if (this.isUsingItem()) {
                    int drawingTime = this.server.getTickCount() - timeSinceBowDraw;
                    if (drawingTime >= 20) {
                        this.stopUsingItem();
                        this.performRangedAttack(owner, BowItem.getPowerForTime(drawingTime));
                        this.attackTime = this.attackIntervalMin;
                        this.timeSinceBowDraw = -1;
                    }
                } else if (--this.attackTime <= 0) {
                    this.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
                    timeSinceBowDraw = this.server.getTickCount();
                }

               // checkAndPerformAttack(owner);
            }
        }


    }

    protected AbstractArrow getArrow(net.minecraft.world.item.ItemStack arrow, float damageModifier) {
        return ProjectileUtil.getMobArrow(this, arrow, damageModifier);
    }

    public void performRangedAttack(LivingEntity target, float pullProgress) {
        net.minecraft.world.item.ItemStack itemstack = this.getProjectile(this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW)));
        AbstractArrow entityarrow = this.getArrow(itemstack, pullProgress);
        double d0 = target.getX() - this.getX();
        double d1 = target.getY(0.3333333333333333) - entityarrow.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        entityarrow.shoot(d0, d1 + d3 * 0.20000000298023224, d2, 1.6F, (float)(14 - this.level().getDifficulty().getId() * 4));
        EntityShootBowEvent event = CraftEventFactory.callEntityShootBowEvent(this, this.getMainHandItem(), entityarrow.getPickupItem(), entityarrow, InteractionHand.MAIN_HAND, 0.8F, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
        } else {
            if (event.getProjectile() == entityarrow.getBukkitEntity()) {
                this.level().addFreshEntity(entityarrow);
            }
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        }
    }

    protected void checkAndPerformAttack(LivingEntity target) {
        if (this.canAttack(target)) {
           // Bukkit.broadcastMessage("test");
            this.resetAttackStrengthTicker();
            this.swing(InteractionHand.MAIN_HAND);
            this.attack(target);
           // this.doHurtTarget(target);
        }

    }

//    protected void findTarget() {
//        if (this.targetType != net.minecraft.world.entity.player.Player.class && this.targetType != ServerPlayer.class) {
//            this.target = this.mob.level().getNearestEntity(this.mob.level().getEntitiesOfClass(this.targetType, this.getTargetSearchArea(this.getFollowDistance()), (entityliving) -> {
//                return true;
//            }), this.targetConditions, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
//        } else {
//            this.target = this.mob.level().getNearestPlayer(this.targetConditions, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
//        }
//
//    }

//    protected AABB getTargetSearchArea(double distance) {
//        return this.mob.getBoundingBox().inflate(distance, 4.0, distance);
//    }

//    private Vec3 getPosition() {
//
//    }
}
