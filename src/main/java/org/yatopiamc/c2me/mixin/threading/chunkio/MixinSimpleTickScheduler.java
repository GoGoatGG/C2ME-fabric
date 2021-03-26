package org.yatopiamc.c2me.mixin.threading.chunkio;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.world.SimpleTickScheduler;
import net.minecraft.util.Identifier;
import net.minecraft.world.ScheduledTick;
import net.minecraft.world.TickScheduler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.yatopiamc.c2me.common.util.DeepCloneable;

import java.util.Set;
import java.util.function.Function;

@Mixin(SimpleTickScheduler.class)
public abstract class MixinSimpleTickScheduler<T> implements DeepCloneable {

    @Shadow @Final private Function<T, Identifier> identifierProvider;

    @Shadow @Final private Set<ScheduledTick<T>> scheduledTicks;

    @Override
    public Object deepClone() {
        final SimpleTickScheduler<T> scheduler = new SimpleTickScheduler<>(identifierProvider, new ObjectArrayList<>());
        scheduler.scheduleAll(scheduledTicks.stream().map(tScheduledTick -> new ScheduledTick<>(tScheduledTick.pos, tScheduledTick.getObject(), tScheduledTick.time, tScheduledTick.priority)));
        return scheduler;
    }
}
