package com.sereneoasis;

import com.sereneoasis.command.CreateCommand;
import com.sereneoasis.listeners.MovementListener;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class SerenityEntities extends JavaPlugin {

    private static SerenityEntities instance;

    public static SerenityEntities getInstance() {
        return instance;
    }

    //Used to keep our NPCs to be accessed in other classes
    private HashMap<ServerPlayer, Location> npcs = new HashMap<>();
    public HashMap<ServerPlayer, Location> getNpcs() {
        return npcs;
    }

    public void updateLocations(){
        for (ServerPlayer npc : npcs.keySet()){
            npcs.put(npc, npc.getBukkitEntity().getLocation());
        }
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
