package com.sereneoasis.entity.AI.goal.basic.combat;

import com.sereneoasis.entity.AI.goal.interfaces.EntityInteraction;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PunchEntity extends Combat {
    

    public PunchEntity(String name, HumanEntity npc, int priority, LivingEntity Entity) {
        super(name, npc, priority, Entity);
        this.entity = Entity;
    }

    @Override
    public LivingEntity getEntity() {
        return entity;
    }

    @Override
    public void tick(){
        if ( npc.canPerformAttack(entity)) {
            npc.setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.fromBukkitCopy(new ItemStack(Material.DIAMOND_SWORD)));

            if (entity instanceof Player player) {
                player.displayClientMessage(Component.literal("you suck"), true);
            }

            npc.performAttack(entity);
        }
        finished = true;
    }
}
