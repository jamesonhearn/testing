package core.items;

public class ItemStack {
    private final Item item;
    private int quantity;

    public ItemStack(Item item, int quantity) {
        this.item = item;
        this.quantity = Math.max(0, quantity);
    }

    public Item item() {
        return item;
    }

    public int quantity() {
        return quantity;
    }

    public void setQuantity() {
        this.quantity = Math.max(0, quantity);
    }

    public int remainingCapacity() {
        return Math.max(0, item.getMaxStackSize() - quantity);
    }

    public int addQuantity(int amount) {
        int space = remainingCapacity();
        int toAdd = Math.min(space, amount);
        quantity += toAdd;
        return amount - toAdd;
    }

    public int removeQuantity(int amount) {
        int removed = Math.min(amount, quantity);
        quantity -= removed;
        return removed;
    }

    @Override
    public String toString() {
        return item.name() + " x" + quantity + " (" + item.rarity() + ")";
    }
}
