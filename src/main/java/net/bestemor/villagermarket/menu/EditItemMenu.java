package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.ItemBuilder;
import net.bestemor.core.menu.*;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.event.interact.DeleteShopItemEvent;
import net.bestemor.villagermarket.event.interact.EditShopItemEvent;
import net.bestemor.villagermarket.event.interact.enums.EditType;
import net.bestemor.villagermarket.shop.*;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class EditItemMenu extends Menu {

    private final VMPlugin plugin;
    private final ShopItem item;
    private final VillagerShop shop;
    private final int page;

    public EditItemMenu(VMPlugin plugin, VillagerShop shop, ShopItem item, int page) {
        super(MenuConfig.fromConfig("menus.edit_item"));

        this.page = page;
        this.plugin = plugin;
        this.item = item;
        this.shop = shop;
    }

    @Override
    protected void onCreate(MenuContent content) {
        int[] slots = ConfigManager.getIntArray("menus.edit_item.filler_slots");
        content.fillSlots(ConfigManager.getItem("items.filler").build(), slots);
        item.reloadMeta(shop);

        int backSlot = ConfigManager.getInt("menus.edit_item.back_slot");
        content.setClickable(backSlot, Clickable.fromConfig("items.back", event -> {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.back"), 0.5f, 1);
            shop.getShopfrontHolder().open(player, Shopfront.Type.EDITOR, this.page);
        }));

        content.setPlaced(PlacedClickable.fromConfig("menus.edit_item.items.delete", event -> {
            Player player = (Player) event.getWhoClicked();

            new ConfirmActionMenu(() -> {
                shop.getShopfrontHolder().removeItem(item.getSlot());
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.remove_item"), 1, 1);
                shop.openInventory(player, ShopMenu.EDIT_SHOPFRONT);
                DeleteShopItemEvent deleteShopItemEvent = new DeleteShopItemEvent(player, this.shop, item);
                Bukkit.getPluginManager().callEvent(deleteShopItemEvent);
            }, () -> open(player)).open(player);
        }));

        update();
    }

    @Override
    protected void onUpdate(MenuContent content) {
        String p = "menus.edit_item.items.";

        int slot = ConfigManager.getInt("menus.edit_item.item_slot");
        content.setClickable(slot, Clickable.empty(item.getRawItem()));

        int cooldownSlot = ConfigManager.getInt(p + "limit_cooldown.slot");
        int commandSlot = ConfigManager.getInt(p + "command.slot");
        int buyLimitSlot = ConfigManager.getInt(p + "buy_limit.slot");
        int playerLimitSlot = ConfigManager.getInt(p + "player_limit.slot");

        content.setClickable(cooldownSlot, null);
        content.setClickable(commandSlot, null);
        content.setClickable(buyLimitSlot, null);
        content.setClickable(playerLimitSlot, null);

        String mode = item.getMode().name().toLowerCase(Locale.ROOT);
        String modeName = ConfigManager.getString("menus.shopfront.modes." + mode);
        String modeCycle = shop.getModeCycle(mode, item.isItemTrade());

        ItemStack modeItem = ConfigManager.getItem(p + "mode").replace("%cycle%", modeCycle).replace("%mode%", modeName).build();
        ItemStack amountItem = ConfigManager.getItem(p + "amount").replace("%amount%", String.valueOf(item.getAmount())).build();
        ItemBuilder priceBuilder = ConfigManager.getItem(p + "price");

        if (item.isItemTrade()) {
            priceBuilder.replace("%price%", item.getItemTradeAmount() + "x " + item.getItemTradeName());
        } else if (item.getSellPrice().equals(BigDecimal.ZERO)) {
            priceBuilder.replace("%price%", ConfigManager.getString("quantity.free"));
        } else if (item.getMode() == ItemMode.BUY_AND_SELL) {
            priceBuilder.replace("%price%", VMUtils.formatBuySellPrice(item.getBuyPrice(false), item.getSellPrice(false)));
        } else {
            priceBuilder.replaceCurrency("%price%", item.getSellPrice(false));
        }
        ItemStack priceItem = priceBuilder.build();
        if (item.getMode() == ItemMode.BUY_AND_SELL) {
            ItemMeta meta = priceItem.getItemMeta();
            meta.setLore(ConfigManager.getStringList(p + "price.buy_and_sell_lore"));
            priceItem.setItemMeta(meta);
        }

        if (item.isItemTrade()) {
            priceItem.setAmount(item.getItemTradeAmount() > item.getItemTrade().getAmount() ? 1 : item.getItemTrade().getAmount());
            priceItem.setType(item.getItemTrade().getType());
        }

        String limit = (item.getLimit() == 0 ? ConfigManager.getString("quantity.unlimited") : String.valueOf(item.getLimit()));

        if (shop instanceof AdminShop) {
            ItemStack limitItem = ConfigManager.getItem(p + "limit_cooldown")
                    .replace("%cooldown%", item.getCooldown() == null ? "none" : item.getCooldown())
                    .replace("%next%", ConfigManager.getTimeLeft(item.getNextReset())).build();
            content.setClickable(cooldownSlot, Clickable.of(limitItem, this::handleCooldown));

            String cycleName = item.getLimitMode().name().toLowerCase();
            content.setClickable(playerLimitSlot, Clickable.of(ConfigManager.getItem(p + "player_limit")
                    .replace("%limit%", limit)
                    .replace("%cycle%", ConfigManager.getString("menus.edit_item.limit_cycle." + cycleName)).build(), this::handleLimit));

        } else if (item.getMode() == ItemMode.BUY || item.getMode() == ItemMode.BUY_AND_SELL) {
            content.setClickable(buyLimitSlot, Clickable.of(ConfigManager.getItem(p + "buy_limit")
                    .replace("%limit%", limit).build(), this::handleLimit));
        }

        if (item.getMode() == ItemMode.COMMAND) {
            ItemStack commandItem = ConfigManager.getItem(p + "command").build();
            ItemMeta meta = commandItem.getItemMeta();
            List<String> lore = meta.getLore();

            Optional<String> commandLine = lore.stream().filter(l -> l.contains("%command%")).findFirst();
            if (commandLine.isPresent()) {
                int index = lore.indexOf(commandLine.get());
                lore.removeIf(l -> l.contains("%command%"));

                for (String command : item.getCommands()) {
                    lore.add(index, commandLine.get().replace("%command%", command));
                    index++;
                }
            }
            meta.setLore(lore);
            commandItem.setItemMeta(meta);

            content.setClickable(commandSlot, Clickable.of(commandItem, event -> {

                if (event.getClick() == ClickType.RIGHT) {
                    item.resetCommand();
                    update();
                    return;
                }

                event.getView().close();
                Player player = (Player) event.getWhoClicked();
                player.sendMessage(ConfigManager.getMessage("messages.type_command"));
                plugin.getChatListener().addStringListener(player, result -> {
                    item.addCommand(result);
                    update();
                    open(player);
                });
            }));
        }

        content.setClickable(ConfigManager.getInt(p + "amount.slot"), Clickable.of(amountItem, event -> {
            event.getView().close();
            typeAmount((Player) event.getWhoClicked());
        }));

        content.setClickable(ConfigManager.getInt(p + "mode.slot"), Clickable.of(modeItem, event -> {
            item.cycleTradeMode();
            update();
        }));

        content.setPlaced(PlacedClickable.fromConfig(p + "custom_amount_" + (item.isAllowCustomAmount() ? "enabled" : "disabled"), event -> {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            item.setAllowCustomAmount(!item.isAllowCustomAmount());
            update();
        }));

        content.setClickable(ConfigManager.getInt(p + "price.slot"), Clickable.of(priceItem, event -> {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                //Set Permissions
                if (player.hasPermission("villagermarket.set_trade_type")) {
                    player.sendMessage(ConfigManager.getMessage("messages.type_amount"));
                    ItemStack clone = event.getCursor().clone();
                    event.getView().close();
                    plugin.getChatListener().addDecimalListener(player, (amount -> {
                        if (amount.intValue() > ConfigManager.getInt("max_sell_amount") || amount.intValue() < 1) {
                            player.sendMessage(ConfigManager.getMessage("messages.not_valid_range"));
                            return;
                        }
                        clone.setAmount(amount.intValue() > clone.getMaxStackSize() ? 1 : amount.intValue());
                        item.setItemTrade(clone, amount.intValue());
                        update();
                        open(player);
                    }));
                } else {
                    player.sendMessage(ConfigManager.getMessage("messages.no_permission_trade_item"));
                }

            } else {
                item.setItemTrade(null, 0);
                event.getView().close();
                typePrice(player, item.getMode() == ItemMode.BUY_AND_SELL && !event.getClick().isRightClick());
            }
        }));

        if (!item.isItemTrade()) {
            ItemStack discountItem = ConfigManager.getItem(p + "discount")
                    .replace("%discount%", String.valueOf(item.getDiscount()))
                    .replace("%discount_end%", ConfigManager.getTimeLeft(item.getDiscountEnd())).build();

            content.setClickable(ConfigManager.getInt(p + "discount.slot"), Clickable.of(discountItem, event -> {
                event.getView().close();
                typeDiscount((Player) event.getWhoClicked());
            }));
        }
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
        if (event.getRawSlot() > event.getView().getTopInventory().getSize() - 1) {
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
            if (result.intValue() > ConfigManager.getInt("max_sell_amount") || result.intValue() < 1) {
                player.sendMessage(ConfigManager.getMessage("messages.not_valid_range"));
                return;
            }
            EditShopItemEvent editShopItemEvent = new EditShopItemEvent(player, this.shop, item, result, EditType.AMOUNT);
            Bukkit.getPluginManager().callEvent(editShopItemEvent);
            if (editShopItemEvent.isCancelled()) {
                return;
            }
            player.sendMessage(ConfigManager.getMessage("messages.amount_successful"));
            item.setAmount(result.intValue());
            update();
            open(player);
        });
    }

    private void typePrice(Player player, boolean isBuy) {
        player.sendMessage(ConfigManager.getMessage("messages.type_price"));
        plugin.getChatListener().addDecimalListener(player, (result) -> {

            BigDecimal maxPrice = BigDecimal.valueOf(ConfigManager.getDouble("max_item_price"));
            if (!player.hasPermission("villagermarket.bypass_price") && maxPrice.doubleValue() != 0 && result.compareTo(maxPrice) > 0) {
                player.sendMessage(ConfigManager.getMessage("messages.max_item_price"));
                return;
            }
            EditShopItemEvent editShopItemEvent = new EditShopItemEvent(player, this.shop, item, result, EditType.PRICE);
            Bukkit.getPluginManager().callEvent(editShopItemEvent);
            if (editShopItemEvent.isCancelled()) {
                return;
            }
            player.sendMessage(ConfigManager.getMessage("messages.price_successful"));
            if (isBuy) {
                item.setBuyPrice(result);
            } else {
                item.setSellPrice(result);
            }
            update();
            open(player);
        });
    }

    private void typeDiscount(Player player) {
        player.sendMessage(ConfigManager.getMessage("messages.type_discount"));
        plugin.getChatListener().addStringListener(player, (amountS) -> {
            if (!VMUtils.isInteger(amountS)) {
                player.sendMessage(ConfigManager.getMessage("messages.not_number"));
                return;
            }
            int discount = Integer.parseInt(amountS);
            if (discount < 0 || discount > 100) {
                typeDiscount(player);
                return;
            }
            if (discount == 0) {
                item.setDiscount(0, Instant.MIN);
                update();
                open(player);
                return;
            }
            EditShopItemEvent editShopItemEvent = new EditShopItemEvent(player, this.shop, item, new BigDecimal(discount), EditType.DISCOUNT);
            Bukkit.getPluginManager().callEvent(editShopItemEvent);
            if (editShopItemEvent.isCancelled()) {
                return;
            }

            player.sendMessage(ConfigManager.getMessage("messages.type_time"));
            plugin.getChatListener().addStringListener(player, (timeS) -> {
                String amount = timeS.substring(0, timeS.length() - 1);
                String unit = timeS.substring(timeS.length() - 1);
                if (!VMUtils.isInteger(amount) || (!unit.equals("d") && !unit.equals("h") && !unit.equals("m"))) {
                    player.sendMessage(ConfigManager.getMessage("messages.not_valid_time"));
                    return;
                }
                item.setDiscount(discount, VMUtils.getTimeFromNow(timeS));
                update();
                open(player);
            });
        });
    }

    private void handleLimit(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (shop instanceof AdminShop && event.getClick() == ClickType.RIGHT) {
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            item.cycleLimitMode();
            update();
            return;
        }
        event.getView().close();

        player.sendMessage(ConfigManager.getMessage("messages.type_limit_" + (shop instanceof AdminShop ? "admin" : "player")));
        plugin.getChatListener().addDecimalListener(player, (result) -> {
            item.setLimit(result.intValue());
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
                    item.setCooldown(result);
                    player.sendMessage(ConfigManager.getMessage("messages.limit_cooldown_set").replace("%time%", result));
                    update();
                    open(player);
                });
                break;
            case RIGHT:
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                item.clearLimits();
                player.sendMessage(ConfigManager.getMessage("messages.limits_cleared"));
        }
    }
}
