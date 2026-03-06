package com.berkan.neienchant.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.registry.tag.BlockTags;

@Mixin(net.minecraft.server.network.ServerPlayerInteractionManager.class)
public class VeinMinerMixin {
    // Per-player mining state tracking — thread-safe ConcurrentHashMap
    private static final Set<UUID> MINING_PLAYERS = ConcurrentHashMap.newKeySet();

    @Shadow public ServerPlayerEntity player;
    @Shadow protected ServerWorld world;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"))
    private void onBlockDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Per-player recursion guard
        UUID playerId = player.getUuid();
        if (MINING_PLAYERS.contains(playerId)) return;

        BlockState state = world.getBlockState(pos);
        ItemStack tool = player.getMainHandStack();

        // Check for Vein Miner enchantment
        ItemEnchantmentsComponent enchants = tool.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        boolean hasVeinMiner = false;
        for (var entry : enchants.getEnchantmentEntries()) {
            if (entry.getKey().getKey().isPresent() &&
                entry.getKey().getKey().get().getValue().toString().equals("neienchant:vein_miner")) {
                hasVeinMiner = true;
                break;
            }
        }

        if (hasVeinMiner && isOre(state)) {
            MINING_PLAYERS.add(playerId);
            try {
                veinMine(world, player, pos, state.getBlock());
            } finally {
                MINING_PLAYERS.remove(playerId);
            }
        }
    }

    private boolean isOre(BlockState state) {
        // Safe ore check using BlockTags
        return state.isIn(BlockTags.COAL_ORES) ||
               state.isIn(BlockTags.IRON_ORES) ||
               state.isIn(BlockTags.COPPER_ORES) ||
               state.isIn(BlockTags.GOLD_ORES) ||
               state.isIn(BlockTags.REDSTONE_ORES) ||
               state.isIn(BlockTags.LAPIS_ORES) ||
               state.isIn(BlockTags.DIAMOND_ORES) ||
               state.isIn(BlockTags.EMERALD_ORES);
    }

    private void veinMine(ServerWorld world, ServerPlayerEntity player, BlockPos startPos, Block targetBlock) {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        int count = 0;
        int maxBlocks = 60;

        while (!queue.isEmpty() && count < maxBlocks) {
            BlockPos pos = queue.poll();
            
            // Skip the first block as it's already being broken by the standard process
            if (count > 0) {
                BlockState state = world.getBlockState(pos);
                if (state.getBlock() == targetBlock) {
                    ((net.minecraft.server.network.ServerPlayerInteractionManager)(Object)this).tryBreakBlock(pos);
                }
            }
            
            count++;

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.offset(dir);
                if (!visited.contains(neighbor)) {
                    BlockState neighborState = world.getBlockState(neighbor);
                    if (neighborState.getBlock() == targetBlock) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
    }
}
