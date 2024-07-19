package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.*;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.ShopMenu;
import net.bestemor.villagermarket.shop.VillagerShop;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.List;
import java.util.Locale;

public class EditVillagerMenu extends Menu {

    private final VMPlugin plugin;
    private final VillagerShop shop;

    public EditVillagerMenu(VMPlugin plugin, VillagerShop shop) {
        super(MenuConfig.fromConfig("menus.edit_villager"));
        this.plugin = plugin;
        this.shop = shop;
    }

    @Override
    public void onCreate(MenuContent content) {
        List<Villager.Profession> professions = VMUtils.getProfessions();

        int[] fillerSlots = ConfigManager.getIntArray("menus.edit_villager.filler_slots");

        content.fillSlots(ConfigManager.getItem("items.filler").build(), fillerSlots);

        for (Villager.Profession profession : professions) {
            String professionName = profession.name().toLowerCase(Locale.ROOT);
            content.setPlaced(PlacedClickable.fromConfig("menus.edit_villager.items." + professionName, event -> {
                Player player = (Player) event.getWhoClicked();
                shop.setProfession(profession);
                event.getView().close();
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.change_profession"), 0.5f, 1);
            }));
        }
        if (plugin.isCitizensEnabled()) {
            content.setPlaced(PlacedClickable.fromConfig("menus.edit_villager.citizens_item", event -> {
                Player player = (Player) event.getWhoClicked();
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
            }));
        }
        int backSlot = ConfigManager.getInt("menus.edit_villager.back_slot");
        content.setClickable(backSlot, Clickable.fromConfig("items.back", event -> {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.back"), 0.5f, 1);
            shop.openInventory(player, ShopMenu.EDIT_SHOP);
        }));
    }
}
