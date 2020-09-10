package bestem0r.villagermarket.events;

import bestem0r.villagermarket.*;
import bestem0r.villagermarket.items.ItemForSale;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.ColorBuilder;
import com.mojang.datafixers.util.Pair;
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
        player.sendMessage(" ");
        player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color( ColorBuilder.color("messages.type_amount")));
        player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color( "messages.type_cancel"));
        dataManager.addAmount(itemStack, player.getUniqueId().toString(), slot, entityUUID);

    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        String message = event.getMessage();
        if (dataManager.getAmountHashMap().containsKey(playerUUID)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.cancelled"));
                dataManager.removeAmount(playerUUID);
                return;
            }
            if (!canConvert(message)) {
                player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.not_number"));
                return;
            } else if (Integer.parseInt(message) > 64 || Integer.parseInt(message) < 1) {
                player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.not_valid_range"));
                return;
            }
            ItemParser itemParser = dataManager.getAmountHashMap().get(playerUUID);

            dataManager.addPrice(itemParser.getItemStack(), playerUUID, itemParser.getSlot(), itemParser.getEntityUUID(), Integer.parseInt(event.getMessage()));
            player.sendMessage(" ");
            player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.amount_successfull"));
            player.sendMessage(" ");
            player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.type_price"));
            player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.type_cancel"));
            dataManager.removeAmount(playerUUID);
            return;
        }
        if (dataManager.getPriceHashMap().containsKey(playerUUID)) {
            event.setCancelled(true);
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.cancelled"));
                dataManager.removePrice(playerUUID);
                return;
            }
            if (!canConvert(message)) {
                player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.not_number"));
                return;
            } else if (Integer.parseInt(message) < 1) {
                player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.negative_price"));
                return;
            }
            ItemParser itemParser = dataManager.getPriceHashMap().get(playerUUID);
            String entityUUID = itemParser.getEntityUUID();
            VillagerShop villagerShop = dataManager.getVillagers().get(entityUUID);

            ItemStack itemStack = itemParser.getItemStack();
            itemStack.setAmount(itemParser.getAmount());

            int slot = dataManager.getPriceHashMap().get(playerUUID).getSlot();
            ItemForSale itemForSale = new ItemForSale(itemStack);
            itemForSale.setWorth(Double.parseDouble(message));
            villagerShop.getItemsForSale().put(slot, itemForSale);

            player.sendMessage(" ");
            player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.add_successfull"));
            villagerShop.updateShopInventories();
            dataManager.removePrice(playerUUID);

            Bukkit.getScheduler().runTask(VMPlugin.getInstance(), () -> {
                player.openInventory(villagerShop.getInventory(VillagerShop.ShopInventory.EDIT_FOR_SALE));
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.add_item")), 0.5f, 1);
            });
            return;
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
