package com.sereneoasis.entity;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.GameProfile;
import com.sereneoasis.SerenityEntities;
import com.sereneoasis.util.PacketUtils;
import io.papermc.paper.util.MCUtil;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HumanEntity extends ServerPlayer {

    private LivingEntity owner;

    private PathNavigation navigation;

    private com.sereneoasis.entity.MoveControl moveControl;

    private LookControl lookControl;
    private com.sereneoasis.entity.JumpControl jumpControl;

    public HumanEntity(MinecraftServer server, ServerLevel world, GameProfile profile, ClientInformation clientOptions) {
        super(server, world, profile, clientOptions);

        this.moveControl = new MoveControl(this);
        this.jumpControl = new JumpControl(this);
        this.lookControl = new LookControl(this);

        this.navigation = new GroundPathNavigation(this, world);
        Player player = this.getBukkitEntity().getPlayer();
        player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
        player.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));

    }
    public float getPathfindingMalus(BlockPathTypes nodeType) {
        return 0;
    }

    public void onPathfindingStart() {
    }

    public void onPathfindingDone() {
    }

    public void setOwner(LivingEntity owner){
        this.owner = owner;
    }

    private int attackTime = -1;
    private int attackIntervalMin = 20;

    private int timeSinceBowDraw = -1;

    public int getMaxHeadXRot() {
        return 40;
    }

    public int getMaxHeadYRot() {
        return 75;
    }

    public int getHeadRotSpeed() {
        return 10;
    }

    @Override
    public void doTick() {
        // super.doTick();
    }
    private void updatingUsingItem() {
        if (this.isUsingItem()) {
            if (net.minecraft.world.item.ItemStack.isSameItem(this.getItemInHand(this.getUsedItemHand()), this.useItem)) {
                this.useItem = this.getItemInHand(this.getUsedItemHand());
                this.updateUsingItem(this.useItem);
            } else {
                this.stopUsingItem();
            }
        }

    }

    public void livingEntityTick(){
        super.tick();
        this.updatingUsingItem();
        //this.updateSwimAmount();
        if (!this.level().isClientSide) {
            int i = this.getArrowCount();
            if (i > 0) {
                if (this.removeArrowTime <= 0) {
                    this.removeArrowTime = 20 * (30 - i);
                }

                --this.removeArrowTime;
                if (this.removeArrowTime <= 0) {
                    this.setArrowCount(i - 1);
                }
            }

            int j = this.getStingerCount();
            if (j > 0) {
                if (this.removeStingerTime <= 0) {
                    this.removeStingerTime = 20 * (30 - j);
                }

                --this.removeStingerTime;
                if (this.removeStingerTime <= 0) {
                    this.setStingerCount(j - 1);
                }
            }

            this.detectEquipmentUpdatesPublic();
            if (this.tickCount % 20 == 0) {
                this.getCombatTracker().recheckStatus();
            }

        }

        if (!this.isRemoved()) {
            this.aiStep();
        }

        double d0 = this.getX() - this.xo;
        double d1 = this.getZ() - this.zo;
        float f = (float)(d0 * d0 + d1 * d1);
        float f1 = this.yBodyRot;
        float f2 = 0.0F;
        this.oRun = this.run;
        float f3 = 0.0F;
        if (f > 0.0025000002F) {
            f3 = 1.0F;
            f2 = (float)Math.sqrt((double)f) * 3.0F;
            float f4 = (float) Mth.atan2(d1, d0) * 57.295776F - 90.0F;
            float f5 = Mth.abs(Mth.wrapDegrees(this.getYRot()) - f4);
            if (95.0F < f5 && f5 < 265.0F) {
                f1 = f4 - 180.0F;
            } else {
                f1 = f4;
            }
        }

        if (this.attackAnim > 0.0F) {
            f1 = this.getYRot();
        }

        if (!this.onGround()) {
            f3 = 0.0F;
        }

        this.run += (f3 - this.run) * 0.3F;
        this.level().getProfiler().push("headTurn");
        f2 = this.tickHeadTurn(f1, f2);
        this.level().getProfiler().pop();
        this.level().getProfiler().push("rangeChecks");
        this.yRotO += (float)Math.round((this.getYRot() - this.yRotO) / 360.0F) * 360.0F;
        this.yBodyRotO += (float)Math.round((this.yBodyRot - this.yBodyRotO) / 360.0F) * 360.0F;
        this.xRotO += (float)Math.round((this.getXRot() - this.xRotO) / 360.0F) * 360.0F;
        this.yHeadRotO += (float)Math.round((this.yHeadRot - this.yHeadRotO) / 360.0F) * 360.0F;
        this.level().getProfiler().pop();
        this.animStep += f2;
        if (this.isFallFlying()) {
            ++this.fallFlyTicks;
        } else {
            this.fallFlyTicks = 0;
        }

        if (this.isSleeping()) {
            this.setXRot(0.0F);
        }

        //this.refreshDirtyAttributes();
    }

    @Override
    public void tick() {
//        super.tick();
        this.livingEntityTick();
        this.travel(new Vec3(0,0,1));

        if (owner != null) {
            if (this.distanceToSqr(this.owner) <= 256.0) {
               // this.lookAt(EntityAnchorArgument.Anchor.EYES, owner, EntityAnchorArgument.Anchor.EYES);

               /* if (this.navigation.moveTo(this.owner, 0.1)){
                    Bukkit.broadcastMessage(String.valueOf(this.position()));
                }

                navigation.tick();
                moveControl.tick();
                jumpControl.tick();
*/


                this.navigation.moveTo(this.owner, 0.1);


                this.level().getProfiler().push("navigation");
                this.navigation.tick();
                this.level().getProfiler().pop();

                this.level().getProfiler().push("controls");
                this.level().getProfiler().push("move");
                this.moveControl.tick();
                this.level().getProfiler().popPush("look");
                this.lookControl.tick();
                this.level().getProfiler().popPush("jump");
                this.jumpControl.tick();
                this.level().getProfiler().pop();
                this.level().getProfiler().pop();



                //this.moveTo(owner.getPosition(0));

//                for (Player player : Bukkit.getOnlinePlayers()){
//                    //PacketUtils.sendPacket(new ClientboundSetEntityMotionPacket(this), player);
//
//
//                    Location oldLoc = SerenityEntities.getInstance().getNpcs().get(this);
//
//                    Bukkit.broadcastMessage("x should be changing by " + (this.getX() - oldLoc.getX()));
//
//                    ClientboundMoveEntityPacket clientboundMoveEntityPacket = new ClientboundMoveEntityPacket.PosRot(this.getId(),
//                            PacketUtils.deltaPosition(this.getX(), oldLoc.getX()),
//                            PacketUtils.deltaPosition(this.getY(), oldLoc.getY()),
//                            PacketUtils.deltaPosition(this.getZ(), oldLoc.getZ()),
//                            (byte) this.getBukkitYaw(),
//                            (byte) this.getBukkitEntity().getPitch(),
//                            this.onGround);
//
//                    PacketUtils.sendPacket(clientboundMoveEntityPacket, player);
//
//
//
//                }
             //   SerenityEntities.getInstance().updateLocations();

//                if (this.distanceToSqr(this.owner) >= 100) {
//                    //this.moveTo(owner.getPosition(0));
//
//
//                }

        /*        if (this.isUsingItem()) {
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
*/
               // checkAndPerformAttack(owner);
            }

           // Bukkit.broadcastMessage(String.valueOf(this.getPosition(0)));
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

    public PathNavigation getNavigation() {
        return navigation;
    }

    public JumpControl getJumpControl() {
        return jumpControl;
    }

    public MoveControl getMoveControl() {
        return moveControl;
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
