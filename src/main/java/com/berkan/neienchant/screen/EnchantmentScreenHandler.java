package com.berkan.neienchant.screen;

import com.berkan.neienchant.NEIEnchantMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class EnchantmentScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final int sourceSlotId;

    public EnchantmentScreenHandler(int syncId, PlayerInventory playerInventory, int sourceSlotId) {
        super(NEIEnchantMod.ENCHANTMENT_SCREEN_HANDLER, syncId);
        this.inventory = new SimpleInventory(1) {
            @Override
            public void markDirty() {
                super.markDirty();
                EnchantmentScreenHandler.this.onContentChanged(this);
            }
        };
        this.sourceSlotId = sourceSlotId;

        // Item slot - positioned to match GUI drawing (x=8, y=20 relative to GUI)
        this.addSlot(new EnchantableSlot(inventory, 0, 8, 20));

        // Player inventory (3 rows) - standard position
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    public int getSourceSlotId() {
        return sourceSlotId;
    }

    public ItemStack getEnchantingStack() {
        return this.inventory.getStack(0);
    }

    public void setEnchantingStack(ItemStack stack) {
        this.inventory.setStack(0, stack);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Clean up the temporary inventory without dropping items to prevent duplication
        this.inventory.setStack(0, ItemStack.EMPTY);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex == 0) {
                // Move from enchanting slot to player inventory
                if (!this.insertItem(originalStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to enchanting slot
                if (!this.insertItem(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    // Custom slot that accepts enchantable items
    private static class EnchantableSlot extends Slot {
        public EnchantableSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }
    }
}
