package net.bestemor.villagermarket.menu;

import net.bestemor.villagermarket.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.shop.*;
import net.bestemor.villagermarket.shop.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Locale;

public class EditItemMenu extends Menu {

    private final VMPlugin plugin;
    private final ShopItem shopItem;
    private final VillagerShop shop;

    public EditItemMenu(VMPlugin plugin, VillagerShop shop, ShopItem shopItem) {
        super(plugin.getMenuListener(), 54, ConfigManager.getString("menus.edit_item.title"));

        this.plugin = plugin;
        this.shopItem = shopItem;
        this.shop = shop;
    }

    @Override
    protected void create(Inventory inventory) {
        fillEdges(ConfigManager.getItem("items.filler").build());

        shopItem.reloadMeta(shop);
        inventory.setItem(53, ConfigManager.getItem("items.back").build());
        inventory.setItem(49, ConfigManager.getItem("menus.edit_item.items.delete").build());

        update(inventory);
    }

    @Override
    protected void update(Inventory inventory) {

        inventory.setItem(4, shopItem.getRawItem());
        inventory.setItem(31, null);

        String mode = shopItem.getMode().name().toLowerCase(Locale.ROOT);
        String modeName = ConfigManager.getString("menus.shopfront.modes." + mode);
        String modeCycle = shop.getModeCycle(mode, shopItem.isItemTrade());

        ItemStack modeItem = ConfigManager.getItem("menus.edit_item.items.mode").replace("%cycle%", modeCycle).replace("%mode%", modeName).build();
        ItemStack amountItem = ConfigManager.getItem("menus.edit_item.items.amount").replace("%amount%", String.valueOf(shopItem.getAmount())).build();
        ConfigManager.ItemBuilder priceBuilder = ConfigManager.getItem("menus.edit_item.items.price");

        if (shopItem.isItemTrade()) {
            priceBuilder.replace("%price%", shopItem.getItemTrade().getAmount() + "x " + shopItem.getItemTradeName());
        } else if (shopItem.getPrice().equals(BigDecimal.ZERO)) {
            priceBuilder.replace("%price%", ConfigManager.getString("quantity.free"));
        } else {
            priceBuilder.replaceCurrency("%price%",  shopItem.getPrice());
        }
        ItemStack priceItem = priceBuilder.build();

        if (shopItem.isItemTrade()) {
            priceItem.setAmount(shopItem.getItemTrade().getAmount());
            priceItem.setType(shopItem.getItemTrade().getType());
        }

        String limit = (shopItem.getLimit() == 0 ? ConfigManager.getString("quantity.unlimited") : String.valueOf(shopItem.getLimit()));

        if (shop instanceof AdminShop) {
            inventory.setItem(32, ConfigManager.getItem("menus.edit_item.items.limit_cooldown")
                    .replace("%cooldown%", shopItem.getCooldown() == null ? "none" : shopItem.getCooldown())
                    .replace("%next%", ConfigManager.getTimeLeft(shopItem.getNextReset())).build());

            ItemStack playerLimitItem = ConfigManager.getItem("menus.edit_item.items.player_limit").replace("%limit%", limit)
                    .replace("%cycle%", ConfigManager.getString("menus.edit_item.limit_cycle." + shopItem.getLimitMode().name().toLowerCase())).build();
            inventory.setItem(30, playerLimitItem);

        } else if (shopItem.getMode() == ItemMode.BUY) {
            ItemStack limitItem = ConfigManager.getItem("menus.edit_item.items.buy_limit").replace("%limit%", limit).build();
            inventory.setItem(31, limitItem);
        }

        if (shopItem.getMode() == ItemMode.COMMAND) {
            String command = shopItem.getCommand() == null || shopItem.getCommand().equals("") ? "none" : shopItem.getCommand();
            ItemStack commandItem =  ConfigManager.getItem("menus.edit_item.items.command").replace("%command%", command).build();
            inventory.setItem(31, commandItem);
        }

        inventory.setItem(21, amountItem);
        inventory.setItem(22, modeItem);
        inventory.setItem(23, priceItem);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        if (event.getRawSlot() < 54 || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
        } else {
            event.setCancelled(false);
        }

        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        if (slot == 53 || slot == 22) {
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
        }

        switch (slot) {
            case 53:
                shop.openInventory(player, ShopMenu.EDIT_SHOPFRONT);
                break;
            case 22:
                shopItem.cycleTradeMode();
                update();
                break;
            case 49:
                new ConfirmActionMenu(plugin.getMenuListener(), () -> {
                    shop.getShopfrontHolder().remove(shopItem.getSlot());
                    player.playSound(player.getLocation(), ConfigManager.getSound("sounds.remove_item"), 1, 1);
                    shop.openInventory(player, ShopMenu.EDIT_SHOPFRONT);
                }, () -> open(player)).open(player);

                break;
            case 21:
                event.getView().close();
                typeAmount(player);
                break;
            case 23:
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    shopItem.setItemTrade(event.getCursor().clone());
                    update();
                } else {
                    shopItem.setItemTrade(null);
                    event.getView().close();
                    typePrice(player);
                }
        }

