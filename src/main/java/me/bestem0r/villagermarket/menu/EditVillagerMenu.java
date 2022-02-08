package me.bestem0r.villagermarket.menu;

import me.bestem0r.villagermarket.ConfigManager;
import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.shop.ShopMenu;
import me.bestem0r.villagermarket.shop.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public class EditVillagerMenu extends Menu {

    private final VMPlugin plugin;
    private final VillagerShop shop;

    public EditVillagerMenu(VMPlugin plugin, VillagerShop shop) {
        super(plugin.getMenuListener(), 36, ConfigManager.getString("menus.edit_villager.title"));
        this.plugin = plugin;
        this.shop = shop;
    }

    @Override
    public void create(Inventory inventory) {
        Villager.Profession[] professions = Villager.Profession.values();

        ItemStack filler = ConfigManager.getItem("items.filler").build();
        fillSlots(filler, 0, 1, 2, 3, 4, 5, 6, 7, 8);
        fillBottom(filler);

        for (int i = 0; i < professions.length; i++) {
            inventory.setItem(i + 9, ConfigManager.getItem("menus.edit_villager.items." + professions[i].name().toLowerCase(Locale.ROOT)).build());
        }
        if (plugin.isCitizensEnabled()) {
            inventory.setItem(31, ConfigManager.getItem("menus.edit_villager.citizens_item").build());
        }
        inventory.setItem(35, ConfigManager.getItem("items.back").build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        if (event.getRawSlot() == 31 && Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            if (!player.hasPermission("villagermarket.use_citizens")) {
                player.sendMessage(ConfigManager.getMessage("messages.no_permission_citizens"));
                return;
            }
            player.sendMessage(ConfigManager.getMessage("messages.type_skin"));

            plugin.getChatListener().addStringListener(player, (result) -> {
                Bukkit.getScheduler().runTask(plugin, () -> shop.setCitizensSkin(result));
                player.sendMessage(ConfigManager.getMessage("messages.skin_set"));
            });

            event.getView().close();
            return;
        }

        if (event.getRawSlot() == 35) {
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.back"), 0.5f, 1);
            shop.openInventory(player, ShopMenu.EDIT_SHOP);

            return;
        }
        if (event.getRawSlot() >= Villager.Profession.values().length + 9 || event.getRawSlot() < 9) {
            return;
        }

        shop.setProfession(Villager.Profession.values()[event.getRawSlot() - 9]);
        event.getView().close();

        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.change_profession"), 0.5f, 1);
    }
}
