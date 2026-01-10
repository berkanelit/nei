package com.berkan.neienchant.network;

import com.berkan.neienchant.NEIEnchantMod;
import com.berkan.neienchant.screen.EnchantmentScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class ServerEnchantmentHandler {

    public static void handleOpenScreen(OpenEnchantmentScreenPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        int sourceSlotId = payload.sourceSlotId();

        context.server().execute(() -> {
            // Check permissions: Creative mode or OP level 2+
            if (!player.getAbilities().creativeMode) {
                player.sendMessage(Text.literal("§cBu özelliği kullanmak için Creative mod veya OP yetkisine sahip olmalısınız!"), true);
                return;
            }

            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, p) -> {
                        EnchantmentScreenHandler handler = new EnchantmentScreenHandler(syncId, playerInventory, sourceSlotId);
                        // Do NOT copy or set the stack here if you don't want a "phantom" item in the visual slot.
                        // Instead, the handler will refer to the item already in the player's inventory.
                        return handler;
                    },
                    Text.literal("NEI Enchantment")
            ));
        });
    }

    public static void handleApplyEnchantment(ApplyEnchantmentPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();

        context.server().execute(() -> {
            // Check permissions: Creative mode only
            if (!player.getAbilities().creativeMode) {
                player.sendMessage(Text.literal("§cBu özelliği kullanmak için Creative mod veya OP yetkisine sahip olmalısınız!"), true);
                return;
            }

            if (!(player.currentScreenHandler instanceof EnchantmentScreenHandler handler)) {
                return;
            }

            int sourceSlotId = handler.getSourceSlotId();
            boolean isInVisualSlot = !handler.getEnchantingStack().isEmpty();
            ItemStack stack = isInVisualSlot ? handler.getEnchantingStack() : 
                (sourceSlotId >= 0 ? player.getInventory().getStack(sourceSlotId) : ItemStack.EMPTY);

            if (stack.isEmpty()) {
                player.sendMessage(Text.literal("§cSlotta eşya yok!"), true);
                return;
            }

            String enchantmentId = payload.enchantmentId();
            int level = Math.min(10, Math.max(1, payload.level()));

            var enchantmentRegistry = context.server().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
            
            Identifier id = Identifier.tryParse(enchantmentId);
            if (id == null) {
                player.sendMessage(Text.literal("§cInvalid enchantment ID!"), true);
                return;
            }

            Optional<RegistryEntry.Reference<Enchantment>> enchantmentEntry = enchantmentRegistry.getEntry(id);
            if (enchantmentEntry.isEmpty()) {
                player.sendMessage(Text.literal("§cEnchantment not found!"), true);
                return;
            }

            // Get current enchantments and add/update the new one
            ItemEnchantmentsComponent currentEnchants = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
            
            // Copy existing enchantments except the one we're adding
            for (var entry : currentEnchants.getEnchantmentEntries()) {
                if (!entry.getKey().getKey().get().getValue().toString().equals(enchantmentId)) {
                    builder.add(entry.getKey(), entry.getIntValue());
                }
            }
            
            // Add the new enchantment
            builder.add(enchantmentEntry.get(), level);

            stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
            
            if (isInVisualSlot) {
                handler.setEnchantingStack(stack);
            } else if (sourceSlotId >= 0 && sourceSlotId < player.getInventory().size()) {
                player.getInventory().setStack(sourceSlotId, stack.copy());
            }

            player.sendMessage(Text.literal("§a" + getEnchantmentName(enchantmentEntry.get()) + " " + toRoman(level) + " applied!"), true);
            NEIEnchantMod.LOGGER.info("Player {} applied {} {} to item", player.getName().getString(), enchantmentId, level);
        });
    }

    public static void handleRemoveEnchantment(RemoveEnchantmentPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();

        context.server().execute(() -> {
            // Check permissions: Creative mode only
            if (!player.getAbilities().creativeMode) {
                player.sendMessage(Text.literal("§cBu özelliği kullanmak için Creative mod veya OP yetkisine sahip olmalısınız!"), true);
                return;
            }

            if (!(player.currentScreenHandler instanceof EnchantmentScreenHandler handler)) {
                return;
            }

            int sourceSlotId = handler.getSourceSlotId();
            boolean isInVisualSlot = !handler.getEnchantingStack().isEmpty();
            ItemStack stack = isInVisualSlot ? handler.getEnchantingStack() : 
                (sourceSlotId >= 0 ? player.getInventory().getStack(sourceSlotId) : ItemStack.EMPTY);

            if (stack.isEmpty()) {
                player.sendMessage(Text.literal("§cSlotta eşya yok!"), true);
                return;
            }

            String enchantmentId = payload.enchantmentId();

            // Get current enchantments and remove the specified one
            ItemEnchantmentsComponent currentEnchants = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
            
            boolean found = false;
            for (var entry : currentEnchants.getEnchantmentEntries()) {
                if (entry.getKey().getKey().isPresent() && 
                    entry.getKey().getKey().get().getValue().toString().equals(enchantmentId)) {
                    found = true;
                    continue; // Skip this enchantment (remove it)
                }
                builder.add(entry.getKey(), entry.getIntValue());
            }

            if (!found) {
                player.sendMessage(Text.literal("§cEnchantment not found!"), true);
                return;
            }

            stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
            
            if (isInVisualSlot) {
                handler.setEnchantingStack(stack);
            } else if (sourceSlotId >= 0 && sourceSlotId < player.getInventory().size()) {
                player.getInventory().setStack(sourceSlotId, stack.copy());
            }

            player.sendMessage(Text.literal("§eEnchantment removed!"), true);
            NEIEnchantMod.LOGGER.info("Player {} removed {} from item", player.getName().getString(), enchantmentId);
        });
    }

    private static String getEnchantmentName(RegistryEntry<Enchantment> entry) {
        Text name = Enchantment.getName(entry, 1);
        String fullName = name.getString();
        if (fullName.endsWith(" I")) {
            return fullName.substring(0, fullName.length() - 2);
        }
        return fullName;
    }

    private static String toRoman(int number) {
        if (number <= 0 || number > 10) return String.valueOf(number);
        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return romans[number - 1];
    }
}
