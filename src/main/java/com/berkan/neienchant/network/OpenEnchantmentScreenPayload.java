package com.berkan.neienchant.network;

import com.berkan.neienchant.NEIEnchantMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-Server payload for requesting to open the enchantment screen.
 */
public record OpenEnchantmentScreenPayload(
        int sourceSlotId
) implements CustomPayload {

    public static final Id<OpenEnchantmentScreenPayload> ID = new Id<>(
            Identifier.of(NEIEnchantMod.MOD_ID, "open_enchant_screen")
    );

    public static final PacketCodec<RegistryByteBuf, OpenEnchantmentScreenPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, OpenEnchantmentScreenPayload::sourceSlotId,
            OpenEnchantmentScreenPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
