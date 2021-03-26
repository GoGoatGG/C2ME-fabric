package org.yatopiamc.c2me.mixin.threading.worldgen;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.yatopiamc.c2me.common.threading.worldgen.ChunkStatusUtils;
import org.yatopiamc.c2me.common.threading.worldgen.IWorldGenLockable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {

    @Shadow @Final public static ChunkStatus FEATURES;

    @Shadow @Final private ChunkStatus.Task task;

    /**
     * @author ishland
     * @reason take over generation
     */
    @Overwrite
    public CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> runTask(ServerWorld serverWorld, ChunkGenerator<?> chunkGenerator, StructureManager structureManager, ServerLightingProvider serverLightingProvider, Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> function, List<Chunk> list) {
        final Chunk targetChunk = list.get(list.size() / 2);
        //noinspection ConstantConditions
        return ChunkStatusUtils.runChunkGenWithLock(targetChunk.getPos(), (Object) this == FEATURES ? 1 : 0, ((IWorldGenLockable) serverWorld).getWorldGenChunkLock(), () ->
                ChunkStatusUtils.getThreadingType((ChunkStatus) (Object) this).runTask(((IWorldGenLockable) serverWorld).getWorldGenSingleThreadedLock(), () ->
                        this.task.doWork((ChunkStatus) (Object) this, serverWorld, chunkGenerator, structureManager, serverLightingProvider, function, list, targetChunk)
                )
        );
    }

}
