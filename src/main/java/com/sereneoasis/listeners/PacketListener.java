package com.sereneoasis.listeners;



import com.sereneoasis.SerenityEntities;
import io.netty.channel.*;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.core.Rotations;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_20_R2.entity.*;
import org.bukkit.entity.*;

public class PacketListener {

    public void removePlayer(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;
        channel.eventLoop().submit(() -> {
            channel.pipeline().remove(player.getName());
            return null;
        });
    }

    public void injectPlayer(Player player) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {

            @Override
            public void channelRead(ChannelHandlerContext channelHandlerContext, Object packet) throws Exception {
                //Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "PACKET READ: " + ChatColor.RED + packet.toString());

                //Bukkit.broadcastMessage(String.valueOf(packet.getClass()));

                if (SerenityEntities.getInstance().getNpcs().keySet().stream().anyMatch((serverPlayer) -> serverPlayer.getBukkitEntity().getPlayer() == player))
                {

                    Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "PACKET READ: " + ChatColor.RED + packet.toString());
                    return;
//                    if (packet instanceof ServerboundPlayerInputPacket){
//                        return;
//                    }
                }

                if ( ! player.getName().equals("SereneOasis"))
                {
                    Bukkit.broadcastMessage(player.getName() + " is trying to steal your packets!!");
                    Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "PACKET STOPPED: " + ChatColor.GREEN + packet.toString());
                    return;
                }
                super.channelRead(channelHandlerContext, packet);
            }

            @Override
            public void write(ChannelHandlerContext channelHandlerContext, Object packet, ChannelPromise channelPromise) throws Exception {
                //Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "PACKET WRITE: " + ChatColor.GREEN + packet.toString());

                if ( ! player.getName().equals("SereneOasis"))
                {
                    Bukkit.broadcastMessage(player.getName() + " is trying to steal your packets!!");
                    Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "PACKET STOPPED: " + ChatColor.GREEN + packet.toString());
                    return;
                }

                //if the server is sending a packet, the function "write" will be called. If you want to cancel a specific packet, just use return; Please keep in mind that using the return thing can break the intire server when using the return thing without knowing what you are doing.
                super.write(channelHandlerContext, packet, channelPromise);
            }


        };

        ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().connection.connection.channel.pipeline();
        pipeline.addBefore("packet_handler", player.getName(), channelDuplexHandler);

    }
}
