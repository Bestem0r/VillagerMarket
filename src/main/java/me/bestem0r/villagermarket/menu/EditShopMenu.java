package me.bestem0r.villagermarket.menu;

import me.bestem0r.villagermarket.ConfigManager;
import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.shop.AdminShop;
import me.bestem0r.villagermarket.shop.PlayerShop;
import me.bestem0r.villagermarket.shop.ShopMenu;
import me.bestem0r.villagermarket.shop.VillagerShop;
import net.citizensnpcs.api.CitizensAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

public class EditShopMenu extends Menu {

    private final VMPlugin plugin;
    private final VillagerShop shop;

    private ItemStack editShopfrontItem;
    private ItemStack previewShopItem;
    private ItemStack editVillagerItem;
    private ItemStack changeNameItem;

    private ItemStack storageItem;
    private ItemStack removeShopItem;
    private ItemStack collectMoneyItem;
    private ItemStack increaseTimeItem;

    public EditShopMenu(VMPlugin plugin, VillagerShop shop) {
        super(plugin.getMenuListener(), 54, ConfigManager.getString("menus.edit_shop.title"));
        this.plugin = plugin;
        this.shop = shop;
    }

    @Override
    protected void update(Inventory inventory) {

        if (shop instanceof PlayerShop && !shop.getDuration().equals("infinite")) {

            String timeShort = shop.getDuration();
            String unit = timeShort.substring(timeShort.length() - 1);
            int amount = Integer.parseInt(timeShort.substring(0, timeShort.length() - 1));
            String time = amount + " " + ConfigManager.getUnit(unit, amount > 1);

            this.increaseTimeItem = ConfigManager.getItem("menus.edit_shop.items.increase_time")
                    .replace("%expire%", ConfigManager.getTimeLeft(shop.getExpireDate()))
                    .replace("%time%", time)
                    .replaceCurrency("%price%", BigDecimal.valueOf(shop.getCost())).build();

            inventory.setItem(13, increaseTimeItem);
        }
        if (shop instanceof PlayerShop && ConfigManager.getBoolean("require_collect")) {
            this.collectMoneyItem = ConfigManager.getItem("menus.edit_shop.items.collect_money").replaceCurrency("%worth%", shop.getCollectedMoney()).build();
            inventory.setItem(40, collectMoneyItem);
        }
    }

    @Override
    public void create(Inventory inventory) {

        ItemStack filler = ConfigManager.getItem("items.filler").build();
        this.editShopfrontItem = ConfigManager.getItem("menus.edit_shop.items.edit_shopfront").build();
        this.previewShopItem = ConfigManager.getItem("menus.edit_shop.items.preview_shop").build();
        this.editVillagerItem = ConfigManager.getItem("menus.edit_shop.items.edit_villager").build();
        this.changeNameItem = ConfigManager.getItem("menus.edit_shop.items.change_name").build();
        this.removeShopItem = ConfigManager.getItem("menus.edit_shop.items.sell_shop").build();
        this.storageItem = ConfigManager.getItem("menus.edit_shop.items.edit_storage").build();

        fillEdges(filler);

        if (shop instanceof AdminShop) {
            inventory.setItem(21, editShopfrontItem);
            inventory.setItem(23, previewShopItem);
            inventory.setItem(30, editVillagerItem);
            inventory.setItem(32, changeNameItem);
        } else {
            inventory.setItem(21, editShopfrontItem);
            inventory.setItem(22, storageItem);
            inventory.setItem(23, previewShopItem);
            inventory.setItem(30, editVillagerItem);
            inventory.setItem(31, removeShopItem);
            inventory.setItem(32, changeNameItem);

            update();
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        if (event.getCurrentItem() == null) {
            return;
        }
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);

        ItemStack currentItem = event.getCurrentItem();
        if (editShopfrontItem.equals(currentItem)) {
            this.shop.openInventory(player, ShopMenu.EDIT_SHOPFRONT);
        } else if (previewShopItem.equals(currentItem)) {
            this.shop.openInventory(player, ShopMenu.CUSTOMER);
        } else if (editVillagerItem.equals(currentItem)) {
            this.shop.openInventory(player, ShopMenu.EDIT_VILLAGER);
        } else if (changeNameItem.equals(currentItem)) {
            event.getView().close();

            if (!player.hasPermission("villagermarket.change_name")) {
                player.sendMessage(ConfigManager.getMessage("messages.no_permission_change_name"));
                return;
            }

            player.sendMessage(ConfigManager.getMessage("messages.change_name"));
            plugin.getChatListener().addStringListener(player, (result) -> {

                for (String word : ConfigManager.getStringList("villager.name_blacklist")) {
                    if (result.toLowerCase(Locale.ROOT).contains(word)) {
                        player.sendMessage(ConfigManager.getMessage("messages.name_blacklisted"));
                        return;
                    }
                }

                String name = ChatColor.translateAlternateColorCodes('&', result);
                String customName = shop instanceof PlayerShop ? ConfigManager.getString("villager.custom_name")
                        .replace("%player%", player.getName())
                        .replace("%custom_name%", name) : name;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Entity villager = Bukkit.getEntity(shop.getEntityUUID());
                    if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(villager)) {
                        CitizensAPI.getNPCRegistry().getNPC(villager).setName(customName);
                    } else if (villager != null) {
                        villager.setCustomName(customName);
                    }
                });

                player.sendMessage(ConfigManager.getMessage("messages.change_name_set").replace("%name%", name));
            });
        }

        if (shop instanceof PlayerShop) {
            PlayerShop playerShop = (PlayerShop) shop;

            if (currentItem.equals(storageItem)) {
                playerShop.openStorage(player);
            } else if (currentItem.equals(collectMoneyItem)) {
                playerShop.collectMoney(player);
            } else if (currentItem.equals(increaseTimeItem)) {

                Instant newExpire = shop.getExpireDate().plusSeconds(playerShop.getSeconds());
                Instant max = Instant.now().plusSeconds(ConfigManager.getInt("max_rent") * 86400L);

                if (newExpire.isAfter(max)) {
                    player.sendMessage(ConfigManager.getMessage("messages.max_rent_time"));
                    return;
                }
                Economy economy = plugin.getEconomy();
                if (economy.getBalance(player) < playerShop.getCost()) {
                    player.sendMessage(ConfigManager.getMessage("messages.not_enough_money"));
                    return;
                }
                economy.withdrawPlayer(player, playerShop.getCost());
                playerShop.increaseTime();
                shop.updateMenu(ShopMenu.EDIT_SHOP);

                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.increase_time"), 1, 1);
            } else if (currentItem.equals(removeShopItem)) {
                playerShop.openInventory(player, ShopMenu.SELL_SHOP);
            }
        }
    }
}
