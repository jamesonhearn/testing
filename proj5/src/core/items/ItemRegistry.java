package core.items;

public final class ItemRegistry {
    public static final Item SMALL_POTION = new Item(
            "small_potion",
            "Small Potion",
            ItemRarity.COMMON,
            5,
            "Restores a small amount of health"
    );

    public static final Item TORCH = new Item(
            "torch",
            "Torch",
            ItemRarity.UNCOMMON,
            10,
            "Sheds light when equipped"
    );

    public static final Item GEMSTONE = new Item(
            "gemstone",
            "Gemstone",
            ItemRarity.RARE,
            20,
            "Valuable crafting catalyst"
    );

    private ItemRegistry() {
    }
}