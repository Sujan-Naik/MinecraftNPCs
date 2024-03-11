package com.sereneoasis.entity.AI.inventory;

import com.sereneoasis.entity.HumanEntity;
import com.sereneoasis.util.NPCUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class InventoryTracker {

    private Inventory inventory;

    private final HumanEntity npc;

    public InventoryTracker(Inventory inventory, HumanEntity humanEntity) {

        this.inventory = inventory;
        this.npc = humanEntity;
    }


    public void tick() {

        this.inventory = npc.getInventory();

        List<ItemStack> heldArmor = inventory.getContents().stream().filter(itemStack -> itemStack.getItem() instanceof ArmorItem && !inventory.getArmorContents().contains(itemStack)).toList();
        for (ItemStack item : heldArmor) {
            ArmorItem armorItem = (ArmorItem) item.getItem();
            EquipmentSlot equipmentSlot = armorItem.getEquipmentSlot();
            boolean shouldSwitch = false;
            if (inventory.getArmor(equipmentSlot.getIndex()).getItem() instanceof ArmorItem currentArmor) {
                if (armorItem.getToughness() > currentArmor.getToughness()) {
                    shouldSwitch = true;
                }
            } else {
                shouldSwitch = true;
            }
            if (shouldSwitch) {
                npc.setItemSlot(equipmentSlot, item.copy());
                npc.getInventory().removeItem(item);

//                for (Player player : Bukkit.getOnlinePlayers()) {
//                    NPCUtils.updateEquipment(npc, player);
//                }
            }
        }
    }

    private List<ItemStack>getOfType(Predicate<ItemStack> condition){
        return inventory.getContents().stream().filter(condition).toList();
    }

    private List<ItemStack>getFood(){
        Predicate<ItemStack> isFood = itemStack -> itemStack.getItem().isEdible();
        return getOfType(isFood);
    }
    public boolean hasEnoughFood(){
        int neededFood = 20 - npc.foodData.getFoodLevel();
        int providedHunger = getFood().stream().map(itemStack -> itemStack.getItem().getFoodProperties().getNutrition()).collect(Collectors.summingInt(Integer::intValue));
        if (providedHunger >= neededFood) {
            return true;
        }
        return false;
    }

    public boolean hasFood(){
        if (getFood().isEmpty()){
            return false;
        }
        return true;
    }

    public ItemStack getMostAppropriateFood(){
        int neededFood = 20 - npc.getFoodData().getFoodLevel();
        return getFood().stream().min(Comparator.comparingInt(o -> (neededFood - o.getItem().getFoodProperties().getNutrition()))).get();
    }

}

