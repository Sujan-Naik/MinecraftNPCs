package com.sereneoasis.command;

import com.sereneoasis.SerenityEntities;
import com.sereneoasis.entity.BaseZombieEntity;
import com.sereneoasis.entity.HumanEntity;
import com.sereneoasis.util.NPCUtils;
import com.sereneoasis.util.PacketUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.entity.Player;

public class CreateCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {

        if (sender instanceof Player player){


//            ps.send(new ClientboundSetEquipmentPacket(npc.getBukkitEntity().getEntityId(), List.of(Pair.of(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(item)))));
//            ps.send(new ClientboundSetEquipmentPacket(npc.getBukkitEntity().getEntityId(), List.of(Pair.of(EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(item)))));
//            ps.send(new ClientboundSetEquipmentPacket(npc.getBukkitEntity().getEntityId(), List.of(Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(new ItemStack(Material.GOLDEN_HELMET, 1))))));
//
//            //add it to the list of NPCs so we can access it in our movement listener
//            SerenityEntities.getInstance().getNpcs().add(npc);



           // ServerPlayer NPC = NPCUtils.createPlayer(p.getLocation());

            HumanEntity npc = NPCUtils.spawnNPC(player.getLocation(), player, "Noob", "Notch");
//            SerenityEntities.getInstance().getNpcs().add(npc);
            player.sendMessage("command run");
            NPCUtils.updateEquipment(npc, player);
            SerenityEntities.getInstance().getNpcs().put(npc, npc.getBukkitEntity().getLocation());

            /*Location loc = p.getEyeLocation();

            BaseZombieEntity npc = new BaseZombieEntity(loc, p);
            ServerLevel serverLevel = ((CraftWorld) loc.getWorld()).getHandle();
            serverLevel.addFreshEntity(npc);
 */

        }

        return true;
    }
}