package net.bestemor.villagermarket;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.shop.PlayerShop;
import net.bestemor.villagermarket.shop.VillagerShop;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.UUID;

public class PlaceholderManager extends PlaceholderExpansion {

    private final VMPlugin plugin;

    private final String available;
    private final String bought;
    private final String noOwner;

    public PlaceholderManager(VMPlugin plugin) {
        this.plugin = plugin;

        this.available = ConfigManager.getString("status.available");
        this.bought = ConfigManager.getString("status.bought");
        this.noOwner = ConfigManager.getString("status.no_owner");
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vm";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Bestem0r";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return super.persist();
    }

    @Override
    public boolean canRegister() {
        return super.canRegister();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {

        if (params.equals("owned_shops")) {
            return String.valueOf(plugin.getShopManager().getOwnedShops(player).size());
        }
        if (params.equals("max_shops")) {
            int maxShops = plugin.getShopManager().getMaxShops(player);

            return maxShops == -1 ? ConfigManager.getString("quantity.unlimited") : String.valueOf(maxShops);
        }

        String[] split = params.split("_");
        if (split.length == 2) {
            UUID uuid = UUID.fromString(split[1]);

            VillagerShop shop = plugin.getShopManager().getShop(uuid);
            PlayerShop playerShop = null;
            if (shop instanceof PlayerShop) {
                playerShop = (PlayerShop) shop;
            }

            if (shop != null) {
                switch (split[0]) {
                    case "status":
                        return playerShop == null ? "adminshop" : playerShop.hasOwner() ? bought : available;
                    case "owner":
                        return playerShop == null ? "adminshop" : playerShop.hasOwner() ? playerShop.getOwnerName() : noOwner;
                    case "shopsize":
                        return String.valueOf(shop.getShopSize());
                    case "storagesize":
                        return String.valueOf(shop.getStorageSize());
                    case "expiredate":
                        Date date = new Date(shop.getExpireDate().toEpochMilli());
                        return date.toString();
                    case "timesrented":
                        return String.valueOf(shop.getTimesRented());
                    case "duration":
                        return String.valueOf(shop.getDuration());
                    case "cost":
                        return String.valueOf(shop.getCost());
                    case "moneycollected":
                        return shop.getCollectedMoney().stripTrailingZeros().toPlainString();
                }
            }
        }

        return "invalid syntax";
    }
}
