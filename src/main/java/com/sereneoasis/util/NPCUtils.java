package com.sereneoasis.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import com.sereneoasis.SerenityEntities;
import com.sereneoasis.entity.HumanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
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

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URL;
import java.util.*;

public class NPCUtils {

    public static HumanEntity spawnNPC(Location location, Player player, String name, String skinUsersIGN){
        //ServerPlayer player = ((CraftPlayer)p).getHandle();

        MinecraftServer minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);

        net.minecraft.world.entity.player.Player nmsPlayer = ((CraftPlayer)player).getHandle();
        HumanEntity serverPlayer = new HumanEntity(minecraftServer, serverLevel, setSkin(skinUsersIGN, gameProfile), ClientInformation.createDefault());
        serverPlayer.setPos(location.getX(), location.getY(), location.getZ());

        SynchedEntityData synchedEntityData = serverPlayer.getEntityData();
        synchedEntityData.set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 127);

        //PacketUtils.setValue(serverPlayer, "c", ((CraftPlayer) player).getHandle().connection);


        serverPlayer.connection = ((CraftPlayer) player).getHandle().connection;

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


    public static void updateEquipment(HumanEntity npc, Player player){
        List<Pair<EquipmentSlot, ItemStack>> equipment = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipment.add(new Pair<EquipmentSlot, ItemStack> (slot, npc.getItemBySlot(slot)));
        }
        ClientboundSetEquipmentPacket clientboundSetEquipmentPacket =
                new ClientboundSetEquipmentPacket(npc.getId(),equipment );
        PacketUtils.sendPacket(clientboundSetEquipmentPacket, player);
    }

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
