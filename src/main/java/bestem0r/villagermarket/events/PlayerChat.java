package bestem0r.villagermarket.events;

import bestem0r.villagermarket.DataManager;
import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.items.ShopfrontItem;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;


public class PlayerChat implements Listener {

    private static DataManager dataManager;

    public PlayerChat(VMPlugin instance, DataManager dataManager) {
        this.dataManager = dataManager;
    }
    public static void startChatSession(Player player, String entityUUID, ItemStack itemStack, int slot) {

        player.sendMessage(new Color.Builder().path("messages.type_amount").addPrefix().build());
        player.sendMessage(new Color.Builder().path("messages.type_cancel").addPrefix().build());

        VillagerShop villagerShop = dataManager.getVillagers().get(entityUUID);

        ShopfrontItem.Builder builder = new ShopfrontItem.Builder(itemStack)
                .entityUUID(entityUUID)
                .villagerType(villagerShop.getType())
                .slot(slot);

        dataManager.addAmount(player.getUniqueId().toString(), builder);

    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        String message = event.getMessage();
        if (dataManager.getAmountHashMap().containsKey(playerUUID)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(new Color.Builder().path("messages.cancelled").addPrefix().build());
                dataManager.removeAmount(playerUUID);
                return;
            }
            if (!canConvert(message)) {
                player.sendMessage(new Color.Builder().path("messages.not_number").addPrefix().build());
                return;
            } else if (Integer.parseInt(message) > 64 || Integer.parseInt(message) < 1) {
                player.sendMessage(new Color.Builder().path("messages_not_valid_range").addPrefix().build());
                return;
            }
            ShopfrontItem.Builder builder = dataManager.getAmountHashMap().get(playerUUID);
            builder.amount(Integer.parseInt(event.getMessage()));
            dataManager.addPrice(playerUUID, builder);

            player.sendMessage(new Color.Builder().path("messages.amount_successful").addPrefix().build());
            player.sendMessage(" ");
            player.sendMessage(new Color.Builder().path("messages.type_price").addPrefix().build());
            player.sendMessage(new Color.Builder().path("messages.type_cancel").addPrefix().build());
            dataManager.removeAmount(playerUUID);
            return;
        }
        if (dataManager.getPriceHashMap().containsKey(playerUUID)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(new Color.Builder().path("messages.cancelled").addPrefix().build());
                dataManager.removePrice(playerUUID);
                return;
            }
            if (!canConvert(message)) {
                player.sendMessage(new Color.Builder().path("messages.not_number").addPrefix().build());
                return;
            } else if (Integer.parseInt(message) < 1) {
                player.sendMessage(new Color.Builder().path("messages.negative_price").addPrefix().build());
                return;
            }
            ShopfrontItem.Builder builder = dataManager.getPriceHashMap().get(playerUUID);
            builder.price(Double.parseDouble(message));

            String entityUUID = builder.getEntityUUID();
            VillagerShop villagerShop = dataManager.getVillagers().get(entityUUID);

            ShopfrontItem shopfrontItem = builder.build();
            shopfrontItem.refreshLore(villagerShop);
            villagerShop.getItemList().put(shopfrontItem.getSlot(), shopfrontItem);

            player.sendMessage(new Color.Builder().path("messages.add_successful").addPrefix().build());
            villagerShop.updateShopInventories();
            dataManager.removePrice(playerUUID);

            Bukkit.getScheduler().runTask(VMPlugin.getInstance(), () -> {
                player.openInventory(villagerShop.getInventory(VillagerShop.ShopMenu.EDIT_SHOPFRONT));
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.add_item")), 0.5f, 1);
            });
        }
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
