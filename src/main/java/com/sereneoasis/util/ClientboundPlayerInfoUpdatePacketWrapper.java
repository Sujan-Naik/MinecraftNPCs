package com.sereneoasis.util;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiConsumer;

public class ClientboundPlayerInfoUpdatePacketWrapper {
    private final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions;

    private final ServerPlayer entity;

    private final ClientboundPlayerInfoUpdatePacket.Entry entry;

    private static final int DEFAULT_LATENCY = 1;

    private static final HashMap<ClientboundPlayerInfoUpdatePacket.Action, BiConsumer<FriendlyByteBuf, ClientboundPlayerInfoUpdatePacket.Entry>> actionWriters;

    static {
        actionWriters = new HashMap<>();
        actionWriters.put(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, (buffer, entry) -> {
            GameProfile profile = Objects.requireNonNull(entry.profile());
            buffer.writeUtf(profile.getName(), 16);
            buffer.writeGameProfileProperties(profile.getProperties());
        });
        actionWriters.put(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT, (buffer, entry) ->
                buffer.writeNullable(entry.chatSession(), RemoteChatSession.Data::write));
        actionWriters.put(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, (buffer, entry) ->
                buffer.writeVarInt(entry.gameMode().getId()));
        actionWriters.put(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, (buffer, entry) ->
                buffer.writeBoolean(entry.listed()));
        actionWriters.put(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY, (buffer, entry) ->
                buffer.writeVarInt(entry.latency()));
        actionWriters.put(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, (buffer, entry) ->
                buffer.writeNullable(entry.displayName(), FriendlyByteBuf::writeComponent));
    }

    public ClientboundPlayerInfoUpdatePacketWrapper(
            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions,
            ServerPlayer entity,
            int latency,
            boolean listed) {
        this.actions = actions;
        this.entity = entity;

        entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                entity.getUUID(),
                entity.getGameProfile(),
                listed,
                latency,
                GameType.CREATIVE,
                null,
                null);
    }

    public ClientboundPlayerInfoUpdatePacketWrapper(
            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions,
            ServerPlayer entity,
            boolean listed) {
        this(actions, entity, DEFAULT_LATENCY, listed);
    }

    public ClientboundPlayerInfoUpdatePacketWrapper(
            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions,
            ServerPlayer entity,
            int latency) {
        this(actions, entity, latency, true);
    }

    public ClientboundPlayerInfoUpdatePacketWrapper(
            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions,
            ServerPlayer entity) {
        this(actions, entity, DEFAULT_LATENCY);
    }

    public ClientboundPlayerInfoUpdatePacket getPacket() {
        FriendlyByteBuf buffer = prepareBuffer();
        return new ClientboundPlayerInfoUpdatePacket(buffer);
    }

    private FriendlyByteBuf prepareBuffer() {
        ByteBuf directByteBuf = Unpooled.directBuffer();
        FriendlyByteBuf buffer = new FriendlyByteBuf(directByteBuf);

        // Actions (Byte)
        buffer.writeEnumSet(actions, ClientboundPlayerInfoUpdatePacket.Action.class);

        // Number of players (VarInt)
        buffer.writeVarInt(1);

        // UUID
        buffer.writeUUID(entity.getUUID());

        // Array of Player Actions
        for (ClientboundPlayerInfoUpdatePacket.Action action : actions) {
            BiConsumer<FriendlyByteBuf, ClientboundPlayerInfoUpdatePacket.Entry> actionWriter = actionWriters.get(action);
            actionWriter.accept(buffer, entry);
        }

        return buffer;
    }
}
