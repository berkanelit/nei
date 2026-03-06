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
            // Get the item from the source slot
            ItemStack stack = sourceSlotId >= 0 ? player.getInventory().getStack(sourceSlotId) : ItemStack.EMPTY;

            if (stack.isEmpty()) {
                player.sendMessage(Text.literal("§cNo item in this slot!"), true);
                return;
            }

            // Clear the original slot to prevent item duplication
            // The item will be returned via onClosed → giveItemStack when the screen closes
            if (sourceSlotId >= 0) {
                player.getInventory().setStack(sourceSlotId, ItemStack.EMPTY);
            }

            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, p) -> {
                        EnchantmentScreenHandler handler = new EnchantmentScreenHandler(syncId, playerInventory, sourceSlotId);
                        handler.setEnchantingStack(stack.copy());
                        return handler;
                    },
                    Text.literal("NEI Enchantment")
            ));
        });
    }

    public static void handleApplyEnchantment(ApplyEnchantmentPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();

        context.server().execute(() -> {
            if (!(player.currentScreenHandler instanceof EnchantmentScreenHandler handler)) {
                return;
            }

            int sourceSlotId = handler.getSourceSlotId();
            boolean isInVisualSlot = !handler.getEnchantingStack().isEmpty();
            ItemStack stack = isInVisualSlot ? handler.getEnchantingStack() :
                (sourceSlotId >= 0 ? player.getInventory().getStack(sourceSlotId) : ItemStack.EMPTY);

            if (stack.isEmpty()) {
                player.sendMessage(Text.literal("§cNo item in slot!"), true);
                return;
            }

            String enchantmentId = payload.enchantmentId();

            // Level validation — max 10 cap
            int level = payload.level();
            if (level < 1 || level > 10) {
                player.sendMessage(Text.literal("§cInvalid level!"), true);
                NEIEnchantMod.LOGGER.warn("Player {} tried to apply invalid level: {}", player.getName().getString(), level);
                return;
            }

            var enchantmentRegistry = context.server().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            
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

            Enchantment enchantment = enchantmentEntry.get().value();
            
            // Cap level at the enchantment's own max level
            int maxLevel = enchantment.getMaxLevel();
            if (level > maxLevel) {
                level = maxLevel;
            }
            
            // Check if the item accepts this enchantment
            if (!enchantment.isAcceptableItem(stack)) {
                player.sendMessage(Text.literal("§cThis enchantment cannot be applied to this item!"), true);
                return;
            }

            // Conflict check against existing enchantments
            ItemEnchantmentsComponent existingForConflict = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
            for (var existingEntry : existingForConflict.getEnchantmentEntries()) {
                // Skip conflict check when updating the same enchantment
                if (existingEntry.getKey().equals(enchantmentEntry.get())) continue;
                if (!Enchantment.canBeCombined(enchantmentEntry.get(), existingEntry.getKey())) {
                    player.sendMessage(Text.literal("§cThis enchantment conflicts with: §e" + getEnchantmentName(existingEntry.getKey())), true);
                    return;
                }
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
            if (!(player.currentScreenHandler instanceof EnchantmentScreenHandler handler)) {
                return;
            }

            int sourceSlotId = handler.getSourceSlotId();
            boolean isInVisualSlot = !handler.getEnchantingStack().isEmpty();
            ItemStack stack = isInVisualSlot ? handler.getEnchantingStack() : 
                (sourceSlotId >= 0 ? player.getInventory().getStack(sourceSlotId) : ItemStack.EMPTY);

            if (stack.isEmpty()) {
                player.sendMessage(Text.literal("§cNo item in slot!"), true);
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
        // Use translation key via registry key — more reliable than getName(entry, 1)
        if (entry.getKey().isPresent()) {
            Identifier id = entry.getKey().get().getValue();
            // minecraft:sharpness → enchantment.minecraft.sharpness
            String translationKey = "enchantment." + id.getNamespace() + "." + id.getPath();
            Text translated = Text.translatable(translationKey);
            String result = translated.getString();
            // If no translation found (key returned as-is), fallback to getName
            if (result.equals(translationKey)) {
                Text name = Enchantment.getName(entry, 1);
                String fullName = name.getString();
                if (fullName.endsWith(" I")) {
                    return fullName.substring(0, fullName.length() - 2);
                }
                return fullName;
            }
            return result;
        }
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
