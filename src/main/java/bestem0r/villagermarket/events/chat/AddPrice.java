package bestem0r.villagermarket.events.chat;

import bestem0r.villagermarket.DataManager;
import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.items.ShopfrontItem;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class AddPrice implements Listener {

    private Player player;
    private ShopfrontItem.Builder builder;

    public AddPrice(Player player, ShopfrontItem.Builder builder) {
        this.player = player;
        this.builder = builder;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer() != player) return;

        String message = event.getMessage();
        event.setCancelled(true);

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(new Color.Builder().path("messages.cancelled").addPrefix().build());
            HandlerList.unregisterAll(this);
            return;
        }
        if (!canConvert(message)) {
            player.sendMessage(new Color.Builder().path("messages.not_number").addPrefix().build());
            return;
        } else if (Integer.parseInt(message) < 1) {
            player.sendMessage(new Color.Builder().path("messages.negative_price").addPrefix().build());
            return;
        }
        builder.price(Double.parseDouble(message));

        String entityUUID = builder.getEntityUUID();
        VillagerShop villagerShop = VMPlugin.getDataManager().getVillagers().get(entityUUID);

        ShopfrontItem shopfrontItem = builder.build();
        shopfrontItem.refreshLore(villagerShop);
        villagerShop.getItemList().put(shopfrontItem.getSlot(), shopfrontItem);

        player.sendMessage(new Color.Builder().path("messages.add_successful").addPrefix().build());
        villagerShop.updateShopInventories();
        HandlerList.unregisterAll(this);

        Bukkit.getScheduler().runTask(VMPlugin.getInstance(), () -> {
            player.openInventory(villagerShop.getInventory(VillagerShop.ShopMenu.EDIT_SHOPFRONT));
            player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.add_item")), 0.5f, 1);
        });
    }

    private Boolean canConvert(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
