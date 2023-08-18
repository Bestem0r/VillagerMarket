package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.AdminShop;
import net.bestemor.villagermarket.shop.PlayerShop;
import net.bestemor.villagermarket.shop.ShopMenu;
import net.bestemor.villagermarket.shop.VillagerShop;
import net.bestemor.villagermarket.utils.VMUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

public class EditShopMenu extends Menu {

    private final VMPlugin plugin;
    private final VillagerShop shop;

    public EditShopMenu(VMPlugin plugin, VillagerShop shop) {
        super(plugin.getMenuListener(), 54, ConfigManager.getString("menus.edit_shop.title"));
        this.plugin = plugin;
        this.shop = shop;
    }

    @Override
    protected void onUpdate(MenuContent content) {

        if (shop instanceof PlayerShop && !shop.getDuration().equals("infinite")) {

            String timeShort = shop.getDuration();
            String unit = timeShort.substring(timeShort.length() - 1);
            int amount = Integer.parseInt(timeShort.substring(0, timeShort.length() - 1));
            String time = amount + " " + ConfigManager.getUnit(unit, amount > 1);

            PlayerShop playerShop = (PlayerShop) shop;

            Clickable increaseTime = Clickable.of(ConfigManager.getItem("menus.edit_shop.items.increase_time")
                    .replace("%expire%", ConfigManager.getTimeLeft(shop.getExpireDate()))
                    .replace("%time%", time)
                    .replaceCurrency("%price%", BigDecimal.valueOf(shop.getCost())).build(), event -> {

                Instant newExpire = shop.getExpireDate().plusSeconds(playerShop.getSeconds());
                Instant max = Instant.now().plusSeconds(ConfigManager.getInt("max_rent") * 86400L);

                Player player = (Player) event.getWhoClicked();
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
            });

            content.setClickable(13, increaseTime);
        }
        if (shop instanceof PlayerShop && ConfigManager.getBoolean("require_collect")) {

            PlayerShop playerShop = (PlayerShop) shop;

            Clickable collectMoney = Clickable.of(ConfigManager.getItem("menus.edit_shop.items.collect_money")
                    .replaceCurrency("%worth%", shop.getCollectedMoney()).build(), event -> {
                playerShop.collectMoney((Player) event.getWhoClicked());
            });
            content.setClickable(40, collectMoney);
        }

        if (shop instanceof PlayerShop) {

            PlayerShop playerShop = (PlayerShop) shop;
            String path = playerShop.isDisableNotifications() ? "menus.edit_shop.items.enable_trade_notifications" :
                    "menus.edit_shop.items.disable_trade_notifications";

            Clickable tradeNotifications = Clickable.of(ConfigManager.getItem(path).build(), event -> {
                playerShop.setDisableNotifications(!playerShop.isDisableNotifications());
                update();
            });
            content.setClickable(40, tradeNotifications);
        }
    }

    @Override
    public void onCreate(MenuContent content) {

        ItemStack filler = ConfigManager.getItem("items.filler").build();

        Clickable editShopfront = Clickable.of(ConfigManager.getItem("menus.edit_shop.items.edit_shopfront").build(), event -> {
            shop.openInventory(event.getWhoClicked(), ShopMenu.EDIT_SHOPFRONT);
        });
        Clickable previewShop = Clickable.of(ConfigManager.getItem("menus.edit_shop.items.preview_shop").build(), event -> {
            shop.openInventory(event.getWhoClicked(), ShopMenu.CUSTOMER);
        });
        Clickable editVillager = Clickable.of(ConfigManager.getItem("menus.edit_shop.items.edit_villager").build(), event -> {
          shop.openInventory(event.getWhoClicked(), ShopMenu.EDIT_VILLAGER);
        });

        Clickable changeName = Clickable.of(ConfigManager.getItem("menus.edit_shop.items.change_name").build(), event -> {

            Player player = (Player) event.getWhoClicked();
            if (!player.hasPermission("villagermarket.change_name")) {
                player.sendMessage(ConfigManager.getMessage("messages.no_permission_change_name"));
                return;
            }

            event.getView().close();
            player.sendMessage(ConfigManager.getMessage("messages.change_name"));
            plugin.getChatListener().addStringListener(player, (result) -> {

                for (String word : ConfigManager.getStringList("villager.name_blacklist")) {
                    if (result.toLowerCase(Locale.ROOT).contains(word)) {
                        player.sendMessage(ConfigManager.getMessage("messages.name_blacklisted"));
                        return;
                    }
                }
                if (result.length() > ConfigManager.getInt("villager.max_name_length")) {
                    player.sendMessage(ConfigManager.getMessage("messages.max_name_length")
                            .replace("%limit%", String.valueOf(ConfigManager.getInt("villager.max_name_length"))));
                    return;
                }

                String name = ChatColor.translateAlternateColorCodes('&', result);
                String customName = shop instanceof PlayerShop ? ConfigManager.getString("villager.custom_name")
                        .replace("%player%", player.getName())
                        .replace("%custom_name%", name) : name;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Entity villager = VMUtils.getEntity(shop.getEntityUUID());
                    shop.setShopName(customName);
                    if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(villager)) {
                        CitizensAPI.getNPCRegistry().getNPC(villager).setName(customName);
                    } else if (villager != null) {
                        villager.setCustomName(customName);
                    }
                });

                player.sendMessage(ConfigManager.getMessage("messages.change_name_set").replace("%name%", name));
            });
        });

        content.fillEdges(filler);

        if (shop instanceof AdminShop) {
            content.setClickable(21, editShopfront);
            content.setClickable(23,  previewShop);
            content.setClickable(30,  editVillager);
            content.setClickable(32,  changeName);
        } else if (shop instanceof PlayerShop) {

            PlayerShop playerShop = (PlayerShop) shop;

            Clickable removeShop = Clickable.of(ConfigManager.getItem("menus.edit_shop.items.sell_shop").build(), event -> {
                shop.openInventory(event.getWhoClicked(), ShopMenu.SELL_SHOP);
            });
            Clickable storage = Clickable.of(ConfigManager.getItem("menus.edit_shop.items.edit_storage").build(), event -> {
                playerShop.openStorage((Player) event.getWhoClicked());
            });

            content.setClickable(21, editShopfront);
            content.setClickable(22, storage);
            content.setClickable(23, previewShop);
            content.setClickable(30, editVillager);
            content.setClickable(31, removeShop);
            content.setClickable(32, changeName);

            update();
        }
    }
}
