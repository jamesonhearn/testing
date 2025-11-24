package core.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Inventory {
    private final List<ItemStack> slots;

    public Inventory(int slotCount) {
        int sanitizedSlots = Math.max(1, slotCount);
        this.slots = new ArrayList<>(Collections.nCopies(sanitizedSlots, null));
    }

    public List<ItemStack> slots() {
        return Collections.unmodifiableList(slots);
    }

    public int add(Item item, int quantity) {
        int remaining = quantity;

        // Fill existing stacks first
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            ItemStack stack = slots.get(i);
            if (stack == null || !stack.item().equals(item)) {
                continue;
            }
            remaining = stack.addQuantity(remaining);
        }

        // Place into empty slots
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            if (slots.get(i) != null) {
                continue;
            }
            int toPlace = Math.min(item.getMaxStackSize(), remaining);
            slots.set(i, new ItemStack(item, toPlace));
            remaining -= toPlace;
        }

        return remaining;
    }

    public boolean remove(Item item, int quantity) {
        int remaining = quantity;
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            ItemStack stack = slots.get(i);
            if (stack == null || !stack.item().equals(item)) {
                continue;
            }
            int removed = stack.removeQuantity(remaining);
            remaining -= removed;
            if (stack.quantity() == 0) {
                slots.set(i, null);
            }
        }
        return remaining == 0;
    }

    public List<ItemStack> nonEmptySlots() {
        List<ItemStack> results = new ArrayList<>();
        for (ItemStack stack : slots) {
            if (stack != null && stack.quantity() > 0) {
                results.add(stack);
            }
        }
        return results;
    }

    /**
     * Remove and return all stored stacks, leaving the inventory empty.
     */
    public List<ItemStack> dumpAll() {
        List<ItemStack> removed = new ArrayList<>();
        for (int i = 0; i < slots.size(); i += 1) {
            ItemStack stack = slots.get(i);
            if (stack != null && stack.quantity() > 0) {
                removed.add(stack);
            }
            slots.set(i, null);
        }
        return removed;
    }
}