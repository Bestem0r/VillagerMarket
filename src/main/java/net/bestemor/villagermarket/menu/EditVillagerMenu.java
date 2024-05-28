package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.ShopMenu;
import net.bestemor.villagermarket.shop.VillagerShop;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.util.List;
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
    public void onCreate(MenuContent content) {
        List<Villager.Profession> professions = VMUtils.getProfessions();

        ItemStack filler = ConfigManager.getItem("items.filler").build();
        content.fillSlots(filler, 0, 1, 2, 3, 4, 5, 6, 7, 8);
        content.fillBottom(filler);

        for (int i = 0; i < professions.size(); i++) {
            String profession = professions.get(i).name().toLowerCase(Locale.ROOT);
            int finalI = i;
            content.setClickable(i + 9, Clickable.of(ConfigManager.getItem("menus.edit_villager.items." + profession).build(), event -> {

                Player player = (Player) event.getWhoClicked();
                shop.setProfession(professions.get(finalI));
                event.getView().close();
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.change_profession"), 0.5f, 1);

            }));
        }
        if (plugin.isCitizensEnabled()) {
            content.setClickable(31, Clickable.of(ConfigManager.getItem("menus.edit_villager.citizens_item").build(), event -> {
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
        content.setClickable(35, Clickable.of(ConfigManager.getItem("items.back").build() ,event -> {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.back"), 0.5f, 1);
            shop.openInventory(player, ShopMenu.EDIT_SHOP);
        }));
    }
}
