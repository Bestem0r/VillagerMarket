package bestem0r.villagermarket;

import org.bukkit.inventory.ItemStack;

public class ItemParser {
    private ItemStack itemStack;
    private int slot;
    private String entityUUID;
    private int amount;

    public ItemParser(ItemStack itemStack, int slot, String UUID, int amount) {
        this.itemStack = itemStack;
        this.slot = slot;
        this.entityUUID = UUID;
        this.amount = amount;
    }
    public ItemStack getItemStack() {
        return itemStack;
    }
    public int getSlot() {
        return slot;
    }
    public String getEntityUUID() {
        return entityUUID;
    }
    public int getAmount() {
        return amount;
    }
}
