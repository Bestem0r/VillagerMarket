package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.*;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

public class EditShopMenu extends Menu {

    private final VMPlugin plugin;
    private final VillagerShop shop;

    public EditShopMenu(VMPlugin plugin, VillagerShop shop) {
        super(MenuConfig.fromConfig("menus.edit_shop"));
        this.plugin = plugin;
        this.shop = shop;
    }

    @Override
    protected void onUpdate(MenuContent content) {
        if (!(shop instanceof PlayerShop)) {
            return;
        }
        PlayerShop playerShop = (PlayerShop) shop;

        String path = playerShop.isDisableNotifications() ? "menus.edit_shop.items.enable_trade_notifications" :
                "menus.edit_shop.items.disable_trade_notifications";
        Clickable tradeNotifications = Clickable.of(ConfigManager.getItem(path).build(), event -> {
            playerShop.setDisableNotifications(!playerShop.isDisableNotifications());
            update();
        });
        content.setClickable(ConfigManager.getInt(path + ".slot"), tradeNotifications);

        if (!shop.getDuration().equals("infinite")) {

            String timeShort = shop.getDuration();
            String unit = timeShort.substring(timeShort.length() - 1);
            int amount = Integer.parseInt(timeShort.substring(0, timeShort.length() - 1));
            String time = amount + " " + ConfigManager.getUnit(unit, amount > 1);

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

            content.setClickable(ConfigManager.getInt("menus.edit_shop.items.increase_time.slot"), increaseTime);
        }
        if (ConfigManager.getBoolean("require_collect")) {

            Clickable collectMoney = Clickable.of(ConfigManager.getItem("menus.edit_shop.items.collect_money")
                    .replaceCurrency("%worth%", shop.getCollectedMoney()).build(), event -> {
                playerShop.collectMoney((Player) event.getWhoClicked());
            });
            content.setClickable(ConfigManager.getInt("menus.edit_shop.items.collect_money.slot"), collectMoney);
        }
    }

    @Override
    public void onCreate(MenuContent content) {

        int[] fillerSlots = ConfigManager.getIntArray("menus.edit_shop.filler_slots");
        content.fillSlots(ConfigManager.getItem("items.filler").build(), fillerSlots);

        String slotSuffix = shop instanceof AdminShop ? "_admin" : "";

        int editSlot = ConfigManager.getInt("menus.edit_shop.items.edit_shopfront.slot" + slotSuffix);
        content.setClickable(editSlot, Clickable.fromConfig("menus.edit_shop.items.edit_shopfront", event -> {
            shop.openInventory(event.getWhoClicked(), ShopMenu.EDIT_SHOPFRONT);
        }));

        int previewSlot = ConfigManager.getInt("menus.edit_shop.items.preview_shop.slot" + slotSuffix);
        content.setClickable(previewSlot, Clickable.fromConfig("menus.edit_shop.items.preview_shop", event -> {
            shop.openInventory(event.getWhoClicked(), ShopMenu.CUSTOMER);
        }));

        int villagerSlot = ConfigManager.getInt("menus.edit_shop.items.edit_villager.slot" + slotSuffix);
        content.setClickable(villagerSlot, Clickable.fromConfig("menus.edit_shop.items.edit_villager", event -> {
          shop.openInventory(event.getWhoClicked(), ShopMenu.EDIT_VILLAGER);
        }));

        int nameSlot = ConfigManager.getInt("menus.edit_shop.items.change_name.slot" + slotSuffix);
        content.setClickable(nameSlot, Clickable.fromConfig("menus.edit_shop.items.change_name", event -> {

            Player player = (Player) event.getWhoClicked();
            if (!player.hasPermission("villagermarket.change_name")) {
                player.sendMessage(ConfigManager.getMessage("messages.no_permission_change_name"));
                return;
            }

            event.getView().close();
            player.sendMessage(ConfigManager.getMessage("messages.change_name"));
            plugin.getChatListener().addStringListener(player, (result) -> {

                for (String word : plugin.getConfig().getStringList("villager.name_blacklist")) {
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
        }));

        if (shop instanceof PlayerShop) {

            content.setPlaced(PlacedClickable.fromConfig("menus.edit_shop.items.sell_shop", event -> {
                shop.openInventory(event.getWhoClicked(), ShopMenu.SELL_SHOP);
            }));

            content.setPlaced(PlacedClickable.fromConfig("menus.edit_shop.items.edit_storage", event -> {
                PlayerShop playerShop = (PlayerShop) shop;
                playerShop.openStorage((Player) event.getWhoClicked());
            }));
        }
    }
}
