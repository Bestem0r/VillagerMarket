package net.bestemor.villagermarket.shop;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.ListBuilder;
import net.bestemor.core.config.VersionUtils;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.menu.EditItemMenu;
import net.bestemor.villagermarket.menu.StorageHolder;
import net.bestemor.villagermarket.utils.VMUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

import static net.bestemor.villagermarket.shop.ItemMode.*;

public class ShopItem {

    public enum LimitMode {
        SERVER,
        PLAYER
    }

    private final VMPlugin plugin;
    private final VillagerShop shop;
    private final ItemStack item;

    private final int slot;

    private boolean isAdmin;
    private boolean allowCustomAmount = true;

    private List<String> editorLore = new ArrayList<>();

    private BigDecimal sellPrice;
    private BigDecimal buyPrice;
    private int amount;
    private int itemTradeAmount = 0;
    private ItemStack itemTrade;

    private int discount = 0;
    private int limit = 0;
    private LimitMode limitMode = LimitMode.PLAYER;
    private String cooldown = "never";
    private int serverTrades = 0;
    private Instant nextReset;
    private Instant discountEnd;
    private final Map<UUID, Integer> playerLimits = new HashMap<>();

    int storageAmount = 0;
    int available = Integer.MAX_VALUE;

    private ItemMode mode = SELL;

    private final List<String> commands = new ArrayList<>();

    public ShopItem(VMPlugin plugin, VillagerShop shop, ItemStack item, int slot) {
        this.plugin = plugin;
        this.shop = shop;
        this.slot = slot;
        this.item = item;
        this.amount = item.getAmount();
    }
    public ShopItem(VMPlugin plugin, VillagerShop shop, ConfigurationSection section) {
        this.plugin = plugin;
        this.shop = shop;
        this.slot = Integer.parseInt(section.getName());

        this.item = section.getItemStack("item");
        if (item == null) {
            throw new NullPointerException("ItemStack is null!");
        }
        this.amount = section.getInt("amount") == 0 ? item.getAmount() : section.getInt("amount");

        Object trade = section.get("price");
        double d = section.getDouble("price");

        if (d != 0) {
            this.sellPrice = new BigDecimal(String.valueOf(d));
        } else if (trade instanceof ItemStack) {
            this.itemTrade = (ItemStack) trade;
            this.itemTradeAmount = section.getInt("trade_amount") == 0 ? itemTrade.getAmount() : section.getInt("trade_amount");
            this.sellPrice = BigDecimal.ZERO;
        }
        this.buyPrice = new BigDecimal(String.valueOf(section.getDouble("buy_price")));

        List<String> commands = section.getStringList("command");
        if (!commands.isEmpty()) {
            this.mode = COMMAND;
            this.commands.addAll(commands);
        }

        this.allowCustomAmount = section.getBoolean("allow_custom_amount");
        this.mode = ItemMode.valueOf(section.getString("mode"));
        this.limit = section.getInt("buy_limit");
        this.limitMode = section.getString("limit_mode") == null ? LimitMode.PLAYER : LimitMode.valueOf(section.getString("limit_mode"));
        this.cooldown = section.getString("cooldown");
        this.serverTrades = section.getInt("server_trades");

        if (this.cooldown != null && !this.cooldown.equals("never")) {
            this.nextReset = Instant.ofEpochSecond(section.getLong("next_reset"));
        }
        if (section.getConfigurationSection("discount") != null && section.getLong("discount.end") > Instant.now().getEpochSecond()) {
            this.discount = section.getInt("discount.amount");
            this.discountEnd = Instant.ofEpochSecond(section.getLong("discount.end"));
        }

        ConfigurationSection limits = section.getConfigurationSection("limits");
        if (limits != null) {
            for (String uuid : limits.getKeys(false)) {
                playerLimits.put(UUID.fromString(uuid), limits.getInt(uuid));
            }
        }
    }
    public Material getType() { return item.getType(); }
    public int getSlot() {
        return slot;
    }
    public ItemMode getMode() {
        return mode;
    }
    public int getLimit() {
        return limit;
    }
    public int getAmount() { return amount; }
    public List<String> getCommands() {
        return new ArrayList<>(commands);
    }
    public boolean isItemTrade() {
        return this.itemTrade != null;
    }
    public ItemStack getItemTrade() {
        return itemTrade;
    }
    public int getServerTrades() {
        return serverTrades;
    }
    public LimitMode getLimitMode() {
        return limitMode;
    }
    public String getCooldown() {
        return cooldown;
    }
    public Instant getNextReset() {
        return nextReset;
    }
    public int getItemTradeAmount() {
        return itemTradeAmount;
    }
    public VillagerShop getShop() {
        return shop;
    }

