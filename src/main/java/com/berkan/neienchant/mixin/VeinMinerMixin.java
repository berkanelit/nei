package com.berkan.neienchant.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
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

import net.minecraft.registry.tag.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

@Mixin(net.minecraft.server.network.ServerPlayerInteractionManager.class)
public class VeinMinerMixin {
    private static final ThreadLocal<Boolean> IS_MINING = ThreadLocal.withInitial(() -> false);

    @Shadow public ServerPlayerEntity player;
    @Shadow protected ServerWorld world;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"))
    private void onBlockDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (IS_MINING.get()) return;

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
            IS_MINING.set(true);
            try {
                veinMine(world, player, pos, state.getBlock());
            } finally {
                IS_MINING.set(false);
            }
        }
    }

    private boolean isOre(BlockState state) {
        String name = state.getBlock().getTranslationKey();
        return name.contains("ore") || name.contains("raw");
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
