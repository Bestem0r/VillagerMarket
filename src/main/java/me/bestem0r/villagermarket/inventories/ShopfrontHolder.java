package me.bestem0r.villagermarket.inventories;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.shops.VillagerShop;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ShopfrontHolder {

    private final List<Shopfront> shopfronts = new ArrayList<>();

    private final VMPlugin plugin;
    private final VillagerShop shop;
    private final boolean isInfinite;

    private int midPages;

    public ShopfrontHolder(VMPlugin plugin, VillagerShop shop) {
        this.plugin = plugin;
        this.shop = shop;
        this.isInfinite = shop.getShopSize() == 0;
    }

    public void open(Player player, Shopfront.Type type, int page) {
        shopfronts.get(page).open(player, type);
    }
    public void open(Player player, Shopfront.Type type) {
        update();
        open(player, type, 0);
    }

    public int getSize() {
        return shopfronts.size();
    }

    public void load() {
        shopfronts.add(new Shopfront(plugin, this, shop, 0));
        this.midPages = shop.getItemList().keySet().stream().mapToInt(v -> v).max().orElse(0) / 45;
        for (int page = 1; page <= midPages; page++) {
            shopfronts.add(new Shopfront(plugin, this, shop, page));
        }
        shopfronts.add(new Shopfront(plugin, this, shop,  (midPages + 1)));
    }

    public void reload() {
        shopfronts.forEach(Shopfront::loadItemsFromConfig);
        update();
    }

    public void update() {
        if (isInfinite) {
            int updatedMidPages = shop.getItemList().keySet().stream().mapToInt(v -> v).max().orElse(0) / 45;
            if (updatedMidPages > midPages) {
                shopfronts.get(shopfronts.size() - 1).addNext();
                shopfronts.add(shopfronts.size(), new Shopfront(plugin, this, shop, shopfronts.size()));
            }
            if (updatedMidPages < midPages) {
                shopfronts.get(shopfronts.size() - 2).removeNext();
                shopfronts.remove(shopfronts.size() - 1);
            }
            this.midPages = updatedMidPages;
        }
        shopfronts.forEach(Shopfront::update);
    }
}
