package com.berkan.neienchant.client;

import com.berkan.neienchant.NEIEnchantMod;
import com.berkan.neienchant.client.screen.EnchantmentMenuScreen;
import com.berkan.neienchant.network.OpenEnchantmentScreenPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

@Environment(EnvType.CLIENT)
public class NEIEnchantModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register keybinding
        ModKeybindings.register();
        
        // Register screen
        HandledScreens.register(NEIEnchantMod.ENCHANTMENT_SCREEN_HANDLER, EnchantmentMenuScreen::new);
    }
}
