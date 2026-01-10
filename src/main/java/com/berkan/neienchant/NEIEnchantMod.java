package com.berkan.neienchant;

import com.berkan.neienchant.network.ApplyEnchantmentPayload;
import com.berkan.neienchant.network.OpenEnchantmentScreenPayload;
import com.berkan.neienchant.network.RemoveEnchantmentPayload;
import com.berkan.neienchant.network.ServerEnchantmentHandler;
import com.berkan.neienchant.screen.EnchantmentScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NEIEnchantMod implements ModInitializer {
    public static final String MOD_ID = "neienchant";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ScreenHandlerType<EnchantmentScreenHandler> ENCHANTMENT_SCREEN_HANDLER = 
            new ScreenHandlerType<>((syncId, inventory) -> new EnchantmentScreenHandler(syncId, inventory, -1), FeatureFlags.VANILLA_FEATURES);

    @Override
    public void onInitialize() {
        LOGGER.info("NEI Enchantments mod initializing...");

        // Register screen handler
        Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "enchantment_screen"), ENCHANTMENT_SCREEN_HANDLER);

        // Register payload types for C2S packets
        PayloadTypeRegistry.playC2S().register(OpenEnchantmentScreenPayload.ID, OpenEnchantmentScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ApplyEnchantmentPayload.ID, ApplyEnchantmentPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RemoveEnchantmentPayload.ID, RemoveEnchantmentPayload.CODEC);

        // Register server-side packet handlers
        ServerPlayNetworking.registerGlobalReceiver(OpenEnchantmentScreenPayload.ID, ServerEnchantmentHandler::handleOpenScreen);
        ServerPlayNetworking.registerGlobalReceiver(ApplyEnchantmentPayload.ID, ServerEnchantmentHandler::handleApplyEnchantment);
        ServerPlayNetworking.registerGlobalReceiver(RemoveEnchantmentPayload.ID, ServerEnchantmentHandler::handleRemoveEnchantment);

        LOGGER.info("NEI Enchantments mod initialized successfully!");
    }
}
