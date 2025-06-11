package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.core.menu.PlacedClickable;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.AdminShop;
import net.bestemor.villagermarket.shop.ItemMode;
import net.bestemor.villagermarket.shop.ShopItem;
import net.bestemor.villagermarket.shop.VillagerShop;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BuyItemMenu extends Menu {

    private final ShopItem item;
    private final Player player;
    private final ItemMode mode;

    private final VillagerShop shop;
    private final int page;

    private int amount;

    public BuyItemMenu(ShopItem item, ItemMode mode, Player player, int page) {
        super(ConfigManager.getInt("menus.buy_item.size"), ConfigManager.getString("menus.buy_item.title_" + mode.name().toLowerCase()));
        this.mode = mode;
        this.item = item;
        this.player = player;
        this.shop = item.getShop();
        this.page = page;
        this.amount = item.getAmount();
    }

    @Override
    protected void onCreate(MenuContent content) {
        int[] fillerSlots = ConfigManager.getIntegerList("menus.buy_item.filler_slots").stream().mapToInt(i -> i).toArray();
        content.fillSlots(ConfigManager.getItem("items.filler").build(), fillerSlots);

        ItemStack back = ConfigManager.getItem("items.back").build();
        content.setClickable(ConfigManager.getInt("menus.buy_item.back_slot"), Clickable.of(back, event -> {
            player.closeInventory();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.back"), 0.5f, 1);
            shop.getShopfrontHolder().open(player, Shopfront.Type.CUSTOMER, page);
        }));

        if (item.isAllowCustomAmount()) {
            content.setPlaced(PlacedClickable.fromConfig("menus.buy_item.items.increase_one", event -> {
                Bukkit.getLogger().info("Increasing amount for " + item.getItemName() + " to " + (amount + 1));
                amount++;
                update();
            }));
            content.setPlaced(PlacedClickable.fromConfig("menus.buy_item.items.decrease_one", event -> {
                amount = Math.max(amount - 1, 1);
                update();
            }));
            content.setPlaced(PlacedClickable.fromConfig("menus.buy_item.items.increase_stack", event -> {
                amount += 64;
                update();
            }));
            content.setPlaced(PlacedClickable.fromConfig("menus.buy_item.items.decrease_stack", event -> {
                amount = Math.max(amount - 64, 1);
                update();
            }));
            content.setPlaced(PlacedClickable.fromConfig("menus.buy_item.items.increase_max", event -> {
                if (amount != getMaxAmount()) {
                    amount = getMaxAmount();
                    update();
                }
            }));
        }

        if (item.getMode() == ItemMode.BUY_AND_SELL) {
            String path = "menus.buy_item.items.toggle_" + mode.inverted().name().toLowerCase();
            content.setPlaced(PlacedClickable.fromConfig(path, event -> {
                BuyItemMenu newMenu = new BuyItemMenu(item, mode.inverted(), player, page);
                newMenu.open(player);
            }));
        }
    }

    @Override
    protected void onUpdate(MenuContent content) {
        int confirmSlot = ConfigManager.getInt("menus.buy_item.confirm_slot");
        ItemStack confirmItem = item.getCustomerItem(player, amount, mode);

        content.setClickable(confirmSlot, Clickable.of(confirmItem, event -> {
            Player player = (Player) event.getWhoClicked();

            if (item.getMode() == ItemMode.COMMAND && shop instanceof AdminShop adminShop) {
                adminShop.buyCommand(player, item);
                shop.getShopfrontHolder().update();
                return;
            }

            switch (mode) {
                case BUY:
                    shop.buyItem(item, amount, player);
                    break;
                case SELL:
                    shop.sellItem(item, amount, player);
                    break;
            }
            shop.getShopfrontHolder().update();
            update();
        }));
    }

    private int getMaxAmount() {
        int maxPurchasable;
        if (mode == ItemMode.SELL) {
            maxPurchasable = VMUtils.getAmountInventory(item.getRawItem(), player.getInventory());
        } else {
            double balance = VMPlugin.getEconomy().getBalance(player);
            maxPurchasable = (int) Math.floor(balance / item.getBuyPrice(1, false).doubleValue());
            maxPurchasable = Math.min(shop.getAvailable(item), maxPurchasable);
        }
        return Math.max(1, maxPurchasable);
    }
}