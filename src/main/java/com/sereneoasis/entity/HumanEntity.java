package com.sereneoasis.entity;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.sereneoasis.SerenityEntities;
import com.sereneoasis.util.PacketUtils;
import io.papermc.paper.event.entity.EntityMoveEvent;
import io.papermc.paper.util.MCUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorldBorder;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_20_R2.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.*;
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
        /*try {
            if (this.valid && !this.isSpectator() || !this.touchingUnloadedChunk()) {
                super.tick();
            }

            for(int i = 0; i < this.getInventory().getContainerSize(); ++i) {
                net.minecraft.world.item.ItemStack itemstack = this.getInventory().getItem(i);
                if (itemstack.getItem().isComplex()) {
                    Packet<?> packet = ((ComplexItem)itemstack.getItem()).getUpdatePacket(itemstack, this.level(), this);
                    if (packet != null) {
                        this.connection.send(packet);
                    }
                }
            }

            if (this.getHealth() != this.lastSentHealth || this.lastSentFood != this.foodData.getFoodLevel() || this.foodData.getSaturationLevel() == 0.0F != this.lastFoodSaturationZero) {
                this.connection.send(new ClientboundSetHealthPacket(this.getBukkitEntity().getScaledHealth(), this.foodData.getFoodLevel(), this.foodData.getSaturationLevel()));
                this.lastSentHealth = this.getHealth();
                this.lastSentFood = this.foodData.getFoodLevel();
                this.lastFoodSaturationZero = this.foodData.getSaturationLevel() == 0.0F;
            }

            if (this.getHealth() + this.getAbsorptionAmount() != this.lastRecordedHealthAndAbsorption) {
                this.lastRecordedHealthAndAbsorption = this.getHealth() + this.getAbsorptionAmount();
                this.updateScoreForCriteria(ObjectiveCriteria.HEALTH, Mth.ceil(this.lastRecordedHealthAndAbsorption));
            }

            if (this.foodData.getFoodLevel() != this.lastRecordedFoodLevel) {
                this.lastRecordedFoodLevel = this.foodData.getFoodLevel();
                this.updateScoreForCriteria(ObjectiveCriteria.FOOD, Mth.ceil((float)this.lastRecordedFoodLevel));
            }

            if (this.getAirSupply() != this.lastRecordedAirLevel) {
                this.lastRecordedAirLevel = this.getAirSupply();
                this.updateScoreForCriteria(ObjectiveCriteria.AIR, Mth.ceil((float)this.lastRecordedAirLevel));
            }

            if (this.getArmorValue() != this.lastRecordedArmor) {
                this.lastRecordedArmor = this.getArmorValue();
                this.updateScoreForCriteria(ObjectiveCriteria.ARMOR, Mth.ceil((float)this.lastRecordedArmor));
            }

            if (this.totalExperience != this.lastRecordedExperience) {
                this.lastRecordedExperience = this.totalExperience;
                this.updateScoreForCriteria(ObjectiveCriteria.EXPERIENCE, Mth.ceil((float)this.lastRecordedExperience));
            }

            if (this.maxHealthCache != (double)this.getMaxHealth()) {
                this.getBukkitEntity().updateScaledHealth();
            }

            if (this.experienceLevel != this.lastRecordedLevel) {
                this.lastRecordedLevel = this.experienceLevel;
                this.updateScoreForCriteria(ObjectiveCriteria.LEVEL, Mth.ceil((float)this.lastRecordedLevel));
            }

            if (this.totalExperience != this.lastSentExp) {
                this.lastSentExp = this.totalExperience;
                this.connection.send(new ClientboundSetExperiencePacket(this.experienceProgress, this.totalExperience, this.experienceLevel));
            }

            if (this.tickCount % 20 == 0) {
                CriteriaTriggers.LOCATION.trigger(this);
            }

            if (this.oldLevel == -1) {
                this.oldLevel = this.experienceLevel;
            }

            if (this.oldLevel != this.experienceLevel) {
                CraftEventFactory.callPlayerLevelChangeEvent(this.getBukkitEntity(), this.oldLevel, this.experienceLevel);
                this.oldLevel = this.experienceLevel;
            }

            if (this.getBukkitEntity().hasClientWorldBorder()) {
                ((CraftWorldBorder)this.getBukkitEntity().getWorldBorder()).getHandle().tick();
            }

        } catch (Throwable var4) {
            CrashReport crashreport = CrashReport.forThrowable(var4, "Ticking player");
            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Player being ticked");
            this.fillCrashReportCategory(crashreportsystemdetails);
            throw new ReportedException(crashreport);
        }*/
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

    public void livingEntityaiStep() {
       /* if (this.noJumpDelay > 0) {
            --this.noJumpDelay;
        }

        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
        }

        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            --this.lerpSteps;
        } else if (!this.isEffectiveAi()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }

        if (this.lerpHeadSteps > 0) {
            this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
            --this.lerpHeadSteps;
        }

        Vec3 vec3d = this.getDeltaMovement();
        double d0 = vec3d.x;
        double d1 = vec3d.y;
        double d2 = vec3d.z;
        if (Math.abs(vec3d.x) < 0.003) {
            d0 = 0.0;
        }

        if (Math.abs(vec3d.y) < 0.003) {
            d1 = 0.0;
        }

        if (Math.abs(vec3d.z) < 0.003) {
            d2 = 0.0;
        }

        this.setDeltaMovement(d0, d1, d2);
        this.level().getProfiler().push("ai");
        if (this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        } else if (this.isEffectiveAi()) {
            this.level().getProfiler().push("newAi");
            this.serverAiStep();
            this.level().getProfiler().pop();
        }

        this.level().getProfiler().pop();
        this.level().getProfiler().push("jump");
        if (this.jumping && this.isAffectedByFluids()) {
            double d3;
            if (this.isInLava()) {
                d3 = this.getFluidHeight(FluidTags.LAVA);
            } else {
                d3 = this.getFluidHeight(FluidTags.WATER);
            }

            boolean flag = this.isInWater() && d3 > 0.0;
            double d4 = this.getFluidJumpThreshold();
            if (!flag || this.onGround() && !(d3 > d4)) {
                if (this.isInLava() && (!this.onGround() || d3 > d4)) {
                    this.jumpInLiquid(FluidTags.LAVA);
                } else if ((this.onGround() || flag && d3 <= d4) && this.noJumpDelay == 0) {
                    if ((new EntityJumpEvent(this.getBukkitLivingEntity())).callEvent()) {
                        this.jumpFromGround();
                        this.noJumpDelay = 10;
                    } else {
                        this.setJumping(false);
                    }
                }
            } else {
                this.jumpInLiquid(FluidTags.WATER);
            }
        } else {
            this.noJumpDelay = 0;
        }

        this.level().getProfiler().pop();
        this.level().getProfiler().push("travel");
        this.xxa *= 0.98F;
        this.zza *= 0.98F;
        this.updateFallFlying();
        AABB axisalignedbb = this.getBoundingBox();
        Vec3 vec3d1 = new Vec3((double)this.xxa, (double)this.yya, (double)this.zza);
        if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
            this.resetFallDistance();
        }

        label132: {
            LivingEntity entityliving = this.getControllingPassenger();
            if (entityliving instanceof net.minecraft.world.entity.player.Player entityhuman) {
                if (this.isAlive()) {
                    this.travelRidden(entityhuman, vec3d1);
                    break label132;
                }
            }

            this.travel(vec3d1);
        }

        this.level().getProfiler().pop();
        this.level().getProfiler().push("freezing");
        if (!this.level().isClientSide && !this.isDeadOrDying() && !this.freezeLocked) {
            int i = this.getTicksFrozen();
            if (this.isInPowderSnow && this.canFreeze()) {
                this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), i + 1));
            } else {
                this.setTicksFrozen(Math.max(0, i - 2));
            }
        }

        this.removeFrost();
        this.tryAddFrost();
        if (!this.level().isClientSide && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
            this.hurt(this.damageSources().freeze(), 1.0F);
        }

        this.level().getProfiler().pop();
        this.level().getProfiler().push("push");
        if (this.autoSpinAttackTicks > 0) {
            --this.autoSpinAttackTicks;
            this.checkAutoSpinAttack(axisalignedbb, this.getBoundingBox());
        }

        this.pushEntities();
        this.level().getProfiler().pop();
        if (((ServerLevel)this.level()).hasEntityMoveEvent && !(this instanceof net.minecraft.world.entity.player.Player) && (this.xo != this.getX() || this.yo != this.getY() || this.zo != this.getZ() || this.yRotO != this.getYRot() || this.xRotO != this.getXRot())) {
            Location from = new Location(this.level().getWorld(), this.xo, this.yo, this.zo, this.yRotO, this.xRotO);
            Location to = new Location(this.level().getWorld(), this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            EntityMoveEvent event = new EntityMoveEvent(this.getBukkitLivingEntity(), from, to.clone());
            if (!event.callEvent()) {
                this.absMoveTo(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
            } else if (!to.equals(event.getTo())) {
                this.absMoveTo(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch());
            }
        }

        if (!this.level().isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
            this.hurt(this.damageSources().drown(), 1.0F);
        }*/

    }

    public void PlayeraiStep() {
      /*  if (this.jumpTriggerTime > 0) {
            --this.jumpTriggerTime;
        }

        if (this.level().getDifficulty() == Difficulty.PEACEFUL && this.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
            if (this.getHealth() < this.getMaxHealth() && this.tickCount % 20 == 0) {
                this.heal(1.0F, EntityRegainHealthEvent.RegainReason.REGEN);
            }

            if (this.foodData.needsFood() && this.tickCount % 10 == 0) {
                this.foodData.setFoodLevel(this.foodData.getFoodLevel() + 1);
            }
        }

        this.inventory.tick();
        this.oBob = this.bob;
        super.aiStep();
        this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
        float f;
        if (this.onGround() && !this.isDeadOrDying() && !this.isSwimming()) {
            f = Math.min(0.1F, (float)this.getDeltaMovement().horizontalDistance());
        } else {
            f = 0.0F;
        }

        this.bob += (f - this.bob) * 0.4F;
        if (this.getHealth() > 0.0F && !this.isSpectator()) {
            AABB axisalignedbb;
            if (this.isPassenger() && !this.getVehicle().isRemoved()) {
                axisalignedbb = this.getBoundingBox().minmax(this.getVehicle().getBoundingBox()).inflate(1.0, 0.0, 1.0);
            } else {
                axisalignedbb = this.getBoundingBox().inflate(1.0, 0.5, 1.0);
            }

            List<Entity> list = this.level().getEntities(this, axisalignedbb);
            List<Entity> list1 = Lists.newArrayList();
            Iterator iterator = list.iterator();

            while(iterator.hasNext()) {
                Entity entity = (Entity)iterator.next();
                if (entity.getType() == EntityType.EXPERIENCE_ORB) {
                    list1.add(entity);
                } else if (!entity.isRemoved()) {
                    this.touch(entity);
                }
            }

            if (!list1.isEmpty()) {
                this.touch((Entity) Util.getRandom(list1, this.random));
            }
        }

        this.playShoulderEntityAmbientSound(this.getShoulderEntityLeft());
        this.playShoulderEntityAmbientSound(this.getShoulderEntityRight());
        if ((!this.level().isClientSide && (this.fallDistance > 0.5F || this.isInWater()) || this.abilities.flying || this.isSleeping() || this.isInPowderSnow) && !this.level().paperConfig().entities.behavior.parrotsAreUnaffectedByPlayerMovement) {
            this.removeEntitiesOnShoulder();
        }*/

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
            this.livingEntityaiStep();
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
        super.tick();


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

                this.livingEntityTick();

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
