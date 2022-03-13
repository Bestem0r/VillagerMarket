package net.bestemor.villagermarket;

import net.bestemor.villagermarket.shop.ShopManager;

public class VillagerMarketAPI {

    private static VMPlugin plugin = null;

    private VillagerMarketAPI() {}

    protected static void init(VMPlugin plugin) {
        VillagerMarketAPI.plugin = plugin;
    }


    public static ShopManager getShopManager() {
        return plugin.getShopManager();
    }
}
