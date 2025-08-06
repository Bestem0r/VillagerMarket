package net.bestemor.villagermarket;

import net.bestemor.core.CorePlugin;
import net.bestemor.core.command.CommandModule;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.command.ShopCommand;
import net.bestemor.villagermarket.command.subcommand.*;
import net.bestemor.villagermarket.listener.EntityListener;
import net.bestemor.villagermarket.listener.PlayerListener;
import net.bestemor.villagermarket.shop.ShopManager;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VMPlugin extends CorePlugin {

    private static Economy economy = null;

    public static final List<String> log = new ArrayList<>();

    private ShopManager shopManager;
    private PlayerListener playerListener;

    private final Map<String, String> localizedMaterials = new HashMap<>();

    @Override
    protected void onPluginEnable() {
        setupEconomy();

        Metrics metrics = new Metrics(this, 8922);

        ConfigManager.setPrefixPath("plugin_prefix");

        setupCommands();

        this.shopManager = new ShopManager(this);
        Bukkit.getScheduler().runTaskAsynchronously(this, shopManager::load);

        this.playerListener = new PlayerListener(this);
        registerEvents();

        boolean enableUpdate = !getConfig().contains("auto_update") || getConfig().getBoolean("auto_update");
        Bukkit.getLogger().info("[VillagerMarket] Auto config update is " + (enableUpdate ? "enabled" : "disabled"));

        Bukkit.getLogger().warning("[VillagerMarket] §cYou are running a §aBETA 1.13.0-#2 of VillagerMarket! Please expect and report all bugs in my discord server");

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Bukkit.getPluginManager().getPlugin("VillagerBank") != null) {
                Bukkit.getLogger().info("[VillagerMarket] Nice to see you Villager Bank!");
                Bukkit.getLogger().info("[VillagerBank] You too Villager Market!");
            }
        }, 31);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new PlaceholderManager(this).register();
        }
        loadMappings();

        VillagerMarketAPI.init(this);
    }

    @Override
    protected String[] getLanguages() {
        return new String[]{"en_US", "de_DE", "es_ES", "pt_BR", "zh_CN", "fr_FR"};
    }

    @Override
    protected String getLanguageFolder() {
        return "language";
    }

    @Override
    protected void onPluginDisable() {
        shopManager.closeAllShopfronts();
        shopManager.saveAll();
        if (getConfig().getBoolean("auto_log")) saveLog();
    }

    private void loadMappings() {
        localizedMaterials.clear();
        File materials = new File(getDataFolder(), "materials.yml");
        if (materials.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(materials);
            ConfigurationSection section = config.getConfigurationSection("materials");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    localizedMaterials.put(key, section.getString(key));
                }
            }
        }
    }

    private void setupCommands() {
        CommandModule module = new CommandModule.Builder(this)
                .addSubCommand("create", new CreateCommand(this))
                .addSubCommand("item", new ItemCommand(this))
                .addSubCommand("move", new MoveCommand(this))
                .addSubCommand("reload", new ReloadCommand(this))
                .addSubCommand("remove", new RemoveCommand(this))
                .addSubCommand("search", new SearchCommand(this))
                .addSubCommand("stats", new StatsCommand(this))
                .addSubCommand("trusted", new TrustedCommand(this))
                .addSubCommand("getid", new GetIDCommand(this))
                .addSubCommand("expiredstorage", new ExpiredStorageCommand(this))
                .addSubCommand("clone", new CloneCommand(this))
                .addSubCommand("setsize", new SetSizeCommand(this))
                .addSubCommand("toggleperm", new ToggleRequirePermissionCommand(this))
                .addSubCommand("open", new OpenCommand(this))
                .permissionPrefix("villagermarket.command")
                .build();

        if (!ConfigManager.getString("default_admin_shop").isEmpty()) {
            getCommand("shop").setExecutor(new ShopCommand(this));
        }
        module.register("vm");
    }

    public void reloadConfiguration() {
        reloadConfig();
        loadMappings();
    }

    /** Setup Vault integration */
    private void setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        } else {
            Bukkit.getLogger().severe("[VillagerMarket] Could not find Economy Provider!");
        }
    }

    /** Registers event listeners */
    private void registerEvents() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new EntityListener(this), this);
        pluginManager.registerEvents(playerListener, this);
    }

    @Override
    protected int getSpigotResourceID() {
        return 82965;
    }

    /** Saves log to /log/ folder and clears log */
    public void saveLog() {
        String fileName = new Date().toString().replace(":","-");
        File file = new File(getDataFolder() + "/logs/" + fileName + ".yml");
        FileConfiguration logConfig = YamlConfiguration.loadConfiguration(file);
        logConfig.set("log", log);

        try {
            logConfig.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.clear();
    }

    public ShopManager getShopManager() {
        return shopManager;
    }
    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    public String getLocalizedMaterial(String material) {
        return localizedMaterials.get(material);
    }

    public static Economy getEconomy() {
        return economy;
    }
    public boolean isCitizensEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("Citizens");
    }
}
