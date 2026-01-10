package com.berkan.neienchant.network;

import com.berkan.neienchant.NEIEnchantMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-Server payload for applying a single enchantment to the item in the enchanting slot.
 */
public record ApplyEnchantmentPayload(
        String enchantmentId,
        int level
) implements CustomPayload {

    public static final Id<ApplyEnchantmentPayload> ID = new Id<>(
            Identifier.of(NEIEnchantMod.MOD_ID, "apply_enchantment")
    );

    public static final PacketCodec<RegistryByteBuf, ApplyEnchantmentPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ApplyEnchantmentPayload::enchantmentId,
            PacketCodecs.INTEGER, ApplyEnchantmentPayload::level,
            ApplyEnchantmentPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
