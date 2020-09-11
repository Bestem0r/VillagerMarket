package bestem0r.villagermarket;

import bestem0r.villagermarket.items.ItemForSale;
import bestem0r.villagermarket.shops.AdminShop;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.VillagerShop;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class DataManager {

    private HashMap<String, String> clickMap = new HashMap<>();
    private HashMap<String, ItemForSale.Builder> amountHashMap = new HashMap<>();
    private HashMap<String, ItemForSale.Builder> priceHashMap = new HashMap<>();

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

    public void addAmount(String playerUUID, ItemForSale.Builder builder) {
        amountHashMap.put(playerUUID, builder);
    }
    public void addPrice(String playerUUID, ItemForSale.Builder builder) {
        priceHashMap.put(playerUUID, builder);
    }


    public void removePrice(String playerUUID) {
        priceHashMap.remove(playerUUID);
    }
    public void removeAmount(String playerUUID) {
        amountHashMap.remove(playerUUID);
    }


    public HashMap<String, ItemForSale.Builder> getAmountHashMap() {
        return amountHashMap;
    }
    public HashMap<String, ItemForSale.Builder> getPriceHashMap() {
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
