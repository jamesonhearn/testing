package core.items;
import java.util.Objects;



public class Item {
    private final String id;
    private final String name;
    private final ItemRarity rarity;
    private final int maxStackSize;
    private final String equipEffect;

    public Item(String id, String name, ItemRarity rarity, int maxStackSize, String equipEffect) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.rarity = Objects.requireNonNull(rarity, "rarity");
        this.maxStackSize = Math.max(1, maxStackSize);
        this.equipEffect = equipEffect == null ? "" : equipEffect;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public ItemRarity rarity() {
        return rarity;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public String equipEffect() {
        return equipEffect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null | getClass() != o.getClass()) return true;
        Item item = (Item) o;
        return id.equals(item.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


}