    public Map<UUID, Integer> getPlayerLimits() {
        return playerLimits;
    }
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
    public void setSellPrice(BigDecimal sellPrice) {
        this.sellPrice = sellPrice;
    }
    public void setBuyPrice(BigDecimal buyPrice) {
        this.buyPrice = buyPrice;
    }
    public void setLimit(int limit) {
        this.limit = limit;
    }
    public void setAmount(int amount) {
        this.item.setAmount(amount > item.getMaxStackSize() ? 1 : amount);
        this.amount = amount;
    }
    public void addCommand(String command) {
        this.mode = ItemMode.COMMAND;
        this.commands.add(command);
    }
    public void setItemTrade(ItemStack itemTrade, int amount) {
        this.itemTrade = itemTrade;
        this.itemTradeAmount = amount;
        if (itemTrade != null) {
            this.mode = SELL;
        }
    }
    public boolean isAllowCustomAmount() {
        return allowCustomAmount && !isItemTrade() && mode != COMMAND;
    }
    public void setAllowCustomAmount(boolean allowCustomAmount) {
        this.allowCustomAmount = allowCustomAmount;
    }

    public void resetCommand() {
        this.commands.clear();
    }


    public void setCooldown(String cooldown) {
        String amount = cooldown.substring(0, cooldown.length() - 1);
        String unit = cooldown.substring(cooldown.length() - 1);
        if (!VMUtils.isInteger(amount) || (!unit.equals("m") && !unit.equals("h") && !unit.equals("d"))) {
            this.cooldown = null;
            return;
        } else {
            this.cooldown = cooldown;
        }
        resetCooldown();
    }

    public void openEditor(Player player, VillagerShop shop, int page) {
        new EditItemMenu(plugin, shop, this, page).open(player);
    }

    public void cycleTradeMode() {
        if (!isItemTrade()) {
            switch (mode) {
                case SELL:
                    mode = ItemMode.BUY;
                    break;
                case BUY:
                    mode = BUY_AND_SELL;
                    break;
                case BUY_AND_SELL:
                    mode = isAdmin ? ItemMode.COMMAND : ItemMode.SELL;
                    break;
                case COMMAND:
                    mode = SELL;
            }
        }
    }
    public int getPlayerLimit(Player player) {
        return playerLimits.getOrDefault(player.getUniqueId(), 0);
    }
    public void incrementPlayerTrades(Player player) {
        playerLimits.put(player.getUniqueId(), getPlayerLimit(player) + 1);
    }
    public void incrementServerTrades() {
        serverTrades ++;
    }
    private void reloadData(VillagerShop shop) {
        if (shop instanceof PlayerShop playerShop) {
            this.storageAmount = playerShop.getStorageHolder().getAmount(item.clone());
        }
        this.available = shop.getAvailable(this);
    }

    private void resetCooldown() {
        this.nextReset = VMUtils.getTimeFromNow(cooldown);
        if (nextReset.getEpochSecond() == 0) {
            this.cooldown = null;
        }
    }
    public void setDiscount(int discount, Instant discountEnd) {
        this.discount = discount;
        this.discountEnd = discountEnd;
    }
    public int getDiscount() {
        return discount;
    }

