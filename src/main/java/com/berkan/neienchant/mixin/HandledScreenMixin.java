package com.berkan.neienchant.mixin;

import com.berkan.neienchant.client.ModKeybindings;
import com.berkan.neienchant.network.OpenEnchantmentScreenPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
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

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // Check if our keybind was pressed
        if (ModKeybindings.OPEN_ENCHANT_KEY.matchesKey(keyCode, scanCode)) {
            // Check if we're hovering over a slot with an item
            if (focusedSlot != null && focusedSlot.hasStack()) {
                int slotId = focusedSlot.id;

                // Send packet to server to open the screen
                ClientPlayNetworking.send(new OpenEnchantmentScreenPayload(slotId));
                cir.setReturnValue(true);
            }
        }
    }
}