        if (slot == 31 && shopItem.getMode() == ItemMode.COMMAND) {
            event.getView().close();
            player.sendMessage(ConfigManager.getMessage("messages.type_command"));
            plugin.getChatListener().addStringListener(player, (result) -> {
                shopItem.setCommand(result);
                update();
                open(player);
            });
        }

        if (event.getCurrentItem() != null && (shop instanceof AdminShop && slot == 30) || (shopItem.getMode() == ItemMode.BUY && slot == 31)) {

            if (shop instanceof AdminShop && event.getClick() == ClickType.RIGHT) {
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                shopItem.cycleLimitMode();
                update();
                return;
            }
            event.getView().close();

            player.sendMessage(ConfigManager.getMessage("messages.type_limit_" + (shop instanceof AdminShop ? "admin" : "player")));
            plugin.getChatListener().addDecimalListener(player, (result) -> {
                shopItem.setLimit(result.intValue());
                update();
                open(player);
            });
        }

        if (shop instanceof AdminShop && slot == 32) {

            switch (event.getClick()) {
                case LEFT:
                    event.getView().close();
                    player.sendMessage(ConfigManager.getMessage("messages.type_limit_cooldown"));
                    plugin.getChatListener().addStringListener(player, (result) -> {
                        shopItem.setCooldown(result);
                        player.sendMessage(ConfigManager.getMessage("messages.limit_cooldown_set").replace("%time%", result));
                        update();
                        open(player);
                    });
                    break;
                case RIGHT:
                    player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                    shopItem.clearLimits();
                    player.sendMessage(ConfigManager.getMessage("messages.limits_cleared"));
            }
        }
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        shop.getShopfrontHolder().update();
    }

    private void typeAmount(Player player) {
        player.sendMessage(ConfigManager.getMessage("messages.type_amount"));
        plugin.getChatListener().addDecimalListener(player, (result) -> {
            if (result.intValue() > 64 || result.intValue() < 1) {
                player.sendMessage(ConfigManager.getMessage("messages.not_valid_range"));
                return;
            }
            player.sendMessage(ConfigManager.getMessage("messages.amount_successful"));
            shopItem.setAmount(result.intValue());
            update();
            open(player);
        });
    }

    private void typePrice(Player player) {
        player.sendMessage(ConfigManager.getMessage("messages.type_price"));
        plugin.getChatListener().addDecimalListener(player, (result) -> {

            BigDecimal maxPrice = BigDecimal.valueOf(ConfigManager.getDouble("max_item_price"));
            if (!player.hasPermission("villagermarket.bypass_price") && maxPrice.doubleValue() != 0 && result.compareTo(maxPrice) > 0) {
                player.sendMessage(ConfigManager.getMessage("messages.max_item_price"));
                return;
            }

            player.sendMessage(ConfigManager.getMessage("messages.price_successful"));
            shopItem.setPrice(result);
            update();
            open(player);
        });
    }
}