    public Instant getDiscountEnd() {
        if (discountEnd == null || discountEnd.getEpochSecond() == 0) {
            return null;
        }
        if (discountEnd.isBefore(Instant.now())) {
            return Instant.now();
        }
        if (discountEnd.isAfter(Instant.MAX)) {
            return Instant.now();
        }
        if (discountEnd.isBefore(Instant.MIN)) {
            return Instant.now();
        }
        return discountEnd;
    }

    public void clearLimits() {
        this.playerLimits.clear();
        this.serverTrades = 0;
        resetCooldown();
    }
    public void cycleLimitMode() {
        limitMode = limitMode == LimitMode.SERVER ? LimitMode.PLAYER : LimitMode.SERVER;
    }

    public void reloadMeta(VillagerShop shop) {
        reloadData(shop);
        editorLore = getLore("edit_shopfront", mode, null, amount);
    }

    public String getItemName() {
        return getItemName(item);
    }

    public boolean verifyPurchase(Player player, ItemMode verifyMode, int amount) {
        return verifyPurchase(player, verifyMode, amount, null,null);
    }
    public boolean verifyPurchase(Player customer, ItemMode verifyMode, int amount, OfflinePlayer owner, StorageHolder storage) {

        if (owner != null && customer.getUniqueId().equals(owner.getUniqueId())) {
            customer.sendMessage(ConfigManager.getMessage("messages.cannot_" + (verifyMode == SELL ?  "buy_from" :"sell_to") + "_yourself"));
            return false;
        }
        Economy economy = VMPlugin.getEconomy();
        if (verifyMode == SELL && isItemTrade() && VMUtils.getAmountInventory(itemTrade, customer.getInventory()) < itemTradeAmount) {
            customer.sendMessage(ConfigManager.getMessage("messages.not_enough_in_inventory"));
            return false;
        }
        if (!isItemTrade() && verifyMode == SELL && storage != null && storage.getAmount(item.clone()) < amount) {
            customer.sendMessage(ConfigManager.getMessage("messages.not_enough_stock"));
            return false;
        }
        if (!isItemTrade() && verifyMode == SELL && itemTrade == null && economy.getBalance(customer) < getSellPrice().doubleValue()) {
            customer.sendMessage(ConfigManager.getMessage("messages.not_enough_money"));
            return false;
        }
        if (!isItemTrade() && verifyMode == BUY && owner != null && itemTrade == null && economy.getBalance(owner) < getBuyPrice().doubleValue()) {
            customer.sendMessage(ConfigManager.getMessage("messages.owner_not_enough_money"));
            return false;
        }
        if (verifyMode == ItemMode.BUY && VMUtils.getAmountInventory(item.clone(), customer.getInventory()) < amount) {
            customer.sendMessage(ConfigManager.getMessage("messages.not_enough_in_inventory"));
            return false;
        }
        if ((verifyMode == BUY || isItemTrade()) && amount > getAvailable()) {
            customer.sendMessage(ConfigManager.getMessage("messages.reached_" + (isItemTrade() ? "buy" : "sell") + "_limit"));
            return false;
        }
        boolean bypass = customer.hasPermission("villagermarket.bypass_limit");
        if (isAdmin && !bypass && limit > 0 && ((limitMode == LimitMode.SERVER && serverTrades >= limit) || (limitMode == LimitMode.PLAYER && getPlayerLimit(customer) >= limit))) {
            customer.sendMessage(ConfigManager.getMessage("messages.reached_" + (verifyMode == BUY ? "sell" : "buy") + "_limit"));
            return false;
        }
        return true;
    }

