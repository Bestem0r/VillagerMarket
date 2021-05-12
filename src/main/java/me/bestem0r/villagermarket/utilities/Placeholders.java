package me.bestem0r.villagermarket.utilities;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.UUID;

public class Placeholders extends PlaceholderExpansion {

    private final VMPlugin plugin;

    private final String available;
    private final String bought;
    private final String noOwner;

    public Placeholders(VMPlugin plugin) {
        this.plugin = plugin;

        this.available = new ColorBuilder(plugin).path("status.available").build();
        this.bought = new ColorBuilder(plugin).path("status.bought").build();
        this.noOwner = new ColorBuilder(plugin).path("status.no_owner").build();
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
            return String.valueOf(Methods.getOwnedShops(player));
        }

        String[] split = params.split("_");
        if (split.length == 2) {
            UUID uuid = UUID.fromString(split[1]);

            VillagerShop shop = Methods.shopFromUUID(uuid);
            if (shop != null) {
                switch (split[0]) {
                    case "status":
                        return (shop.hasOwner() ? bought : available);
                    case "owner":
                        return (shop.hasOwner() ? shop.getOwnerName() : noOwner);
                    case "shopsize":
                        return String.valueOf(shop.getShopSize());
                    case "storagesize":
                        return String.valueOf(shop.getStorageSize());
                    case "expiredate":
                        Date date = new Date(shop.getExpireDate().getTime());
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
