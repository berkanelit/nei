package com.berkan.neienchant.mixin;

import com.berkan.neienchant.client.ModKeybindings;
import com.berkan.neienchant.client.screen.EnchantmentMenuScreen;
import com.berkan.neienchant.network.OpenEnchantmentScreenPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Shadow
    protected int x;

    @Shadow
    protected int y;
    
    @Shadow
    public abstract ScreenHandler getScreenHandler();

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(net.minecraft.client.input.KeyInput keyInput, CallbackInfoReturnable<Boolean> cir) {
        // Don't open again if the NEI enchantment screen is already open
        if ((Object) this instanceof EnchantmentMenuScreen) return;

        // Check if our keybind was pressed
        if (ModKeybindings.OPEN_ENCHANT_KEY.matchesKey(keyInput)) {
            Slot targetSlot = focusedSlot;
            
            // If focusedSlot is null (survival mode), find slot manually from mouse position
            if (targetSlot == null) {
                MinecraftClient client = MinecraftClient.getInstance();
                double mouseX = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
                double mouseY = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();
                
                // Iterate through all slots to find which one is under the mouse
                for (Slot slot : getScreenHandler().slots) {
                    int slotX = this.x + slot.x;
                    int slotY = this.y + slot.y;
                    
                    if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                        targetSlot = slot;
                        break;
                    }
                }
            }
            
            // Check if we have a slot with an item that belongs to the player inventory
            if (targetSlot != null && !targetSlot.getStack().isEmpty()) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player == null) return;

                // Only accept slots that belong to the player's own inventory
                // (excludes external inventories such as chests and furnaces)
                if (targetSlot.inventory != client.player.getInventory()) return;

                // targetSlot.index = real index within the slot's own inventory
                // targetSlot.id   = sequential number in the screen handler (shifts when external slots exist)
                int inventoryIndex = targetSlot.getIndex();

                // Send packet to server to open the screen
                ClientPlayNetworking.send(new OpenEnchantmentScreenPayload(inventoryIndex));
                cir.setReturnValue(true);
            }
        }
    }
}

