package net.bestemor.villagermarket.shop;

public enum ItemMode {
    BUY("buy"),
    SELL("buy"),
    BUY_AND_SELL("buy_and_sell"),
    COMMAND("buy");

    private final String interactionType;

    ItemMode(String interactionType) {
        this.interactionType = interactionType;
    }

    public ItemMode inverted() {
        switch (this) {
            case BUY:
                return SELL;
            case SELL:
                return BUY;
            case BUY_AND_SELL:
                return BUY_AND_SELL;
            case COMMAND:
                return COMMAND;
            default:
                return null;
        }
    }

    public String getInteractionType() {
        return interactionType;
    }
}
