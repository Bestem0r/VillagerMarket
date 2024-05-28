package net.bestemor.villagermarket.menu;

import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.AdminShop;
import net.bestemor.villagermarket.shop.ShopItem;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShopfrontHolder {

    private final List<Shopfront> shopfronts = new ArrayList<>();

    private final VMPlugin plugin;
    private final VillagerShop shop;
    private final boolean isInfinite;

    private final Map<Integer, ShopItem> itemList = new ConcurrentHashMap<>();
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
        open(player, type, 0);
    }

    public int getSize() {
        return shopfronts.size();
    }

    public void load() {
        this.loadItems();
        this.shopfronts.add(new Shopfront(plugin, this, shop, 0));
        this.midPages = itemList.keySet().stream().mapToInt(v -> v).max().orElse(0) / 45;
        for (int page = 1; page <= midPages; page++) {
            this.shopfronts.add(new Shopfront(plugin, this, shop, page));
        }
        this.shopfronts.add(new Shopfront(plugin, this, shop,  (midPages + 1)));
        shopfronts.forEach(Shopfront::update);
    }

    private void loadItems() {
        ConfigurationSection section = shop.getConfig().getConfigurationSection("items_for_sale");
        if (section == null) { return; }

        for (String slot : section.getKeys(false)) {
            ItemStack itemStack = shop.getConfig().getItemStack("items_for_sale." + slot + ".item");
            if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getItemMeta() == null) {
                Bukkit.getLogger().severe("[VM] Skipping corrupt item while loading shop " + shop.getConfig() + "! Please check the shop file!");
            } else {
                ShopItem shopItem = new ShopItem(plugin, section.getConfigurationSection(slot));
                shopItem.setAdmin(shop instanceof AdminShop);
                itemList.put(Integer.parseInt(slot), shopItem);
            }
        }
    }

    public void closeAll() {
        shopfronts.forEach(Shopfront::close);
    }

    public void clear() {
        this.itemList.clear();
        update();
    }

    public void update() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isInfinite) {
                int updatedMidPages = getItemList().keySet().stream().mapToInt(v -> v).max().orElse(0) / 45;
                if (updatedMidPages > midPages) {
                    shopfronts.add(shopfronts.size(), new Shopfront(plugin, this, shop, shopfronts.size()));
                }
                if (updatedMidPages < midPages) {
                    shopfronts.remove(shopfronts.size() - 1);
                }
                this.midPages = updatedMidPages;
            }
            try {
                List<Shopfront> shopfronts = new ArrayList<>(this.shopfronts);
                shopfronts.forEach(Shopfront::update);
            } catch (Exception e) {
                Bukkit.getLogger().severe("An error occurred while updating shopfront " + shop.getEntityUUID().toString());
                e.printStackTrace();
            }
        });
    }


    /**
     * Returns a copy of the item list.
     * @return a copy of the item list.
     */
    public Map<Integer, ShopItem> getItemList() {
        return new ConcurrentHashMap<>(itemList);
    }

    /**
     * Removes an item from the item list.
     * @param slot the slot of the item to remove.
     */
    public void removeItem(int slot) {
        itemList.remove(slot);
        update();
    }

    /**
     * Adds an item to the item list.
     * @param slot the slot to add the item to.
     * @param item the item to add.
     */
    public void addItem(int slot, ShopItem item) {
        itemList.put(slot, item);
        update();
    }
}
