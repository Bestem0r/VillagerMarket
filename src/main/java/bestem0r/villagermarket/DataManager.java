package bestem0r.villagermarket;

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

    public HashMap<String, VillagerShop> getVillagers() {
        return villagers;
    }
    public ArrayList<Entity> getVillagerEntities() {
        return villagerEntities;
    }
     public void setVillagerEntities(ArrayList<Entity> list) {
        this.villagerEntities = list;
     }

    public ArrayList<Player> getRemoveVillager() {
        return removeVillager;
    }

    public HashMap<String, String> getClickMap() {
        return clickMap;
    }

}
