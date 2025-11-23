package core.items;

public class DroppedItem {
    private final Item item;
    private int quantity;
    private final int x;
    private final int y;

    public DroppedItem(Item item, int quantity, int x, int y) {
        this.item = item;
        this.quantity = Math.max(1, quantity);
        this.x = x;
        this.y = y;
    }

    public Item item() {
        return item;
    }

    public int quantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }
}