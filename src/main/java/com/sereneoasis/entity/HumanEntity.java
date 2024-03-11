package com.sereneoasis.entity;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import com.sereneoasis.entity.AI.control.BodyRotationControl;
import com.sereneoasis.entity.AI.control.JumpControl;
import com.sereneoasis.entity.AI.control.LookControl;
import com.sereneoasis.entity.AI.control.MoveControl;
import com.sereneoasis.entity.AI.goal.GoalSelector;
import com.sereneoasis.entity.AI.goal.MasterGoalSelector;
import com.sereneoasis.entity.AI.goal.complex.combat.KillTargetEntity;
import com.sereneoasis.entity.AI.goal.complex.interaction.GatherBlocks;
import com.sereneoasis.entity.AI.goal.complex.movement.RandomExploration;
import com.sereneoasis.entity.AI.inventory.FoodData;
import com.sereneoasis.entity.AI.inventory.InventoryTracker;
import com.sereneoasis.entity.AI.navigation.GroundPathNavigation;
import com.sereneoasis.entity.AI.navigation.PathNavigation;
import com.sereneoasis.entity.AI.target.TargetSelector;
import com.sereneoasis.util.Vec3Utils;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.event.entity.EntityMoveEvent;
import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R2.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftLocation;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftVector;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class HumanEntity extends ServerPlayer {

    private LivingEntity owner;

    private PathNavigation navigation;

    private MoveControl moveControl;

    public LookControl lookControl;
    private JumpControl jumpControl;

    private BodyRotationControl bodyRotationControl;
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

    private MasterGoalSelector masterGoalSelector;

    private TargetSelector targetSelector;

    private InventoryTracker inventoryTracker;

    public FoodData foodData = new FoodData(this);


    public HumanEntity(MinecraftServer server, ServerLevel world, GameProfile profile, ClientInformation clientOptions) {
        super(server, world, profile, clientOptions);


        this.moveControl = new MoveControl(this);
        this.jumpControl = new JumpControl(this);
        this.lookControl = new LookControl(this);
        this.bodyRotationControl = new BodyRotationControl(this);

        this.navigation = new GroundPathNavigation(this, world);
        this.masterGoalSelector = new MasterGoalSelector();
        this.targetSelector = new TargetSelector(this);
        this.inventoryTracker = new InventoryTracker(inventory, this);

        this.feetBlockState = null;
        this.remainingFireTicks = -this.getFireImmuneTicks();
        this.blockPosition = BlockPos.ZERO;
        this.fluidOnEyes = new HashSet();

        this.lastItemInMainHand = net.minecraft.world.item.ItemStack.EMPTY;

        this.cooldowns = this.createItemCooldowns();

        this.advancements = server.getPlayerList().getPlayerAdvancements(this);
        this.setItemSlot(EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.fromBukkitCopy(new org.bukkit.inventory.ItemStack(Material.IRON_HELMET)));
        this.setItemSlot(EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.fromBukkitCopy(new org.bukkit.inventory.ItemStack(Material.IRON_CHESTPLATE)));
        this.setItemSlot(EquipmentSlot.LEGS, net.minecraft.world.item.ItemStack.fromBukkitCopy(new org.bukkit.inventory.ItemStack(Material.IRON_LEGGINGS)));
        this.setItemSlot(EquipmentSlot.FEET, net.minecraft.world.item.ItemStack.fromBukkitCopy(new org.bukkit.inventory.ItemStack(Material.IRON_BOOTS)));

        // Player player = this.getBukkitEntity().getPlayer();

//         player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
//         player.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));



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

    public int attackTime = -1;
    public int attackIntervalMin = 20;

    public int timeSinceBowDraw = -1;

    public int getMaxHeadXRot() {
        return 40;
    }

    public int getMaxHeadYRot() {
        return 75;
    }

    public int getHeadRotSpeed() {
        return 10;
    }

    protected float tickHeadTurn(float bodyRotation, float headRotation) {
        this.bodyRotationControl.clientTick();
        return headRotation;
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
        }
        //} else if (this.isEffectiveAi()) {
            this.level().getProfiler().push("newAi");
            this.serverAiStep();
            this.level().getProfiler().pop();
        //}

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

            // Bukkit.broadcastMessage("travelling " + vec3d1.length() + " distance");
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

    @Override
    public void travel(Vec3 movementInput) {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        double d3;
        if (this.isSwimming() && !this.isPassenger()) {
            d3 = this.getLookAngle().y;
            double d4 = d3 < -0.2 ? 0.085 : 0.06;
            if (d3 <= 0.0 || this.jumping || !this.level().getBlockState(BlockPos.containing(this.getX(), this.getY() + 1.0 - 0.1, this.getZ())).getFluidState().isEmpty()) {
                Vec3 vec3d1 = this.getDeltaMovement();
                this.setDeltaMovement(vec3d1.add(0.0, (d3 - vec3d1.y) * d4, 0.0));
            }
        }

        if (this.getAbilities().flying && !this.isPassenger()) {
            d3 = this.getDeltaMovement().y;
            super.travel(movementInput);
            Vec3 vec3d2 = this.getDeltaMovement();
            this.setDeltaMovement(vec3d2.x, d3 * 0.6, vec3d2.z);
            this.resetFallDistance();
            if (this.getSharedFlag(7) && !CraftEventFactory.callToggleGlideEvent(this, false).isCancelled()) {
                this.setSharedFlag(7, false);
            }
        } else {
            super.travel(movementInput);
        }

        this.checkMovementStatistics(this.getX() - d0, this.getY() - d1, this.getZ() - d2);
    }

    @Override
    public void checkMovementStatistics(double dx, double dy, double dz) {
        if (!this.isPassenger()) {
            int i;
            if (this.isSwimming()) {
                i = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);
                if (i > 0) {
                    this.awardStat(Stats.SWIM_ONE_CM, i);
                    this.causeFoodExhaustion(this.level().spigotConfig.swimMultiplier * (float)i * 0.01F, EntityExhaustionEvent.ExhaustionReason.SWIM);
                }
            } else if (this.isEyeInFluid(FluidTags.WATER)) {
                i = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);
                if (i > 0) {
                    this.awardStat(Stats.WALK_UNDER_WATER_ONE_CM, i);
                    this.causeFoodExhaustion(this.level().spigotConfig.swimMultiplier * (float)i * 0.01F, EntityExhaustionEvent.ExhaustionReason.WALK_UNDERWATER);
                }
            } else if (this.isInWater()) {
                i = Math.round((float)Math.sqrt(dx * dx + dz * dz) * 100.0F);
                if (i > 0) {
                    this.awardStat(Stats.WALK_ON_WATER_ONE_CM, i);
                    this.causeFoodExhaustion(this.level().spigotConfig.swimMultiplier * (float)i * 0.01F, EntityExhaustionEvent.ExhaustionReason.WALK_ON_WATER);
                }
            } else if (this.onClimbable()) {
                if (dy > 0.0) {
                    this.awardStat(Stats.CLIMB_ONE_CM, (int)Math.round(dy * 100.0));
                }
            } else if (this.onGround()) {
                i = Math.round((float)Math.sqrt(dx * dx + dz * dz) * 100.0F);
                if (i > 0) {
                    if (this.isSprinting()) {
                        this.awardStat(Stats.SPRINT_ONE_CM, i);
                        this.causeFoodExhaustion(this.level().spigotConfig.sprintMultiplier * (float)i * 0.01F, EntityExhaustionEvent.ExhaustionReason.SPRINT);
                    } else if (this.isCrouching()) {
                        this.awardStat(Stats.CROUCH_ONE_CM, i);
                        this.causeFoodExhaustion(this.level().spigotConfig.otherMultiplier * (float)i * 0.01F, EntityExhaustionEvent.ExhaustionReason.CROUCH);
                    } else {
                        this.awardStat(Stats.WALK_ONE_CM, i);
                        this.causeFoodExhaustion(this.level().spigotConfig.otherMultiplier * (float)i * 0.01F, EntityExhaustionEvent.ExhaustionReason.WALK);
                    }
                }
            } else if (this.isFallFlying()) {
                i = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);
                this.awardStat(Stats.AVIATE_ONE_CM, i);
            } else {
                i = Math.round((float)Math.sqrt(dx * dx + dz * dz) * 100.0F);
                if (i > 25) {
                    this.awardStat(Stats.FLY_ONE_CM, i);
                }
            }
        }

    }

    @Override
    public void causeFoodExhaustion(float f, EntityExhaustionEvent.ExhaustionReason reason) {
        if (!this.getAbilities().invulnerable && !this.level().isClientSide) {
            EntityExhaustionEvent event = CraftEventFactory.callPlayerExhaustionEvent(this, reason, f);
            if (!event.isCancelled()) {
                this.foodData.addExhaustion(event.getExhaustion());
            }
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

    public void mobServerAiStep(){
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
    }

    protected void serverAiStep() {
        //super.serverAiStep();
        this.mobServerAiStep();
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



//        if (!masterGoalSelector.doingGoal("break wood")) {
//            masterGoalSelector.addMasterGoal(new GatherBlocks("break wood", this, Blocks.OAK_WOOD, 1));
//        }

        if (! masterGoalSelector.doingGoal("kill hostile entity")) {
            if (targetSelector.retrieveTopHostile() instanceof LivingEntity hostile &&  (!Vec3Utils.isObstructed(this.getPosition(0), hostile.getPosition(0), this.level()))){
                masterGoalSelector.addMasterGoal(new KillTargetEntity("kill hostile entity", this, hostile));
            }
            else {
                if (!masterGoalSelector.doingGoal("roam")){
                    masterGoalSelector.addMasterGoal(new RandomExploration("roam", this, null));
                }
                if (! inventoryTracker.hasEnoughFood()){
                    if (! masterGoalSelector.doingGoal("kill food entity")) {
                        if (targetSelector.retrieveTopPeaceful() instanceof LivingEntity peaceful){
                            masterGoalSelector.addMasterGoal(new KillTargetEntity("kill food entity", this, peaceful));
                        }
                    }
                } else if (inventoryTracker.hasFood()){
                    this.eat(this.level(), inventoryTracker.getMostAppropriateFood());
                }
            }
        }


        masterGoalSelector.tick();
        targetSelector.tick();
        inventoryTracker.tick();

//        if (owner != null) {
//            if (this.distanceToSqr(this.owner) <= 144.0) {
//                //this.navigation.moveTo(this.owner, 10);
//            }
//            else {
//                if (! masterGoalSelector.hasGoal()) {
//                    masterGoalSelector.addMasterGoal(new KillTargetEntity("kill", this));
//
//                }
//            }
//        }


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

    private void addEatEffect(ItemStack stack, Level world, LivingEntity targetEntity) {
        net.minecraft.world.item.Item item = stack.getItem();
        if (item.isEdible()) {
            List<Pair<MobEffectInstance, Float>> list = item.getFoodProperties().getEffects();
            Iterator iterator = list.iterator();

            while(iterator.hasNext()) {
                Pair<MobEffectInstance, Float> pair = (Pair)iterator.next();
                if (!world.isClientSide && pair.getFirst() != null && world.random.nextFloat() < (Float)pair.getSecond()) {
                    targetEntity.addEffect(new MobEffectInstance((MobEffectInstance)pair.getFirst()), EntityPotionEffectEvent.Cause.FOOD);
                }
            }
        }

    }

    public ItemStack livingEntityEat(Level world, ItemStack stack) {
        if (stack.isEdible()) {
            world.playSound((net.minecraft.world.entity.player.Player)null, this.getX(), this.getY(), this.getZ(), this.getEatingSound(stack), SoundSource.NEUTRAL, 1.0F, 1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.4F);
            this.addEatEffect(stack, world, this);
            if (!(this instanceof net.minecraft.world.entity.player.Player) || !((net.minecraft.world.entity.player.Player)this).getAbilities().instabuild) {
                stack.shrink(1);
            }

            this.gameEvent(GameEvent.EAT);
        }

        return stack;
    }

    public ItemStack playerEat(Level world, ItemStack stack) {
        this.foodData.eat(stack.getItem(), stack);
        this.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        world.playSound((net.minecraft.world.entity.player.Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        if (this instanceof ServerPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)this, stack);
        }

        return livingEntityEat(world, stack);
    }

    public TargetSelector getTargetSelector() {
        return targetSelector;
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

    public boolean canPerformAttack(LivingEntity target) {
        if (this.canAttack(target) && this.distanceToSqr(target) < 9 && this.lookControl.isLookingAtTarget()) {
            return true;
        }
        return false;
    }

    public void checkAndPerformAttack(LivingEntity target) {
        if (canPerformAttack(target)) {
            performAttack(target);
        }
    }

    public void performAttack(LivingEntity target){
        this.resetAttackStrengthTicker();
        this.swing(InteractionHand.MAIN_HAND);
        this.lookControl.setLookAt(target);
         this.attack(target);

        //this.indicateDamage();
    }

    public boolean livingEntityHurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (this.level().isClientSide) {
            return false;
        } else if (!this.isRemoved() && !this.dead && !(this.getHealth() <= 0.0F)) {
            if (source.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                return false;
            } else {
                if (this.isSleeping() && !this.level().isClientSide) {
                    this.stopSleeping();
                }

                this.noActionTime = 0;
                float f1 = amount;
                boolean flag = amount > 0.0F && this.isDamageSourceBlocked(source);
                float f2 = 0.0F;
                if (source.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                    amount *= 5.0F;
                }

                this.walkAnimation.setSpeed(1.5F);
                boolean flag1 = true;
                if ((float)this.invulnerableTime > (float)this.invulnerableDuration / 2.0F && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
                    if (amount <= this.lastHurt) {
                        return false;
                    }

                    if (!this.damageEntity0(source, amount - this.lastHurt)) {
                        return false;
                    }

                    this.lastHurt = amount;
                    flag1 = false;
                } else {
                    if (!this.damageEntity0(source, amount)) {
                        return false;
                    }

                    this.lastHurt = amount;
                    this.invulnerableTime = this.invulnerableDuration;
                    this.hurtDuration = 10;
                    this.hurtTime = this.hurtDuration;
                }

                Entity entity1 = source.getEntity();
                if (entity1 != null) {
                    if (entity1 instanceof LivingEntity) {
                        LivingEntity entityliving1 = (LivingEntity)entity1;
                        if (!source.is(DamageTypeTags.NO_ANGER)) {
                            this.setLastHurtByMob(entityliving1);
                        }
                    }

                    if (entity1 instanceof net.minecraft.world.entity.player.Player) {
                        net.minecraft.world.entity.player.Player entityhuman = (net.minecraft.world.entity.player.Player)entity1;
                        this.lastHurtByPlayerTime = 100;
                        this.lastHurtByPlayer = entityhuman;
                    } else if (entity1 instanceof Wolf) {
                        Wolf entitywolf = (Wolf)entity1;
                        if (entitywolf.isTame()) {
                            this.lastHurtByPlayerTime = 100;
                            LivingEntity entityliving2 = entitywolf.getOwner();
                            if (entityliving2 instanceof net.minecraft.world.entity.player.Player) {
                                net.minecraft.world.entity.player.Player entityhuman1 = (net.minecraft.world.entity.player.Player)entityliving2;
                                this.lastHurtByPlayer = entityhuman1;
                            } else {
                                this.lastHurtByPlayer = null;
                            }
                        }
                    }
                }

                boolean flag2;
                if (flag1) {
                    if (flag) {
                        this.level().broadcastEntityEvent(this, (byte)29);
                    } else {
                        this.level().broadcastDamageEvent(this, source);
                    }

                    if (!source.is(DamageTypeTags.NO_IMPACT) && (!flag || amount > 0.0F)) {
                        this.markHurt();
                    }

                    if (entity1 != null && !source.is(DamageTypeTags.NO_KNOCKBACK)) {
                        flag2 = entity1.distanceToSqr(this) > 40000.0;
                        double d0 = flag2 ? Math.random() - Math.random() : entity1.getX() - this.getX();

                        double d1;
                        for(d1 = flag2 ? Math.random() - Math.random() : entity1.getZ() - this.getZ(); d0 * d0 + d1 * d1 < 1.0E-4; d1 = (Math.random() - Math.random()) * 0.01) {
                            d0 = (Math.random() - Math.random()) * 0.01;
                        }

                        this.knockback(0.4000000059604645, d0, d1, entity1);
                        if (!flag) {
                          //  this.indicateDamage(d0, d1);
                        }
                    }
                }

                if (this.isDeadOrDying()) {
//                    if (!this.checkTotemDeathProtection(source)) {
                        this.silentDeath = !flag1;
                        this.die(source);
                        this.silentDeath = false;
//                    }
                } else if (flag1) {
                    this.playHurtSound(source);
                }

                flag2 = !flag || amount > 0.0F;
                if (flag2) {
//                    this.lastDamageSource = source;
//                    this.lastDamageStamp = this.level().getGameTime();
                }

                if (this instanceof ServerPlayer) {
                    CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer)this, source, f1, amount, flag);
                    if (f2 > 0.0F && f2 < 3.4028235E37F) {
                        ((ServerPlayer)this).awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(f2 * 10.0F));
                    }
                }

                if (entity1 instanceof ServerPlayer) {
                    CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer)entity1, this, source, f1, amount, flag);
                }

                return flag2;
            }
        } else {
            return false;
        }
    }

    public boolean playerHurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (this.getAbilities().invulnerable && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            this.noActionTime = 0;
            if (this.isDeadOrDying()) {
                return false;
            } else {
                if (!this.level().isClientSide) {
                }

                if (source.scalesWithDifficulty()) {
                    if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
                        return false;
                    }

                    if (this.level().getDifficulty() == Difficulty.EASY) {
                        amount = Math.min(amount / 2.0F + 1.0F, amount);
                    }

                    if (this.level().getDifficulty() == Difficulty.HARD) {
                        amount = amount * 3.0F / 2.0F;
                    }
                }

                // boolean damaged = super.hurt(source, amount);
                boolean damaged = livingEntityHurt(source, amount);
                if (damaged) {
                    this.removeEntitiesOnShoulder();
                }

                return damaged;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            boolean flag = this.server.isDedicatedServer() && source.is(DamageTypeTags.IS_FALL);
            if (!flag && this.spawnInvulnerableTime > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return false;
            } else {
                Entity entity = source.getEntity();
                if (entity instanceof net.minecraft.world.entity.player.Player) {
                    net.minecraft.world.entity.player.Player entityhuman = (net.minecraft.world.entity.player.Player)entity;
                    if (!this.canHarmPlayer(entityhuman)) {
                        return false;
                    }
                }

                if (entity instanceof AbstractArrow) {
                    AbstractArrow entityarrow = (AbstractArrow)entity;
                    Entity entity1 = entityarrow.getOwner();
                    if (entity1 instanceof net.minecraft.world.entity.player.Player) {
                        net.minecraft.world.entity.player.Player entityhuman1 = (net.minecraft.world.entity.player.Player)entity1;
                        if (!this.canHarmPlayer(entityhuman1)) {
                            return false;
                        }
                    }
                }


                //boolean damaged = super.hurt(source, amount);
                boolean damaged = playerHurt(source, amount);
                return damaged;
            }
        }
    }

    public void attack(Entity target) {
        boolean willAttack = target.isAttackable() && !target.skipAttackInteraction(this);
        PrePlayerAttackEntityEvent playerAttackEntityEvent = new PrePlayerAttackEntityEvent((org.bukkit.entity.Player)this.getBukkitEntity(), target.getBukkitEntity(), willAttack);
        if (!playerAttackEntityEvent.isCancelled() && willAttack) {
            float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float f1;
            if (target instanceof LivingEntity) {
                f1 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity)target).getMobType());
            } else {
                f1 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), MobType.UNDEFINED);
            }

            float f2 = this.getAttackStrengthScale(0.5F);
            f *= 0.2F + f2 * f2 * 0.8F;
            f1 *= f2;
            if (f > 0.0F || f1 > 0.0F) {
                boolean flag = f2 > 0.9F;
                boolean flag1 = false;
                byte b0 = 0;
                int i = b0 + EnchantmentHelper.getKnockbackBonus(this);
                if (this.isSprinting() && flag) {
                    // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.0F);
                    ++i;
                    flag1 = true;
                }

                boolean flag2 = flag && this.fallDistance > 0.0F && !this.onGround() && !this.onClimbable() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && target instanceof LivingEntity;
                flag2 = flag2 && !this.level().paperConfig().entities.behavior.disablePlayerCrits;
                flag2 = flag2 && !this.isSprinting();
                if (flag2) {
                    f *= 1.5F;
                }

                f += f1;
                boolean flag3 = false;
                double d0 = (double)(this.walkDist - this.walkDistO);
                if (flag && !flag2 && !flag1 && this.onGround() && d0 < (double)this.getSpeed()) {
                    ItemStack itemstack = this.getItemInHand(InteractionHand.MAIN_HAND);
                    if (itemstack.getItem() instanceof SwordItem) {
                        flag3 = true;
                    }
                }

                float f3 = 0.0F;
                boolean flag4 = false;
                int j = EnchantmentHelper.getFireAspect(this);
                if (target instanceof LivingEntity) {
                    f3 = ((LivingEntity)target).getHealth();
                    if (j > 0 && !target.isOnFire()) {
                        EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(this.getBukkitEntity(), target.getBukkitEntity(), 1);
                        Bukkit.getPluginManager().callEvent(combustEvent);
                        if (!combustEvent.isCancelled()) {
                            flag4 = true;
                            target.setSecondsOnFire(combustEvent.getDuration(), false);
                        }
                    }
                }

                Vec3 vec3d = target.getDeltaMovement();
                boolean flag5 = target.hurt(this.damageSources().playerAttack(this).critical(flag2), f);
                if (flag5) {
                    if (i > 0) {
                        if (target instanceof LivingEntity) {
                            ((LivingEntity)target).knockback((double)((float)i * 0.5F), (double)Mth.sin(this.getYRot() * 0.017453292F), (double)(-Mth.cos(this.getYRot() * 0.017453292F)), this);
                        } else {
                            target.push((double)(-Mth.sin(this.getYRot() * 0.017453292F) * (float)i * 0.5F), 0.1, (double)(Mth.cos(this.getYRot() * 0.017453292F) * (float)i * 0.5F), this);
                        }

                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
                        if (!this.level().paperConfig().misc.disableSprintInterruptionOnAttack) {
                            this.setSprinting(false);
                        }
                    }

                    if (flag3) {
                        float f4 = 1.0F + EnchantmentHelper.getSweepingDamageRatio(this) * f;
                        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.0, 0.25, 1.0));
                        Iterator iterator = list.iterator();

                        label190:
                        while(true) {
                            LivingEntity entityliving;
                            do {
                                do {
                                    do {
                                        do {
                                            if (!iterator.hasNext()) {
                                                // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.0F);
                                                this.sweepAttack();
                                                break label190;
                                            }

                                            entityliving = (LivingEntity)iterator.next();
                                        } while(entityliving == this);
                                    } while(entityliving == target);
                                } while(this.isAlliedTo(entityliving));
                            } while(entityliving instanceof ArmorStand && ((ArmorStand)entityliving).isMarker());

                            if (this.distanceToSqr(entityliving) < 9.0 && entityliving.hurt(this.damageSources().playerAttack(this).sweep().critical(flag2), f4)) {
                                entityliving.knockback(0.4000000059604645, (double)Mth.sin(this.getYRot() * 0.017453292F), (double)(-Mth.cos(this.getYRot() * 0.017453292F)), this);
                            }
                        }
                    }

                    if (target instanceof ServerPlayer && target.hurtMarked) {
                        boolean cancelled = false;
                        org.bukkit.entity.Player player = (org.bukkit.entity.Player)target.getBukkitEntity();
                        Vector velocity = CraftVector.toBukkit(vec3d);
                        PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity.clone());
                        this.level().getCraftServer().getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            cancelled = true;
                        } else if (!velocity.equals(event.getVelocity())) {
                            player.setVelocity(event.getVelocity());
                        }

                        if (!cancelled) {
                            // ((ServerPlayer)target).connection.send(new ClientboundSetEntityMotionPacket(target));
                            target.hurtMarked = false;
                            target.setDeltaMovement(vec3d);
                        }
                    }

                    if (flag2) {
                        // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.0F);
                        this.crit(target);
                    }

                    if (!flag2 && !flag3) {
                        if (flag) {
                            // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0F, 1.0F);
                        } else {
                           //  sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0F, 1.0F);
                        }
                    }

                    if (f1 > 0.0F) {
                        this.magicCrit(target);
                    }

                    this.setLastHurtMob(target);
                    if (target instanceof LivingEntity) {
                        EnchantmentHelper.doPostHurtEffects((LivingEntity)target, this);
                    }

                    EnchantmentHelper.doPostDamageEffects(this, target);
                    ItemStack itemstack1 = this.getMainHandItem();
                    Object object = target;
                    if (target instanceof EnderDragonPart) {
                        object = ((EnderDragonPart)target).parentMob;
                    }

                    if (!this.level().isClientSide && !itemstack1.isEmpty() && object instanceof LivingEntity) {
                        itemstack1.hurtEnemy((LivingEntity)object, this);
                        if (itemstack1.isEmpty()) {
                            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        }
                    }

                    if (target instanceof LivingEntity) {
                        float f5 = f3 - ((LivingEntity)target).getHealth();
                        this.awardStat(Stats.DAMAGE_DEALT, Math.round(f5 * 10.0F));
                        if (j > 0) {
                            EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(this.getBukkitEntity(), target.getBukkitEntity(), j * 4);
                            Bukkit.getPluginManager().callEvent(combustEvent);
                            if (!combustEvent.isCancelled()) {
                                target.setSecondsOnFire(combustEvent.getDuration(), false);
                            }
                        }

                        if (this.level() instanceof ServerLevel && f5 > 2.0F) {
                            int k = (int)((double)f5 * 0.5);
                            ((ServerLevel)this.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(), k, 0.1, 0.0, 0.1, 0.2);
                        }
                    }

                    this.causeFoodExhaustion(this.level().spigotConfig.combatExhaustion, EntityExhaustionEvent.ExhaustionReason.ATTACK);
                } else {
                   //  sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0F, 1.0F);
                    if (flag4) {
                        target.clearFire();
                    }

                    if (this instanceof ServerPlayer) {
                        ((ServerPlayer)this).getBukkitEntity().updateInventory();
                    }
                }
            }
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

            // Written by me


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

    @Override
    public void onEnterCombat() {

    }

    @Override
    public void onLeaveCombat() {

    }

    @Override
    public void die(DamageSource damageSource) {
        this.gameEvent(GameEvent.ENTITY_DIE);
        boolean flag = this.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        if (!this.isRemoved()) {
            List<Entity.DefaultDrop> loot = new ArrayList(this.getInventory().getContainerSize());
            boolean keepInventory = this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || this.isSpectator();
            if (!keepInventory) {
                Iterator var5 = this.getInventory().getContents().iterator();

                while(var5.hasNext()) {
                    ItemStack item = (ItemStack)var5.next();
                    if (!item.isEmpty() && !EnchantmentHelper.hasVanishingCurse(item)) {
                        loot.add(new Entity.DefaultDrop(item, (stack) -> {
                            this.drop(stack, true, false);
                        }));
                    }
                }
            }

            if (this.shouldDropLoot() && this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                this.dropFromLootTable(damageSource, this.lastHurtByPlayerTime > 0);
                loot.addAll(this.drops);
                this.drops.clear();
            }
            net.minecraft.network.chat.Component defaultMessage = this.getCombatTracker().getDeathMessage();
            this.keepLevel = keepInventory;
            PlayerDeathEvent event = CraftEventFactory.callPlayerDeathEvent(this, loot, PaperAdventure.asAdventure(defaultMessage), defaultMessage.getString(), keepInventory);
//            if (event.isCancelled()) {
////                if (this.getHealth() <= 0.0F) {
////                    this.setHealth((float)event.getReviveHealth());
////                }

           // } else {



                this.removeEntitiesOnShoulder();

                this.dropExperience();

//                if (!event.getKeepInventory()) {
//                    Iterator var13 = this.getInventory().compartments.iterator();
//
//                    while(var13.hasNext()) {
//                        NonNullList<ItemStack> inv = (NonNullList)var13.next();
//                        processKeep(event, inv);
//                    }
//
//                    processKeep(event, (NonNullList)null);
//                }

                this.setCamera(this);
                this.level().getCraftServer().getScoreboardManager().getScoreboardScores(ObjectiveCriteria.DEATH_COUNT, this.getScoreboardName(), Score::increment);
                LivingEntity entityliving = this.getKillCredit();
                if (entityliving != null) {
                    this.awardStat(Stats.ENTITY_KILLED_BY.get(entityliving.getType()));
                    entityliving.awardKillScore(this, this.deathScore, damageSource);
                    this.createWitherRose(entityliving);
                }

                this.level().broadcastEntityEvent(this, (byte)3);
                this.awardStat(Stats.DEATHS);
                this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
                this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
                this.clearFire();
                this.setTicksFrozen(0);
                this.setSharedFlagOnFire(false);
                this.getCombatTracker().recheckStatus();
                this.setLastDeathLocation(Optional.of(GlobalPos.of(this.level().dimension(), this.blockPosition())));
           // }
        }
    }

    @Nullable @Override
    public Entity changeDimension(ServerLevel worldserver, PlayerTeleportEvent.TeleportCause cause) {
        return this;
    }

    @Override
    protected void completeUsingItem() {
        if (!this.useItem.isEmpty() && this.isUsingItem()) {
         //   this.connection.send(new ClientboundEntityEventPacket(this, (byte)9));
            // super.completeUsingItem();
            livingEntityCompleteUsingItem();
        }

    }

    protected void livingEntityCompleteUsingItem() {
        if (!this.level().isClientSide || this.isUsingItem()) {
            InteractionHand enumhand = this.getUsedItemHand();
            if (!this.useItem.equals(this.getItemInHand(enumhand))) {
                this.releaseUsingItem();
            } else if (!this.useItem.isEmpty() && this.isUsingItem()) {
                this.startUsingItem(this.getUsedItemHand(), true);
                this.triggerItemUseEffects(this.useItem, 16);
                PlayerItemConsumeEvent event = null;
                ItemStack itemstack;
                if (this instanceof ServerPlayer) {
                    org.bukkit.inventory.ItemStack craftItem = CraftItemStack.asBukkitCopy(this.useItem);
                    org.bukkit.inventory.EquipmentSlot hand = CraftEquipmentSlot.getHand(enumhand);
                    event = new PlayerItemConsumeEvent((org.bukkit.entity.Player)this.getBukkitEntity(), craftItem, hand);
                    this.level().getCraftServer().getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        this.stopUsingItem();
                        ((ServerPlayer)this).getBukkitEntity().updateInventory();
                        ((ServerPlayer)this).getBukkitEntity().updateScaledHealth();
                        return;
                    }

                    itemstack = craftItem.equals(event.getItem()) ? this.useItem.finishUsingItem(this.level(), this) : CraftItemStack.asNMSCopy(event.getItem()).finishUsingItem(this.level(), this);
                } else {
                    itemstack = this.useItem.finishUsingItem(this.level(), this);
                }


                if (itemstack != this.useItem) {
                    this.setItemInHand(enumhand, itemstack);
                }

                this.stopUsingItem();
                if (this instanceof ServerPlayer) {
                    ((ServerPlayer)this).getBukkitEntity().updateInventory();
                }
            }
        }

    }

    protected void livingEntityOnEffectAdded(MobEffectInstance effect, @Nullable Entity source) {
        this.effectsDirty = true;
        if (!this.level().isClientSide) {
            effect.getEffect().addAttributeModifiers(this.getAttributes(), effect.getAmplifier());
            // this.sendEffectToPassengers(effect);
        }

    }


    @Override
    protected void onEffectAdded(MobEffectInstance effect, @Nullable Entity source) {
        livingEntityOnEffectAdded(effect, source);
       // this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effect));
        if (effect.getEffect() == MobEffects.LEVITATION) {
            this.levitationStartTime = this.tickCount;
            this.levitationStartPos = this.position();
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, source);
    }


    protected void livingEntityOnEffectUpdated(MobEffectInstance effect, boolean reapplyEffect, @Nullable Entity source) {
        this.effectsDirty = true;
        if (reapplyEffect && !this.level().isClientSide) {
            MobEffect mobeffectlist = effect.getEffect();
            mobeffectlist.removeAttributeModifiers(this.getAttributes());
            mobeffectlist.addAttributeModifiers(this.getAttributes(), effect.getAmplifier());
            // this.refreshDirtyAttributes();
        }

        if (!this.level().isClientSide) {
       //     this.sendEffectToPassengers(effect);
        }

    }
    @Override
    protected void onEffectUpdated(MobEffectInstance effect, boolean reapplyEffect, @Nullable Entity source) {
        livingEntityOnEffectUpdated(effect, reapplyEffect, source);
  //      this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effect));
        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, source);
    }

    protected void livingEntityOnEffectRemoved(MobEffectInstance effect) {
        this.effectsDirty = true;
        if (!this.level().isClientSide) {
            effect.getEffect().removeAttributeModifiers(this.getAttributes());
          //  this.refreshDirtyAttributes();
            Iterator iterator = this.getPassengers().iterator();

            while(iterator.hasNext()) {
                Entity entity = (Entity)iterator.next();
                if (entity instanceof ServerPlayer) {
                    ServerPlayer entityplayer = (ServerPlayer)entity;
           //         entityplayer.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), effect.getEffect()));
                }
            }
        }

    }

    @Override
    protected void onEffectRemoved(MobEffectInstance effect) {
        livingEntityOnEffectRemoved(effect);
       //  this.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), effect.getEffect()));
        if (effect.getEffect() == MobEffects.LEVITATION) {
            this.levitationStartPos = null;
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, (Entity)null);
    }

    @Override
    public void indicateDamage(double deltaX, double deltaZ) {
        this.hurtDir = (float)(Mth.atan2(deltaZ, deltaX) * 57.2957763671875 - (double)this.getYRot());
        // this.connection.send(new ClientboundHurtAnimationPacket(this));
    }

}
