package net.bestemor.villagermarket.shop;

public enum ItemMode {
    BUY,
    SELL,
    BUY_AND_SELL,
    COMMAND;

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
}
