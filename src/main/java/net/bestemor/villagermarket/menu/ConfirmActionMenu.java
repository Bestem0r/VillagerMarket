/*
 * Copyright (c) 2021. Vebjørn Viem Elvekrok
 * All rights reserved.
 */

package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.core.menu.MenuListener;
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
    protected void onCreate(MenuContent content) {

        content.fillEdges(ConfigManager.getItem("items.filler").build());

        content.setClickable(12, Clickable.of(ConfigManager.getItem("menus.confirm_action.items.accept").build(), (event) -> {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            accept.run();
        }));
        content.setClickable(14, Clickable.of(ConfigManager.getItem("menus.confirm_action.items.cancel").build(), (event) -> {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            cancel.run();
        }));
    }
}
