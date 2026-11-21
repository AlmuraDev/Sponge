/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.tracker.world.level.block.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.DelegatingConfigTrackableBridge;
import org.spongepowered.common.bridge.TrackableBridge;
import org.spongepowered.common.bridge.world.inventory.ViewableInventoryBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.tick.TileEntityTickContext;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin_Tracker implements DelegatingConfigTrackableBridge {

    // @formatter:off
    @Shadow @Final private BlockEntityType<?> type;
    // @formatter:on

    @Override
    public TrackableBridge bridge$trackingConfigDelegate() {
        return (TrackableBridge) this.type;
    }

    /**
     * A block entity ticking can change another block entity's container: a hopper or a mod's pipe (through NeoForge's item
     * transfer) emptying a chest. If players have that container open, it is remembered so TrackingUtil synchronizes their menus
     * before the tick ends. Otherwise the change is first noticed by a viewer's next packet and logged as a slot transaction
     * without an event.
     */
    @Inject(method = "setChanged()V", at = @At("HEAD"))
    private void tracker$markChangedForViewers(final CallbackInfo ci) {
        if (!((Object) this instanceof ViewableInventoryBridge viewable) || !viewable.viewableBridge$hasViewers()
                || !(((BlockEntity) (Object) this).getLevel() instanceof ServerLevel level)) {
            return;
        }
        final PhaseTracker tracker = PhaseTracker.getWorldInstance(level);
        if (!tracker.onSidedThread()) {
            return;
        }
        if (tracker.getPhaseContext() instanceof TileEntityTickContext context) {
            context.markViewableChanged(viewable);
        }
    }
}
