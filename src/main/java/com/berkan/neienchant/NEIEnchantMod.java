package com.berkan.neienchant;

import com.berkan.neienchant.network.ApplyEnchantmentPayload;
import com.berkan.neienchant.network.OpenEnchantmentScreenPayload;
import com.berkan.neienchant.network.RemoveEnchantmentPayload;
import com.berkan.neienchant.network.ServerEnchantmentHandler;
import com.berkan.neienchant.screen.EnchantmentScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NEIEnchantMod implements ModInitializer {
    public static final String MOD_ID = "neienchant";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ScreenHandlerType<EnchantmentScreenHandler> ENCHANTMENT_SCREEN_HANDLER = 
            new ScreenHandlerType<>((syncId, inventory) -> new EnchantmentScreenHandler(syncId, inventory, -1), FeatureFlags.VANILLA_FEATURES);

    // Track players who have seen the welcome message
    private static final Set<UUID> shownWelcomeMessage = new HashSet<>();

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

        // Register player join event to show welcome message
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID playerId = player.getUuid();
            
            // Show welcome message only once per player
            if (!shownWelcomeMessage.contains(playerId)) {
                shownWelcomeMessage.add(playerId);
                
                // Send welcome message after a short delay
                server.execute(() -> {
                    player.sendMessage(
                        Text.literal("[NEI Enchantments] ").formatted(Formatting.GOLD)
                            .append(Text.literal("Press ").formatted(Formatting.WHITE))
                            .append(Text.literal("X").formatted(Formatting.YELLOW, Formatting.BOLD))
                            .append(Text.literal(" while hovering over an item in your inventory to enchant it!").formatted(Formatting.WHITE)),
                        false
                    );
                });
            }
        });

        // Clear welcome message tracking when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            shownWelcomeMessage.clear();
        });

        LOGGER.info("NEI Enchantments mod initialized successfully!");
    }
}
