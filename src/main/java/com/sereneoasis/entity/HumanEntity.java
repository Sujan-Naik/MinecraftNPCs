package com.sereneoasis.entity;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.sereneoasis.entity.AI.control.JumpControl;
import com.sereneoasis.entity.AI.control.LookControl;
import com.sereneoasis.entity.AI.control.MoveControl;
import com.sereneoasis.entity.AI.navigation.GroundPathNavigation;
import com.sereneoasis.entity.AI.navigation.PathNavigation;
import io.papermc.paper.event.entity.EntityMoveEvent;
import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R2.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class HumanEntity extends ServerPlayer {

    private LivingEntity owner;

    private PathNavigation navigation;

    private MoveControl moveControl;

    private LookControl lookControl;
    private JumpControl jumpControl;
    private int noJumpDelay;
    private final Inventory inventory = new Inventory(this);
    private BlockState feetBlockState;

    private int remainingFireTicks;


    private BlockPos blockPosition;

    private final Set<TagKey<Fluid>> fluidOnEyes;

    private BlockPos lastPos;
    private LivingEntity lastHurtMob;

    private net.minecraft.world.item.ItemStack lastItemInMainHand;

    private final ItemCooldowns cooldowns;

    private int containerUpdateDelay;

    @Nullable
    private Vec3 levitationStartPos;

    private int levitationStartTime;


    private final PlayerAdvancements advancements;

    public HumanEntity(MinecraftServer server, ServerLevel world, GameProfile profile, ClientInformation clientOptions) {
        super(server, world, profile, clientOptions);

        this.moveControl = new MoveControl(this);
        this.jumpControl = new JumpControl(this);
        this.lookControl = new LookControl(this);

        this.navigation = new GroundPathNavigation(this, world);

        this.feetBlockState = null;
        this.remainingFireTicks = -this.getFireImmuneTicks();
        this.blockPosition = BlockPos.ZERO;
        this.fluidOnEyes = new HashSet();

        this.lastItemInMainHand = net.minecraft.world.item.ItemStack.EMPTY;

        this.cooldowns = this.createItemCooldowns();

        this.advancements = server.getPlayerList().getPlayerAdvancements(this);

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

    public void livingEntityAiStep() {
        if (this.noJumpDelay > 0) {
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
                    if ( !(new EntityJumpEvent(this.getBukkitLivingEntity())).isCancelled()) {
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

            Bukkit.broadcastMessage("travelling " + vec3d1.length() + " distance");
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
            if (event.isCancelled()) {
                this.absMoveTo(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
            } else if (!to.equals(event.getTo())) {
                this.absMoveTo(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch());
            }
        }

        if (!this.level().isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
            this.hurt(this.damageSources().drown(), 1.0F);
        }

    }

    private void travelRidden(net.minecraft.world.entity.player.Player controllingPlayer, Vec3 movementInput) {
        Vec3 vec3d1 = this.getRiddenInput(controllingPlayer, movementInput);
        this.tickRidden(controllingPlayer, vec3d1);
        if (this.isControlledByLocalInstance()) {
            this.setSpeed(this.getRiddenSpeed(controllingPlayer));
            this.travel(vec3d1);
        } else {
            this.calculateEntityAnimation(false);
            this.setDeltaMovement(Vec3.ZERO);
            this.tryCheckInsideBlocks();
        }

    }

    private void updateFallFlying() {
        boolean flag = this.getSharedFlag(7);
        if (flag && !this.onGround() && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
            net.minecraft.world.item.ItemStack itemstack = this.getItemBySlot(EquipmentSlot.CHEST);
            if (itemstack.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(itemstack)) {
                flag = true;
                int i = this.fallFlyTicks + 1;
                if (!this.level().isClientSide && i % 10 == 0) {
                    int j = i / 10;
                    if (j % 2 == 0) {
                        itemstack.hurtAndBreak(1, this, (entityliving) -> {
                            entityliving.broadcastBreakEvent(EquipmentSlot.CHEST);
                        });
                    }

                    this.gameEvent(GameEvent.ELYTRA_GLIDE);
                }
            } else {
                flag = false;
            }
        } else {
            flag = false;
        }

        if (!this.level().isClientSide && flag != this.getSharedFlag(7) && !CraftEventFactory.callToggleGlideEvent(this, flag).isCancelled()) {
            this.setSharedFlag(7, flag);
        }

    }

    public void playerAiStep() {
        if (this.jumpTriggerTime > 0) {
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
        // super.aiStep();
        livingEntityAiStep();
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
        if ((!this.level().isClientSide && (this.fallDistance > 0.5F || this.isInWater()) || this.getAbilities().flying || this.isSleeping() || this.isInPowderSnow) && !this.level().paperConfig().entities.behavior.parrotsAreUnaffectedByPlayerMovement) {
            this.removeEntitiesOnShoulder();
        }

    }

    private void playShoulderEntityAmbientSound(@Nullable CompoundTag entityNbt) {
        if (entityNbt != null && (!entityNbt.contains("Silent") || !entityNbt.getBoolean("Silent")) && this.level().random.nextInt(200) == 0) {
            String s = entityNbt.getString("id");
            EntityType.byString(s).filter((entitytypes) -> {
                return entitytypes == EntityType.PARROT;
            }).ifPresent((entitytypes) -> {
                if (!Parrot.imitateNearbyMobs(this.level(), this)) {
                    this.level().playSound((net.minecraft.world.entity.player.Player)null, this.getX(), this.getY(), this.getZ(), Parrot.getAmbient(this.level(), this.level().random), this.getSoundSource(), 1.0F, Parrot.getPitch(this.level().random));
                }

            });
        }

    }


    private void touch(Entity entity) {
        entity.playerTouch(this);
    }

    public void livingEntityTick(){
        // super.tick();
        livingEntityBaseTick();
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
            this.playerAiStep();
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

    public void entityBaseTick() {


        this.level().getProfiler().push("entityBaseTick");
        if (this.firstTick && this instanceof NeutralMob neutralMob) {
            neutralMob.tickInitialPersistentAnger(this.level());
        }

        this.feetBlockState = null;
        if (this.isPassenger() && this.getVehicle().isRemoved()) {
            this.stopRiding();
        }

        if (this.boardingCooldown > 0) {
            --this.boardingCooldown;
        }

        this.walkDistO = this.walkDist;
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        if (this instanceof ServerPlayer) {
            this.handleNetherPortal();
        }

        if (this.canSpawnSprintParticle()) {
            this.spawnSprintParticle();
        }

        this.wasInPowderSnow = this.isInPowderSnow;
        this.isInPowderSnow = false;
        this.updateInWaterStateAndDoFluidPushing();
        this.updateFluidOnEyes();
        this.updateSwimming();
        if (this.level().isClientSide) {
            this.clearFire();
        } else if (this.remainingFireTicks > 0) {
            if (this.fireImmune()) {
                this.setRemainingFireTicks(this.remainingFireTicks - 4);
                if (this.remainingFireTicks < 0) {
                    this.clearFire();
                }
            } else {
                if (this.remainingFireTicks % 20 == 0 && !this.isInLava()) {
                    this.hurt(this.damageSources().onFire(), 1.0F);
                }

                this.setRemainingFireTicks(this.remainingFireTicks - 1);
            }

            if (this.getTicksFrozen() > 0 && !this.freezeLocked) {
                this.setTicksFrozen(0);
                this.level().levelEvent((net.minecraft.world.entity.player.Player)null, 1009, this.blockPosition, 1);
            }
        }

        if (this.isInLava()) {
            this.lavaHurt();
            this.fallDistance *= 0.5F;
        } else {
            this.lastLavaContact = null;
        }

        this.checkBelowWorld();
        if (!this.level().isClientSide) {
            this.setSharedFlagOnFire(this.remainingFireTicks > 0);
        }

        this.firstTick = false;
        this.level().getProfiler().pop();
    }

    private void updateFluidOnEyes() {
        this.wasEyeInWater = this.isEyeInFluid(FluidTags.WATER);
        this.fluidOnEyes.clear();
        double d0 = this.getEyeY() - 0.1111111119389534;
        Entity entity = this.getVehicle();
        if (entity instanceof Boat entityboat) {
            if (!entityboat.isUnderWater() && entityboat.getBoundingBox().maxY >= d0 && entityboat.getBoundingBox().minY <= d0) {
                return;
            }
        }

        BlockPos blockposition = BlockPos.containing(this.getX(), d0, this.getZ());
        FluidState fluid = this.level().getFluidState(blockposition);
        double d1 = (double)((float)blockposition.getY() + fluid.getHeight(this.level(), blockposition));
        if (d1 > d0) {
            Stream stream = fluid.getTags();
            Set set = this.fluidOnEyes;
            java.util.Objects.requireNonNull(this.fluidOnEyes);
            java.util.Objects.requireNonNull(set);
            stream.forEach(set::add);
        }

    }

    public void livingEntityBaseTick() {
        this.oAttackAnim = this.attackAnim;
        if (this.firstTick) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
        }

        if (this.canSpawnSoulSpeedParticle()) {
            this.spawnSoulSpeedParticle();
        }

        //super.baseTick();
        entityBaseTick();
        this.level().getProfiler().push("livingEntityBaseTick");
        if (this.fireImmune() || this.level().isClientSide) {
            this.clearFire();
        }

        if (this.isAlive()) {
            boolean flag = this instanceof net.minecraft.world.entity.player.Player;
            if (!this.level().isClientSide) {
                if (this.isInWall()) {
                    this.hurt(this.damageSources().inWall(), 1.0F);
                } else if (flag && !this.level().getWorldBorder().isWithinBounds(this.getBoundingBox())) {
                    double d0 = this.level().getWorldBorder().getDistanceToBorder(this) + this.level().getWorldBorder().getDamageSafeZone();
                    if (d0 < 0.0) {
                        double d1 = this.level().getWorldBorder().getDamagePerBlock();
                        if (d1 > 0.0) {
                            this.hurt(this.damageSources().outOfBorder(), (float)Math.max(1, Mth.floor(-d0 * d1)));
                        }
                    }
                }
            }

            if (this.isEyeInFluid(FluidTags.WATER) && !this.level().getBlockState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
                boolean flag1 = !this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && (!flag || !((net.minecraft.world.entity.player.Player)this).getAbilities().invulnerable);
                if (flag1) {
                    this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
                    if (this.getAirSupply() == -20) {
                        this.setAirSupply(0);
                        Vec3 vec3d = this.getDeltaMovement();

                        for(int i = 0; i < 8; ++i) {
                            double d2 = this.random.nextDouble() - this.random.nextDouble();
                            double d3 = this.random.nextDouble() - this.random.nextDouble();
                            double d4 = this.random.nextDouble() - this.random.nextDouble();
                            this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + d2, this.getY() + d3, this.getZ() + d4, vec3d.x, vec3d.y, vec3d.z);
                        }

                        this.hurt(this.damageSources().drown(), 2.0F);
                    }
                }

                if (!this.level().isClientSide && this.isPassenger() && this.getVehicle() != null && this.getVehicle().dismountsUnderwater()) {
                    this.stopRiding();
                }
            } else if (this.getAirSupply() < this.getMaxAirSupply()) {
                this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
            }

            if (!this.level().isClientSide) {
                BlockPos blockposition = this.blockPosition();
                if (!Objects.equal(this.lastPos, blockposition)) {
                    this.lastPos = blockposition;
                    this.onChangedBlock(blockposition);
                }
            }
        }

        if (this.isAlive() && (this.isInWaterRainOrBubble() || this.isInPowderSnow)) {
            this.extinguishFire();
        }

        if (this.hurtTime > 0) {
            --this.hurtTime;
        }

        if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
            --this.invulnerableTime;
        }

        if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) {
            this.tickDeath();
        }

        if (this.lastHurtByPlayerTime > 0) {
            --this.lastHurtByPlayerTime;
        } else {
            this.lastHurtByPlayer = null;
        }

        if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
            this.lastHurtMob = null;
        }

        if (this.lastHurtByMob != null) {
            if (!this.lastHurtByMob.isAlive()) {
                this.setLastHurtByMob((LivingEntity)null);
            } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
                this.setLastHurtByMob((LivingEntity)null);
            }
        }

        this.tickEffects();
        this.animStepO = this.animStep;
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.level().getProfiler().pop();
    }

    private void setPosToBed(BlockPos pos) {
        this.setPos((double)pos.getX() + 0.5, (double)pos.getY() + 0.6875, (double)pos.getZ() + 0.5);
    }

    public void playerTick(){
        this.noPhysics = this.isSpectator();
        if (this.isSpectator()) {
            this.setOnGround(false);
        }

        if (this.takeXpDelay > 0) {
            --this.takeXpDelay;
        }

        if (this.isSleeping()) {
            ++this.sleepCounter;
            if (this.sleepCounter == 100 && (new PlayerDeepSleepEvent((org.bukkit.entity.Player)this.getBukkitEntity())).isCancelled()) {
                this.sleepCounter = Integer.MIN_VALUE;
            }

            if (this.sleepCounter > 100) {
                this.sleepCounter = 100;
            }

            if (!this.level().isClientSide && this.level().isDay()) {
                this.stopSleepInBed(false, true);
            }
        } else if (this.sleepCounter > 0) {
            ++this.sleepCounter;
            if (this.sleepCounter >= 110) {
                this.sleepCounter = 0;
            }
        }

        this.updateIsUnderwater();
        // super.tick();
        livingEntityTick();
        if (!this.level().isClientSide && this.containerMenu != null && !this.containerMenu.stillValid(this)) {
          //  this.closeContainer(Reason.CANT_USE);
            this.closeContainer();
            this.containerMenu = this.inventoryMenu;
        }

        this.moveCloak();
        if (!this.level().isClientSide) {
            this.foodData.tick(this);
            this.awardStat(Stats.PLAY_TIME);
            this.awardStat(Stats.TOTAL_WORLD_TIME);
            if (this.isAlive()) {
                this.awardStat(Stats.TIME_SINCE_DEATH);
            }

            if (this.isDiscrete()) {
                this.awardStat(Stats.CROUCH_TIME);
            }

            if (!this.isSleeping()) {
                this.awardStat(Stats.TIME_SINCE_REST);
            }
        }

        int i = 29999999;
        double d0 = Mth.clamp(this.getX(), -2.9999999E7, 2.9999999E7);
        double d1 = Mth.clamp(this.getZ(), -2.9999999E7, 2.9999999E7);
        if (d0 != this.getX() || d1 != this.getZ()) {
            this.setPos(d0, this.getY(), d1);
        }

        ++this.attackStrengthTicker;
        net.minecraft.world.item.ItemStack itemstack = this.getMainHandItem();
        if (!net.minecraft.world.item.ItemStack.matches(this.lastItemInMainHand, itemstack)) {
            if (!net.minecraft.world.item.ItemStack.isSameItem(this.lastItemInMainHand, itemstack)) {
                this.resetAttackStrengthTicker();
            }

            this.lastItemInMainHand = itemstack.copy();
        }

        this.turtleHelmetTick();
        this.cooldowns.tick();
        this.updatePlayerPose();
    }



    private void turtleHelmetTick() {
        net.minecraft.world.item.ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
        if (itemstack.is(Items.TURTLE_HELMET) && !this.isEyeInFluid(FluidTags.WATER)) {
            this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true), EntityPotionEffectEvent.Cause.TURTLE_HELMET);
        }

    }

    private void moveCloak() {
        this.xCloakO = this.xCloak;
        this.yCloakO = this.yCloak;
        this.zCloakO = this.zCloak;
        double d0 = this.getX() - this.xCloak;
        double d1 = this.getY() - this.yCloak;
        double d2 = this.getZ() - this.zCloak;
        double d3 = 10.0;
        if (d0 > 10.0) {
            this.xCloak = this.getX();
            this.xCloakO = this.xCloak;
        }

        if (d2 > 10.0) {
            this.zCloak = this.getZ();
            this.zCloakO = this.zCloak;
        }

        if (d1 > 10.0) {
            this.yCloak = this.getY();
            this.yCloakO = this.yCloak;
        }

        if (d0 < -10.0) {
            this.xCloak = this.getX();
            this.xCloakO = this.xCloak;
        }

        if (d2 < -10.0) {
            this.zCloak = this.getZ();
            this.zCloakO = this.zCloak;
        }

        if (d1 < -10.0) {
            this.yCloak = this.getY();
            this.yCloakO = this.yCloak;
        }

        this.xCloak += d0 * 0.25;
        this.zCloak += d2 * 0.25;
        this.yCloak += d1 * 0.25;
    }

    protected void serverAiStep() {
        super.serverAiStep();
        this.updateSwingTime();
        this.yHeadRot = this.getYRot();
    }

    @Override
    public void doTick(){
        serverPlayerDoTick();
    }

    @Override
    public void tick() {
        // tick() calls serverPlayerTick() and doTick()
        // doTick() calls serverPlayerDoTick()
        // serverPlayerDoTick() calls playerTick()
        // playerTick() calls livingEntityTick()
        // livingEntityTick() calls livingEntityBaseTick() and playerAiStep()
        // livingEntityBaseTick() calls entityBaseTick()
        // playerAiStep() calls livingEntityAiStep()
        // livingEntityAiStep() calls serverAiStep()

        serverPlayerTick();
        doTick();

        if (owner != null) {
            if (this.distanceToSqr(this.owner) <= 256.0) {


                this.lookControl.setLookAt(this.owner, 10.0F, (float)this.getMaxHeadXRot());
                this.navigation.moveTo(this.owner, 10);

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

    public void serverPlayerTick() {
        if (this.joining) {
            this.joining = false;
        }

        this.gameMode.tick();
        this.wardenSpawnTracker.tick();
        --this.spawnInvulnerableTime;
        if (this.invulnerableTime > 0) {
            --this.invulnerableTime;
        }

        if (--this.containerUpdateDelay <= 0) {
            this.containerMenu.broadcastChanges();
            this.containerUpdateDelay = this.level().paperConfig().tickRates.containerUpdate;
        }

        if (!this.level().isClientSide && this.containerMenu != this.inventoryMenu && (this.isImmobile() || !this.containerMenu.stillValid(this))) {
            //this.closeContainer(Reason.CANT_USE);
            this.closeContainer();
            this.containerMenu = this.inventoryMenu;
        }

        Entity entity = this.getCamera();
        if (entity != this) {
            if (entity.isAlive()) {
                this.absMoveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                this.serverLevel().getChunkSource().move(this);
                if (this.wantsToStopRiding()) {
                    this.setCamera(this);
                }
            } else {
                this.setCamera(this);
            }
        }

        CriteriaTriggers.TICK.trigger(this);
        if (this.levitationStartPos != null) {
            CriteriaTriggers.LEVITATION.trigger(this, this.levitationStartPos, this.tickCount - this.levitationStartTime);
        }

        this.trackStartFallingPosition();
        this.trackEnteredOrExitedLavaOnVehicle();
        this.advancements.flushDirty(this);
    }

    public void serverPlayerDoTick() {
        //try {
            if (this.valid && !this.isSpectator() || !this.touchingUnloadedChunk()) {
                // super.tick();
                playerTick();
            }

//            for(int i = 0; i < this.getInventory().getContainerSize(); ++i) {
//                net.minecraft.world.item.ItemStack itemstack = this.getInventory().getItem(i);
//                if (itemstack.getItem().isComplex()) {
//                    Packet<?> packet = ((ComplexItem)itemstack.getItem()).getUpdatePacket(itemstack, this.level(), this);
//                    if (packet != null) {
//                        this.connection.send(packet);
//                    }
//                }
//            }
//
//            if (this.getHealth() != this.lastSentHealth || this.lastSentFood != this.foodData.getFoodLevel() || this.foodData.getSaturationLevel() == 0.0F != this.lastFoodSaturationZero) {
//                this.connection.send(new ClientboundSetHealthPacket(this.getBukkitEntity().getScaledHealth(), this.foodData.getFoodLevel(), this.foodData.getSaturationLevel()));
//                this.lastSentHealth = this.getHealth();
//                this.lastSentFood = this.foodData.getFoodLevel();
//                this.lastFoodSaturationZero = this.foodData.getSaturationLevel() == 0.0F;
//            }
//
//            if (this.getHealth() + this.getAbsorptionAmount() != this.lastRecordedHealthAndAbsorption) {
//                this.lastRecordedHealthAndAbsorption = this.getHealth() + this.getAbsorptionAmount();
//                this.updateScoreForCriteria(ObjectiveCriteria.HEALTH, Mth.ceil(this.lastRecordedHealthAndAbsorption));
//            }
//
//            if (this.foodData.getFoodLevel() != this.lastRecordedFoodLevel) {
//                this.lastRecordedFoodLevel = this.foodData.getFoodLevel();
//                this.updateScoreForCriteria(ObjectiveCriteria.FOOD, Mth.ceil((float)this.lastRecordedFoodLevel));
//            }
//
//            if (this.getAirSupply() != this.lastRecordedAirLevel) {
//                this.lastRecordedAirLevel = this.getAirSupply();
//                this.updateScoreForCriteria(ObjectiveCriteria.AIR, Mth.ceil((float)this.lastRecordedAirLevel));
//            }
//
//            if (this.getArmorValue() != this.lastRecordedArmor) {
//                this.lastRecordedArmor = this.getArmorValue();
//                this.updateScoreForCriteria(ObjectiveCriteria.ARMOR, Mth.ceil((float)this.lastRecordedArmor));
//            }
//
//            if (this.totalExperience != this.lastRecordedExperience) {
//                this.lastRecordedExperience = this.totalExperience;
//                this.updateScoreForCriteria(ObjectiveCriteria.EXPERIENCE, Mth.ceil((float)this.lastRecordedExperience));
//            }
//
//            if (this.maxHealthCache != (double)this.getMaxHealth()) {
//                this.getBukkitEntity().updateScaledHealth();
//            }
//
//            if (this.experienceLevel != this.lastRecordedLevel) {
//                this.lastRecordedLevel = this.experienceLevel;
//                this.updateScoreForCriteria(ObjectiveCriteria.LEVEL, Mth.ceil((float)this.lastRecordedLevel));
//            }
//
//            if (this.totalExperience != this.lastSentExp) {
//                this.lastSentExp = this.totalExperience;
//                this.connection.send(new ClientboundSetExperiencePacket(this.experienceProgress, this.totalExperience, this.experienceLevel));
//            }
//
//            if (this.tickCount % 20 == 0) {
//                CriteriaTriggers.LOCATION.trigger(this);
//            }
//
//            if (this.oldLevel == -1) {
//                this.oldLevel = this.experienceLevel;
//            }
//
//            if (this.oldLevel != this.experienceLevel) {
//                CraftEventFactory.callPlayerLevelChangeEvent(this.getBukkitEntity(), this.oldLevel, this.experienceLevel);
//                this.oldLevel = this.experienceLevel;
//            }
//
//            if (this.getBukkitEntity().hasClientWorldBorder()) {
//                ((CraftWorldBorder)this.getBukkitEntity().getWorldBorder()).getHandle().tick();
//            }
//
//        } catch (Throwable var4) {
//            CrashReport crashreport = CrashReport.forThrowable(var4, "Ticking player");
//            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Player being ticked");
//            this.fillCrashReportCategory(crashreportsystemdetails);
//            throw new ReportedException(crashreport);
        //}
    }
}
