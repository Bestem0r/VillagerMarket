package bestem0r.villagermarket;

import bestem0r.villagermarket.commands.MainCommand;
import bestem0r.villagermarket.commands.TabComplete;
import bestem0r.villagermarket.events.GenericEvents;
import bestem0r.villagermarket.events.InventoryClick;
import bestem0r.villagermarket.events.PlayerChat;
import bestem0r.villagermarket.events.PlayerEvents;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.ColorBuilder;
import bestem0r.villagermarket.utilities.Config;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VMPlugin extends JavaPlugin {

    private static Economy econ = null;
    private static VMPlugin instance;
    private static String prefix;
    private static DataManager dataManager;
    List<Material> materialBlackList = new ArrayList<>();

    @Override
    public void onEnable() {
        setupEconomy();

        instance = this;
        dataManager = new DataManager();

        getConfig().options().copyDefaults();
        saveDefaultConfig();
        reloadConfig();
        getLogger().info("Villager Market enabled!");

        setupConfigValues();
        loadConfigs();
        registerEvents();
        beginThreads();
        loadVillagers();
        loadDefaultValues();

        getCommand("vm").setTabCompleter(new TabComplete());
        getCommand("vm").setExecutor(new MainCommand());
        Config.setupPendingConfig();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        for (String entityUUID : dataManager.getVillagers().keySet()) {
            dataManager.getVillagers().get(entityUUID).save();
        }
        super.onDisable();
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        } else {
            Bukkit.getLogger().info("Could not find Economy Provider!");
        }

        return (econ != null);
    }

    private void setupConfigValues() {
        List<String> materials = getConfig().getStringList("item_blacklist");
        for (String materialString : materials) {
            Material material = Material.valueOf(materialString);
            materialBlackList.add(material);
        }
    }

    private void registerEvents() {
        GenericEvents genericEvents = new GenericEvents(this, dataManager);
        InventoryClick inventoryClick = new InventoryClick(dataManager);
        PlayerChat playerChat = new PlayerChat(this, dataManager);
        PlayerEvents playerEvents = new PlayerEvents(dataManager);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(genericEvents, this);
        pluginManager.registerEvents(inventoryClick, this);
        pluginManager.registerEvents(playerChat, this);
        pluginManager.registerEvents(playerEvents, this);
    }

    private void beginThreads() {
        AsyncThreads thread = new AsyncThreads();
        thread.beginSaveThread(dataManager);
    }

    private void loadVillagers() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (dataManager.getVillagers().containsKey(entity.getUniqueId().toString())) {
                    dataManager.getVillagerEntities().add(entity);
                }
            }
        }
    }
    private void loadConfigs() {
        File shopsFile = new File(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket").getDataFolder() + "/Shops/");
        if (shopsFile.exists()) {
            for (File file : shopsFile.listFiles()) {
                String fileName = file.getName();
                String entityUUID = fileName.substring(0, fileName.length() - 4);
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                String type = config.getString("type");
                type = (type == null ? "player" : type);

                dataManager.addVillager(entityUUID, file, VillagerShop.VillagerType.valueOf(type.toUpperCase()));
            }
        }
    }
    public static void loadDefaultValues() {
        prefix = ColorBuilder.color("plugin_prefix") + " ";
    }
    public static Economy getEconomy() {
        return econ;
    }
    public static VMPlugin getInstance() {
        return instance;
    }
    public static String getPrefix() {return prefix;}

    public static DataManager getDataManager() {
        return dataManager;
    }

    public List<Material> getMaterialBlackList() {
        return materialBlackList;
    }
}
