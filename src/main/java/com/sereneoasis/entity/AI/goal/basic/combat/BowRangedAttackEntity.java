package com.sereneoasis.entity.AI.goal.basic.combat;

import com.sereneoasis.entity.HumanEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class BowRangedAttackEntity extends Combat{
    public BowRangedAttackEntity(String name, HumanEntity npc, int priority, LivingEntity entity) {
        super(name, npc, priority, entity);

        this.entity = entity;


//        Player player = npc.getBukkitEntity().getPlayer();
//        player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
//        player.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));


         npc.setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.fromBukkitCopy(new ItemStack(Material.BOW)));
         npc.getInventory().setItem(npc.getInventory().getFreeSlot(), net.minecraft.world.item.ItemStack.fromBukkitCopy(new ItemStack(Material.ARROW, 1)));

         if (entity instanceof Player player) {
             player.displayClientMessage(Component.literal("dodge this arrow fat nerd"), true);
         }
    }

    @Override
    public void tick() {
        if (npc.isUsingItem()) {
            int drawingTime = npc.server.getTickCount() - npc.timeSinceBowDraw;
            if (drawingTime >= 20) {

                npc.stopUsingItem();
                npc.performRangedAttack(entity, BowItem.getPowerForTime(drawingTime));
                npc.attackTime = npc.attackIntervalMin;
                npc.timeSinceBowDraw = -1;
                finished = true;
            }
        } else if (--npc.attackTime <= 0) {
            npc.startUsingItem(ProjectileUtil.getWeaponHoldingHand(npc, Items.BOW));
            npc.timeSinceBowDraw = npc.server.getTickCount();

        }
    }
}
