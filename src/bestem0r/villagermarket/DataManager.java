package bestem0r.villagermarket;

import bestem0r.villagermarket.shops.AdminShop;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.ColorBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class DataManager {

    private HashMap<String, String> clickMap = new HashMap<>();
    private HashMap<String, ItemParser> amountHashMap = new HashMap<>();
    private HashMap<String, ItemParser> priceHashMap = new HashMap<>();

    private ArrayList<Player> removeVillager = new ArrayList<>();

    private HashMap<String, VillagerShop> villagers = new HashMap<>();
    private ArrayList<Entity> villagerEntities = new ArrayList<>();


    public void addVillager(String entityUUID, File file, VillagerShop.VillagerType type) {
        switch (type) {
            case ADMIN:
                villagers.put(entityUUID, new AdminShop(file));
                break;
            case PLAYER:
                villagers.put(entityUUID, new PlayerShop(file));
        }
    }
    public void removeVillager(String entityUUID) {
        villagers.remove(entityUUID);
    }
    public void addAmount(ItemStack itemStack, String playerUUID, int slot, String entityUUID) {
        amountHashMap.put(playerUUID, new ItemParser(itemStack, slot, entityUUID, 0));
    }
    public void addPrice(ItemStack itemStack, String playerUUID, int slot, String entityUUID, int amount) {
        priceHashMap.put(playerUUID, new ItemParser(itemStack, slot, entityUUID, amount));
    }


    public void removePrice(String playerUUID) {
        priceHashMap.remove(playerUUID);
    }
    public void removeAmount(String playerUUID) {
        amountHashMap.remove(playerUUID);
    }

    public HashMap<String, ItemParser> getAmountHashMap() {
        return amountHashMap;
    }
    public HashMap<String, ItemParser> getPriceHashMap() {
        return priceHashMap;
    }
    public HashMap<String, VillagerShop> getVillagers() {
        return villagers;
    }
    public ArrayList<Entity> getVillagerEntities() {
        return villagerEntities;
    }

    public ArrayList<Player> getRemoveVillager() {
        return removeVillager;
    }

    public HashMap<String, String> getClickMap() {
        return clickMap;
    }

}
