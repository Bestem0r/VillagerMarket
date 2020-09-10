package bestem0r.villagermarket.items;

import org.bukkit.inventory.ItemStack;

public class ItemForSale extends ItemStack {

    private double worth;

    public ItemForSale(ItemStack itemStack) {
        super(itemStack);
        worth = 0.0;
    }

    public double getWorth() {
        return worth;
    }

    public void setWorth(double worth) {
        this.worth = worth;
    }

    public ItemStack asItemStack() {
        return new ItemStack(super.clone());
    }
}