    private List<String> getLore(String path, ItemMode mode, Player p, int amount) {
        String typePath = (isAdmin ? "admin_shop." : "player_shop.");
        String modePath = isItemTrade() ? "trade" : mode.toString().toLowerCase();

        String reset = nextReset == null || nextReset.getEpochSecond() == 0 ? ConfigManager.getString("time.never") : ConfigManager.getTimeLeft(nextReset);
        String bought = String.valueOf(limitMode == LimitMode.SERVER || p == null ? serverTrades : getPlayerLimit(p));
        String limitInfo = limit == 0 ? ConfigManager.getString("quantity.unlimited") : String.valueOf(limit);

        String lorePath = "menus." + path + "." + typePath + (isAdmin && path.startsWith("edit") ? "standard" : modePath)  + "_lore";
        ListBuilder builder = ConfigManager.getListBuilder(lorePath)
                .replace("%amount%", String.valueOf(amount))
                .replace("%stock%", String.valueOf(storageAmount))
                .replace("%bought%", bought)
                .replace("%available%", String.valueOf(available))
                .replace("%mode%", ConfigManager.getString("menus.shopfront.modes." + modePath))
                .replace("%reset%", reset)
                .replace("%limit%", limitInfo);

        if (isItemTrade()) {
            builder.replace("%price%", getItemTradeAmount() + "x" + " " + getItemName(itemTrade));
        } else if (getSellPrice().equals(BigDecimal.ZERO)) {

            builder.replace("%price%", ConfigManager.getString("quantity.free"));
            builder.replace("%price_per_unit%", ConfigManager.getString("quantity.free"));
        } else if (mode != BUY_AND_SELL) {
            if (discount > 0) {
                ChatColor c = VMUtils.getCodeBeforePlaceholder(ConfigManager.getStringList(lorePath), "%price%");
                String prePrice = ConfigManager.getCurrencyBuilder("%price%").replaceCurrency("%price%", getSellPrice(amount, true)).build();
                String currentPrice = ConfigManager.getCurrencyBuilder("%price%").replaceCurrency("%price%", getSellPrice(amount, true)).build();
                builder.replace("%price%", "§m" + prePrice + c + " " + currentPrice);
            } else {
                builder.replaceCurrency("%price%", getSellPrice());
            }
            builder.replaceCurrency("%price_per_unit%", getSellPrice().divide(BigDecimal.valueOf(getAmount()), RoundingMode.HALF_UP));
        } else {
            boolean isCustomerMenu = path.equals("shopfront");
            if (isAdmin && !isCustomerMenu) {
                builder.replace("%price%", VMUtils.formatBuySellPrice(getBuyPrice(), getSellPrice()));
            } else if (discount > 0) {
                String preSell = ConfigManager.getCurrencyBuilder("%price%").replaceCurrency("%price%", getSellPrice(amount, false)).build();
                String currentSell = ConfigManager.getCurrencyBuilder("%price%").replaceCurrency("%price%", getSellPrice(amount, true)).build();
                String preBuy = ConfigManager.getCurrencyBuilder("%price%").replaceCurrency("%price%", getBuyPrice(amount, false)).build();
                String currentBuy = ConfigManager.getCurrencyBuilder("%price%").replaceCurrency("%price%", getBuyPrice(amount, true)).build();

                ChatColor cBuy = VMUtils.getCodeBeforePlaceholder(ConfigManager.getStringList(lorePath), "%buy_price%");
                ChatColor cSell = VMUtils.getCodeBeforePlaceholder(ConfigManager.getStringList(lorePath), "%sell_price%");
                builder.replace("%buy_price%", "§m" + (isCustomerMenu ? preSell : preBuy) + cBuy + " " + (isCustomerMenu ? currentSell : currentBuy));
                builder.replace("%sell_price%", "§m" + (isCustomerMenu ? preBuy : preSell) + cSell + " " + (isCustomerMenu ? currentBuy : currentSell));
            } else {
                builder.replaceCurrency("%buy_price%", isCustomerMenu ? getSellPrice(amount, true) : getBuyPrice(amount, true));
                builder.replaceCurrency("%sell_price%", isCustomerMenu ? getBuyPrice(amount, true) : getSellPrice(amount, true));
            }
        }
        List<String> lore = builder.build();

        if (discount > 0 && discountEnd != null) {
            lore.addAll(ConfigManager.getListBuilder("menus.shopfront.discount_lore")
                    .replace("%discount%", String.valueOf(discount))
                    .replace("%time%", ConfigManager.getTimeLeft(getDiscountEnd())).build());
        }
        if (isAdmin && limit > 0) {
            int index = lore.indexOf("%limit_lore%");
            if (index != -1) {
                lore.remove(index);
                String type = isItemTrade() ? "buy" : mode.getInteractionType();
                lore.addAll(index, ConfigManager.getListBuilder("menus.shopfront.admin_shop." + type + "_limit_lore")
                        .replace("%reset%", reset)
                        .replace("%limit%", limitInfo)
                        .replace("%bought%", bought).build());
            }
        }
        lore.remove("%limit_lore%");

        return lore;
    }

