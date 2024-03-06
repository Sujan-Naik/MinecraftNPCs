package com.sereneoasis.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.sereneoasis.SerenityEntities;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URL;
import java.util.Collections;

import java.util.EnumSet;
import java.util.Scanner;
import java.util.UUID;

public class NPCUtils {

    public static ServerPlayer spawnNPC(Location location, Player player, String name, String skinUsersIGN){
        //ServerPlayer player = ((CraftPlayer)p).getHandle();

        MinecraftServer minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);

        net.minecraft.world.entity.player.Player nmsPlayer = ((CraftPlayer)player).getHandle();
        Location loc = player.getLocation();
        HumanEntity serverPlayer = new HumanEntity(minecraftServer, serverLevel, setSkin(skinUsersIGN, gameProfile), ClientInformation.createDefault());
        //ServerPlayer serverPlayer = new ServerPlayer(minecraftServer, serverLevel, setSkin(skinUsersIGN, gameProfile), ClientInformation.createDefault());
        serverPlayer.setPos(location.getX(), location.getY(), location.getZ());

        SynchedEntityData synchedEntityData = serverPlayer.getEntityData();
        synchedEntityData.set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 127);

        //PacketUtils.setValue(serverPlayer, "c", ((CraftPlayer) player).getHandle().connection);

        // PacketUtils.setValue(serverPlayer, "c", ((CraftPlayer) player).getHandle().connection);

        serverPlayer.connection = ((CraftPlayer) player).getHandle().connection;
//
        PacketUtils.sendPacket(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, serverPlayer), player);

//        ClientboundPlayerInfoUpdatePacketWrapper playerInfoPacket = new ClientboundPlayerInfoUpdatePacketWrapper(
//                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED),
//                serverPlayer,
//                180,
//                true
//        );
//        PacketUtils.sendPacket(playerInfoPacket.getPacket(), player);


        serverLevel.addFreshEntity(serverPlayer);
      //  serverPlayer.connection = null;


        PacketUtils.sendPacket(new ClientboundAddEntityPacket(serverPlayer), player);
        PacketUtils.sendPacket(new ClientboundSetEntityDataPacket(serverPlayer.getId(), synchedEntityData.getNonDefaultValues()), player);

        serverPlayer.setOwner(nmsPlayer);



        Bukkit.getScheduler().runTaskLaterAsynchronously(SerenityEntities.getInstance(), new Runnable() {
            @Override
            public void run() {
                PacketUtils.sendPacket(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(serverPlayer.getUUID())), player);
            }
        }, 40);

        return serverPlayer;
    }

//    public static ServerPlayer createPlayer(Location loc) {
//        try {
//            DedicatedServer srv = ((CraftServer) Bukkit.getServer()).getServer();
//            ServerLevel sw = ((CraftWorld) loc.getWorld()).getHandle();
//            UUID uid = UUID.randomUUID(); // The fake player's UUID
//            GameProfile profile = new GameProfile(uid, uid.toString().substring(0, 16)); // The second part is the player's name. If it is unnecessary, I just use the first 16 chars of the UUID
//            ClientInformation info = new ClientInformation("en", 0, ChatVisiblity.HIDDEN, false, 0, HumanoidArm.RIGHT, false, false); // Client information for the fake player. Locale, Visbility, Default Arm, etc.
//
//            ServerPlayer sp = new ServerPlayer(srv, sw, profile, info);
//            sp.connection = new ServerGamePacketListenerImpl(srv, new Connection(PacketFlow.CLIENTBOUND), sp, new CommonListenerCookie(profile, 0, info)); // A fake packet listener
//            sp.setPos(loc.getX(), loc.getY(), loc.getZ());
//
//            for (Player p : loc.getWorld().getPlayers()) {
//                ServerPlayer sph = ((CraftPlayer) p).getHandle();
//                sph.connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, sp)); // Adds the player to the server list, necessary to be spawned
//                sph.connection.send(new ClientboundAddEntityPacket(sp)); // Spawns the player
//                Bukkit.getScheduler().runTaskLaterAsynchronously(SerenityEntities.getInstance(), () -> sph.connection.send(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(uid))),
//                        1); // This is a utility class that calls an asynchronous task on the next tick to remove the fake player from the server list
//            }
//
//            return sp;
//        } catch (Exception e) {
//            // handle errors
//            return null;
//        }
//    }


    public static GameProfile setSkin(String name, GameProfile gameProfile) {
        Gson gson = new Gson();
        String url = "https://api.mojang.com/users/profiles/minecraft/" + name;
        String json = getStringFromURL(url);
        String uuid = gson.fromJson(json, JsonObject.class).get("id").getAsString();

        url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false";
        json = getStringFromURL(url);
        JsonObject mainObject = gson.fromJson(json, JsonObject.class);
        JsonObject jsonObject = mainObject.get("properties").getAsJsonArray().get(0).getAsJsonObject();
        String value = jsonObject.get("value").getAsString();
        String signature = jsonObject.get("signature").getAsString();
        PropertyMap propertyMap = gameProfile.getProperties();
        propertyMap.put("name", new Property("name", name));
        propertyMap.put("textures", new Property("textures", value, signature));
        return gameProfile;
    }

    public static void changeSkin(String value, String signature, GameProfile gameProfile) {
        gameProfile.getProperties().put("textures", new Property("textures", value, signature));
    }

    private static String getStringFromURL(String url) {
        StringBuilder text = new StringBuilder();
        try {
            Scanner scanner = new Scanner(new URL(url).openStream());
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                while (line.startsWith(" ")) {
                    line = line.substring(1);
                }
                text.append(line);
            }
            scanner.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return text.toString();
    }
}
