package net.bestemor.villagermarket.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class MenuListener implements Listener {

    private final Plugin plugin;
    private final List<Menu> menus = new ArrayList<>();

    public MenuListener(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Registers menu one tick later
     * @param menu Menu to register **/
    public void registerMenu(Menu menu) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> menus.add(menu), 1L);
    }

    @EventHandler (priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {

        Player player = (Player) event.getWhoClicked();

        List<Menu> menusCopy = new ArrayList<>(menus);
        for (Menu menu : menusCopy) {
            if (menu.hasPlayer(player)) {
                menu.handleClick(event);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        for (Menu menu : menus) {
            if (menu.hasPlayer(event.getPlayer())) {
                menu.onClose(event);
            }
        }
        menus.removeIf(menu -> menu.hasPlayer(event.getPlayer()) && menu.getViewers().size() == 1);
    }

    public void closeAll() {
        List<Menu> menusCopy = new ArrayList<>(menus);
        menusCopy.forEach(Menu::close);
    }
}
