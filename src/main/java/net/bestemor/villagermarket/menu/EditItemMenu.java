package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class EditItemMenu extends Menu {

    private final VMPlugin plugin;
    private final ShopItem shopItem;
    private final VillagerShop shop;
    private final int page;

    public EditItemMenu(VMPlugin plugin, VillagerShop shop, ShopItem shopItem, int page) {
        super(plugin.getMenuListener(), 54, ConfigManager.getString("menus.edit_item.title"));

        this.page = page;
        this.plugin = plugin;
        this.shopItem = shopItem;
        this.shop = shop;
    }

    @Override
    protected void onCreate(MenuContent content) {
        content.fillEdges(ConfigManager.getItem("items.filler").build());
        shopItem.reloadMeta(shop);
        
        content.setClickable(53, Clickable.of(ConfigManager.getItem("items.back").build(), (event) -> {
            Player player = (Player) event.getWhoClicked(); 
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            shop.getShopfrontHolder().open(player, Shopfront.Type.EDITOR, this.page);
        }));
        
        content.setClickable(49, Clickable.of(ConfigManager.getItem("menus.edit_item.items.delete").build(), (event) -> {
            Player player = (Player) event.getWhoClicked();
            
            new ConfirmActionMenu(plugin.getMenuListener(), () -> {
                shop.getShopfrontHolder().remove(shopItem.getSlot());
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.remove_item"), 1, 1);
                shop.openInventory(player, ShopMenu.EDIT_SHOPFRONT);
            }, () -> open(player)).open(player);
        }));

        update();
    }

    @Override
    protected void onUpdate(MenuContent content) {

        content.setClickable(4, Clickable.empty(shopItem.getRawItem()));
        content.setClickable(31, null);

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
            content.setClickable(32, Clickable.of(ConfigManager.getItem("menus.edit_item.items.limit_cooldown")
                    .replace("%cooldown%", shopItem.getCooldown() == null ? "none" : shopItem.getCooldown())
                    .replace("%next%", ConfigManager.getTimeLeft(shopItem.getNextReset())).build(), this::handleCooldown));

            String cycleName = shopItem.getLimitMode().name().toLowerCase();
            content.setClickable(30, Clickable.of(ConfigManager.getItem("menus.edit_item.items.player_limit")
                    .replace("%limit%", limit)
                    .replace("%cycle%", ConfigManager.getString("menus.edit_item.limit_cycle." + cycleName)).build(), this::handleLimit));

        } else if (shopItem.getMode() == ItemMode.BUY || shopItem.getMode() == ItemMode.BUY_AND_SELL) {
            content.setClickable(31, Clickable.of(ConfigManager.getItem("menus.edit_item.items.buy_limit")
                    .replace("%limit%", limit).build(), this::handleLimit));
        }

        if (shopItem.getMode() == ItemMode.COMMAND) {
            ItemStack commandItem =  ConfigManager.getItem("menus.edit_item.items.command").build();
            ItemMeta meta = commandItem.getItemMeta();
            List<String> lore = meta.getLore();

            Optional<String> commandLine = lore.stream().filter(l -> l.contains("%command%")).findFirst();
            if (commandLine.isPresent()) {
                int index = lore.indexOf(commandLine.get());
                lore.removeIf(l -> l.contains("%command%"));

                for (String command : shopItem.getCommands()) {
                    lore.add(index, commandLine.get().replace("%command%", command));
                    index++;
                }
            }
            meta.setLore(lore);
            commandItem.setItemMeta(meta);

            content.setClickable(31, Clickable.of(commandItem, (event) -> {

                if (event.getClick() == ClickType.RIGHT) {
                    shopItem.resetCommand();
                    update();
                    return;
                }

                event.getView().close();
                Player player = (Player) event.getWhoClicked();
                player.sendMessage(ConfigManager.getMessage("messages.type_command"));
                plugin.getChatListener().addStringListener(player, (result) -> {
                    shopItem.addCommand(result);
                    update();
                    open(player);
                });
            }));
        }

        content.setClickable(21, Clickable.of(amountItem, (event) -> {
            event.getView().close();
            typeAmount((Player) event.getWhoClicked());
        }));
        
        content.setClickable(22, Clickable.of(modeItem, (event) -> {
            shopItem.cycleTradeMode();
            update();
        }));
        content.setClickable(23, Clickable.of(priceItem, (event) -> {
            Player player = (Player) event.getWhoClicked(); 
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                shopItem.setItemTrade(event.getCursor().clone());
                update();
            } else {
                shopItem.setItemTrade(null);
                event.getView().close();
                typePrice(player);
            }
        }));
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        shop.getShopfrontHolder().update();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (event.getClick() != ClickType.RIGHT && event.getClick() != ClickType.LEFT) {
            return;
        }
        if (event.getRawSlot() > 53) {
            event.setCancelled(false);
        }
    }

    @Override
    protected void onDrag(InventoryDragEvent event) {
        event.setCancelled(true);
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

    private void handleLimit(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
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

    private void handleCooldown(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
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
