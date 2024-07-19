/*
 * Copyright (c) 2021. Vebjørn Viem Elvekrok
 * All rights reserved.
 */

package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuConfig;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.core.menu.PlacedClickable;
import org.bukkit.entity.Player;

public class ConfirmActionMenu extends Menu {

    private final Runnable accept;
    private final Runnable cancel;

    public ConfirmActionMenu(Runnable accept, Runnable cancel) {
        super(MenuConfig.fromConfig("menus.confirm_action"));
        this.accept = accept;
        this.cancel = cancel;
    }

    @Override
    protected void onCreate(MenuContent content) {

        int[] fillerSlots = ConfigManager.getIntegerList("menus.confirm_action.filler_slots")
                .stream().mapToInt(i -> i).toArray();

        content.fillSlots(ConfigManager.getItem("items.filler").build(), fillerSlots);

        content.setPlaced(PlacedClickable.fromConfig("menus.confirm_action.items.accept", event -> {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            accept.run();
        }));
        content.setPlaced(PlacedClickable.fromConfig("menus.confirm_action.items.cancel", event -> {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            cancel.run();
        }));
    }
}
