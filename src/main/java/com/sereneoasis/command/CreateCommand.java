package com.sereneoasis.command;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import com.sereneoasis.SerenityEntities;
import com.sereneoasis.entity.HumanEntity;
import com.sereneoasis.util.NPCUtils;
import com.sereneoasis.util.PacketUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CreateCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {

        if (sender instanceof Player p){


//            ps.send(new ClientboundSetEquipmentPacket(npc.getBukkitEntity().getEntityId(), List.of(Pair.of(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(item)))));
//            ps.send(new ClientboundSetEquipmentPacket(npc.getBukkitEntity().getEntityId(), List.of(Pair.of(EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(item)))));
//            ps.send(new ClientboundSetEquipmentPacket(npc.getBukkitEntity().getEntityId(), List.of(Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(new ItemStack(Material.GOLDEN_HELMET, 1))))));
//
//            //add it to the list of NPCs so we can access it in our movement listener
//            SerenityEntities.getInstance().getNpcs().add(npc);



           // ServerPlayer NPC = NPCUtils.createPlayer(p.getLocation());

//            ServerPlayer npc = NPCUtils.spawnNPC(p.getLocation(), p, "Noob", "Notch");
//            SerenityEntities.getInstance().getNpcs().add(npc);
            p.sendMessage("command run");

            Location loc = p.getEyeLocation();

            HumanEntity npc = new HumanEntity(loc, p);
            ServerLevel serverLevel = ((CraftWorld) loc.getWorld()).getHandle();
            serverLevel.addFreshEntity(npc);
        }

        return true;
    }
}