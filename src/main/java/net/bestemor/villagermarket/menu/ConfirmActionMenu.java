/*
 * Copyright (c) 2021. Vebjørn Viem Elvekrok
 * All rights reserved.
 */

package net.bestemor.villagermarket.menu;

import net.bestemor.villagermarket.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class ConfirmActionMenu extends Menu {

    private final Runnable accept;
    private final Runnable cancel;

    public ConfirmActionMenu(MenuListener listener, Runnable accept, Runnable cancel) {
        super(listener, 27, ConfigManager.getString("menus.confirm_action.title"));
        this.accept = accept;
        this.cancel = cancel;
    }

    @Override
    protected void create(Inventory inventory) {

        fillEdges(ConfigManager.getItem("items.filler").build());

        inventory.setItem(12, ConfigManager.getItem("menus.confirm_action.items.accept").build());
        inventory.setItem(14, ConfigManager.getItem("menus.confirm_action.items.cancel").build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        switch (event.getRawSlot()) {
            case 12:
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                accept.run();
                break;
            case 14:
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                cancel.run();
                break;
        }
    }
}
