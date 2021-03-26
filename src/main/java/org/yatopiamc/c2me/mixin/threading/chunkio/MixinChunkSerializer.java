package org.yatopiamc.c2me.mixin.threading.chunkio;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.world.ServerTickScheduler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.LightType;
import net.minecraft.world.TickScheduler;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.yatopiamc.c2me.common.threading.chunkio.AsyncSerializationManager;
import org.yatopiamc.c2me.common.threading.chunkio.ChunkIoMainThreadTaskUtils;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mixin(ChunkSerializer.class)
public class MixinChunkSerializer {

    @Redirect(method = "deserialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/poi/PointOfInterestStorage;initForPalette(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/ChunkSection;)V"))
    private static void onPoiStorageInitForPalette(PointOfInterestStorage pointOfInterestStorage, ChunkPos chunkPos, ChunkSection chunkSection) {
        ChunkIoMainThreadTaskUtils.executeMain(() -> pointOfInterestStorage.initForPalette(chunkPos, chunkSection));
    }

    @Redirect(method = "serialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getBlockEntityPositions()Ljava/util/Set;"))
    private static Set<BlockPos> onChunkGetBlockEntityPositions(Chunk chunk) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(chunk.getPos());
        return scope != null ? scope.blockEntities.keySet() : chunk.getBlockEntityPositions();
    }

    @Redirect(method = "serialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;method_20598(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/nbt/CompoundTag;"))
    private static CompoundTag onChunkGetPackedBlockEntityTag(Chunk chunk, BlockPos pos) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(chunk.getPos());
        if (scope == null) return chunk.method_20598(pos);
        final BlockEntity blockEntity = scope.blockEntities.get(pos);
        if (blockEntity == null || blockEntity.isRemoved()) return null;
        final CompoundTag compoundTag = new CompoundTag();
        if (chunk instanceof WorldChunk) compoundTag.putBoolean("keepPacked", false);
        blockEntity.toTag(compoundTag);
        return compoundTag;
    }

    @Redirect(method = "serialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getBlockTickScheduler()Lnet/minecraft/world/TickScheduler;"))
    private static TickScheduler<Block> onChunkGetBlockTickScheduler(Chunk chunk) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(chunk.getPos());
        return scope != null ? scope.blockTickScheduler : chunk.getBlockTickScheduler();
    }

    @Redirect(method = "serialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getFluidTickScheduler()Lnet/minecraft/world/TickScheduler;"))
    private static TickScheduler<Fluid> onChunkGetFluidTickScheduler(Chunk chunk) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(chunk.getPos());
        return scope != null ? scope.fluidTickScheduler : chunk.getFluidTickScheduler();
    }

    @Redirect(method = "serialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerTickScheduler;toTag(Lnet/minecraft/util/math/ChunkPos;)Lnet/minecraft/nbt/ListTag;"))
    private static ListTag onServerTickSchedulerToTag(@SuppressWarnings("rawtypes") ServerTickScheduler serverTickScheduler, ChunkPos chunkPos) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(chunkPos);
        return scope != null ? CompletableFuture.supplyAsync(() -> serverTickScheduler.toTag(chunkPos), serverTickScheduler.world.getChunkManager().mainThreadExecutor).join() : serverTickScheduler.toTag(chunkPos);
    }

    @Redirect(method = "serialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;get(Lnet/minecraft/world/LightType;)Lnet/minecraft/world/chunk/light/ChunkLightingView;"))
    private static ChunkLightingView onLightingProviderGet(LightingProvider lightingProvider, LightType lightType) {
        final AsyncSerializationManager.Scope scope = AsyncSerializationManager.getScope(null);
        return scope != null ? scope.lighting.get(lightType) : lightingProvider.get(lightType);
    }

}
