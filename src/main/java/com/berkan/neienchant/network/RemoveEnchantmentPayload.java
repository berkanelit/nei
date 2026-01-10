package com.berkan.neienchant.network;

import com.berkan.neienchant.NEIEnchantMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-Server payload for removing an enchantment from the item in the enchanting slot.
 */
public record RemoveEnchantmentPayload(
        String enchantmentId
) implements CustomPayload {

    public static final Id<RemoveEnchantmentPayload> ID = new Id<>(
            Identifier.of(NEIEnchantMod.MOD_ID, "remove_enchantment")
    );

    public static final PacketCodec<RegistryByteBuf, RemoveEnchantmentPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, RemoveEnchantmentPayload::enchantmentId,
            RemoveEnchantmentPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
