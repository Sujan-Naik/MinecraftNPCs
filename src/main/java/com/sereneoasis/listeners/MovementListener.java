package com.sereneoasis.listeners;

import com.sereneoasis.SerenityEntities;
import com.sereneoasis.util.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MovementListener implements Listener {

    //Every time a player moves, get the NPCs and make them look at the player's new location
/*    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e){

        Player p = e.getPlayer();

        //Loop through each NPC
        SerenityEntities.getInstance().getNpcs().entrySet().stream()
                .forEach( (entry) -> {
                    Entity npc = entry.getKey();
                    Location oldLoc = entry.getValue();
                    //get the connection so we can send packets in NMS
                    ServerGamePacketListenerImpl ps = ((CraftPlayer) p).getHandle().connection;
                    Bukkit.broadcastMessage("x should be changing by " + (npc.getX() - oldLoc.getX()));


                    ClientboundMoveEntityPacket clientboundMoveEntityPacket = new ClientboundMoveEntityPacket.PosRot(npc.getId(),
                            PacketUtils.deltaPosition(npc.getX(), oldLoc.getX()),
                            PacketUtils.deltaPosition(npc.getY(), oldLoc.getY()),
                            PacketUtils.deltaPosition(npc.getZ(), oldLoc.getZ()),
                            (byte) npc.getBukkitYaw(),
                            (byte) npc.getBukkitEntity().getPitch(),
                            npc.onGround);

                    ps.send(clientboundMoveEntityPacket);

                });

        SerenityEntities.getInstance().updateLocations();
    }*/

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        SerenityEntities.getPacketListener().injectPlayer(event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        SerenityEntities.getPacketListener().removePlayer(event.getPlayer());
    }
}