    public ItemStack getEditorItem() {
        ItemStack i = getRawItem();
        ItemMeta m = i.getItemMeta();
        if (m != null && editorLore != null) {

            m.setLore(editorLore);
            i.setItemMeta(m);
        }
        return i;
    }

    public ItemStack getCustomerItem(Player p, int amount) {
        return getCustomerItem(p, amount, mode.inverted());
    }

    public ItemStack getCustomerItem(Player p, int amount, ItemMode mode) {
        ItemStack i = getRawItem();
        i.setAmount(Math.min(amount, item.getMaxStackSize()));
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setLore(getLore("shopfront", mode, p, amount));
            i.setItemMeta(m);
        }
        return i;
    }

    public ItemStack getRawItem() {
        return item.clone();
    }

    public String getItemTradeName() {
        return getItemName(itemTrade);
    }

    public int getAvailable() {
        return available;
    }

    public BigDecimal getSellPrice() {
        return getSellPrice(amount, true);
    }
    public BigDecimal getBuyPrice() {
        return getBuyPrice(amount, true);
    }

    public BigDecimal getSellPrice(boolean applyDiscount) {
        if (sellPrice == null) {
            return BigDecimal.ZERO;
        } else if (!applyDiscount || discount <= 0) {
            return sellPrice;
        } else {
            return sellPrice.subtract(sellPrice.multiply(BigDecimal.valueOf(discount / 100.0)));
        }
    }
    public BigDecimal getBuyPrice(boolean applyDiscount) {
        if (mode != BUY_AND_SELL) {
            return sellPrice;
        } else if (buyPrice == null) {
            return BigDecimal.ZERO;
        } else if (!applyDiscount || discount <= 0) {
            return buyPrice;
        } else {
            return buyPrice.subtract(buyPrice.multiply(BigDecimal.valueOf(discount / 100.0)));
        }
    }

    public BigDecimal getBuyPrice(int amount, boolean applyDiscount) {
        if (buyPrice == null) {
            return BigDecimal.ZERO;
        }
        return getBuyPrice(applyDiscount).divide(BigDecimal.valueOf(item.getAmount()), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(amount));
    }

    public BigDecimal getSellPrice(int amount, boolean applyDiscount) {
        if (sellPrice == null) {
            return BigDecimal.ZERO;
        }
        return getSellPrice(applyDiscount).divide(BigDecimal.valueOf(item.getAmount()), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(amount));
    }

    private String getItemName(ItemStack i) {
        ItemMeta m = i.getItemMeta();
        if (m != null && m.hasDisplayName()) {
            return m.getDisplayName();
        } else if (plugin.getLocalizedMaterial(i.getType().name()) != null) {
            return plugin.getLocalizedMaterial(i.getType().name());
        } else if (m != null && VersionUtils.getMCVersion() > 11 && m.hasLocalizedName()) {
            return m.getLocalizedName();
        } else {
            return i.getType().name().replaceAll("_", " ");
        }
    }
}
