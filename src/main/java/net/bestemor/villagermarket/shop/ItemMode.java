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
        return switch (this) {
            case BUY -> SELL;
            case SELL -> BUY;
            case BUY_AND_SELL -> BUY_AND_SELL;
            case COMMAND -> COMMAND;
        };
    }

    public String getInteractionType() {
        return interactionType;
    }
}
