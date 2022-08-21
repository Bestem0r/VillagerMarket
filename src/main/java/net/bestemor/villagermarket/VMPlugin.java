package net.bestemor.villagermarket;

import net.bestemor.core.command.CommandModule;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.MenuListener;
import net.bestemor.villagermarket.command.subcommand.*;
import net.bestemor.villagermarket.listener.ChatListener;
import net.bestemor.villagermarket.listener.EntityListener;
import net.bestemor.villagermarket.listener.PlayerListener;
import net.bestemor.villagermarket.shop.ShopManager;
import net.bestemor.villagermarket.utils.UpdateChecker;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VMPlugin extends JavaPlugin {

    private Economy econ = null;

    public static final List<String> log = new ArrayList<>();

    private boolean citizensEnabled;

    private ShopManager shopManager;
    private MenuListener menuListener;
    private ChatListener chatListener;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        setupEconomy();

        Metrics metrics = new Metrics(this, 8922);

        getConfig().options().copyDefaults();
        saveDefaultConfig();
        reloadConfiguration();

        ConfigManager.setConfig(getConfig());
        ConfigManager.setPrefixPath("plugin_prefix");

        this.chatListener = new ChatListener(this);

        setupCommands();

        this.shopManager = new ShopManager(this);
        this.menuListener = new MenuListener(this);
        shopManager.load();

        this.playerListener = new PlayerListener(this);
        registerEvents();

        Bukkit.getLogger().warning("[VillagerMarket] §cYou are running a §aBETA 1.11.0-#1 of VillagerMarket! Please expect and report all bugs in my discord server");

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Bukkit.getPluginManager().getPlugin("VillagerBank") != null) {
                Bukkit.getLogger().info("[VillagerMarket] Nice to see you Villager Bank!");
                Bukkit.getLogger().info("[VillagerBank] You too Villager Market!");
            }
        }, 31);

        new UpdateChecker(this, 82965).getVersion(version -> {
            String currentVersion = this.getDescription().getVersion();
            if (!currentVersion.equalsIgnoreCase(version)) {
                String foundVersion = ChatColor.AQUA + "A new version of VillagerMarket was found!";
                String latestVersion = ChatColor.AQUA + "Latest version: " + ChatColor.GREEN + version;
                String yourVersion = ChatColor.AQUA + "Your version: " + ChatColor.RED + currentVersion;
                String downloadVersion = ChatColor.AQUA + "Get it here for the latest features and bug fixes: " + ChatColor.YELLOW + "https://www.spigotmc.org/resources/82965/";

                getLogger().warning(foundVersion);
                getLogger().warning(latestVersion);
                getLogger().warning(yourVersion);
                getLogger().warning(downloadVersion);
            }
        });

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new PlaceholderManager(this).register();
        }

        VillagerMarketAPI.init(this);

        super.onEnable();
    }

    @Override
    public void onDisable() {

        menuListener.closeAll();
        shopManager.closeAllShopfronts();
        shopManager.saveAll();
        Bukkit.getScheduler().cancelTasks(this);
        if (getConfig().getBoolean("auto_log")) saveLog();

        super.onDisable();
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
                .addSubCommand("regen", new RegenCommand(this))
                .addSubCommand("clone", new CloneCommand(this))
                .addSubCommand("setsize", new SetSizeCommand(this))
                .permissionPrefix("villagermarket.command")
                .build();

        module.register("vm");
    }

    public void reloadConfiguration() {
        ConfigManager.clearCache();
        reloadConfig();
        ConfigManager.setConfig(getConfig());
        this.citizensEnabled = Bukkit.getPluginManager().getPlugin("Citizens") != null;
    }

    /** Setup Vault integration */
    private void setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        } else {
            Bukkit.getLogger().severe("[VillagerMarket] Could not find Economy Provider!");
        }
    }

    /** Registers event listeners */
    private void registerEvents() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new EntityListener(this), this);
        pluginManager.registerEvents(playerListener, this);
        pluginManager.registerEvents(menuListener, this);
        pluginManager.registerEvents(chatListener, this);
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
    public MenuListener getMenuListener() {
        return menuListener;
    }
    public ChatListener getChatListener() {
        return chatListener;
    }
    public PlayerListener getPlayerEvents() {
        return playerListener;
    }

    public Economy getEconomy() {
        return econ;
    }
    public boolean isCitizensEnabled() {
        return citizensEnabled;
    }
}
