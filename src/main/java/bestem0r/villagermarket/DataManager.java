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
    private ArrayList<Player> moveVillager = new ArrayList<>();
    private HashMap<Player, Entity> moveTo = new HashMap<>();

    private HashMap<String, VillagerShop> villagers = new HashMap<>();
    private ArrayList<Entity> villagerEntities = new ArrayList<>();


    public void addVillager(String entityUUID, File file, VillagerShop.VillagerType type) {
        switch (type) {
            case ADMIN:
                if (villagers.containsKey(entityUUID)) {
                    villagers.replace(entityUUID, new AdminShop(file));
                } else {
                    villagers.put(entityUUID, new AdminShop(file));
                }
                break;
            case PLAYER:
                if (villagers.containsKey(entityUUID)) {
                    villagers.replace(entityUUID, new PlayerShop(file));
                } else {
                    villagers.put(entityUUID, new PlayerShop(file));
                }

        }
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

    public ArrayList<Player> getMoveVillager() {
        return moveVillager;
    }

    public HashMap<Player, Entity> getMoveTo() {
        return moveTo;
    }

    public HashMap<String, String> getClickMap() {
        return clickMap;
    }

}
