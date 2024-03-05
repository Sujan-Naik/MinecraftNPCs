package com.sereneoasis;

import com.sereneoasis.command.CreateCommand;
import com.sereneoasis.listeners.MovementListener;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SerenityEntities extends JavaPlugin {

    private static SerenityEntities instance;

    public static SerenityEntities getInstance() {
        return instance;
    }

    //Used to keep our NPCs to be accessed in other classes
    private List<ServerPlayer> npcs = new ArrayList<>();
    public List<ServerPlayer> getNpcs() {
        return npcs;
    }
    private static Logger log;

    public static void main(String[] args) {

    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        SerenityEntities.log = instance.getLogger();

        this.getCommand("create").setExecutor(new CreateCommand());

        this.getServer().getPluginManager().registerEvents(new MovementListener(), this);


    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
