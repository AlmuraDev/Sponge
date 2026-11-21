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
package org.spongepowered.common.event.tracking.phase.tick;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.bridge.TrackableBridge;
import org.spongepowered.common.bridge.world.inventory.ViewableInventoryBridge;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseTracker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TileEntityTickContext extends LocationBasedTickContext<TileEntityTickContext> {

    // Other viewed containers the ticking block entity changed (e.g. a pipe or hopper emptying an open chest)
    private @Nullable Set<ViewableInventoryBridge> changedViewables;

    public TileEntityTickContext(final IPhaseState<TileEntityTickContext> phaseState, final PhaseTracker tracker) {
        super(phaseState, tracker);
    }

    /**
     * Remembers a viewed container changed during this tick, so its viewers are synchronized before the tick ends and the change
     * is attributed to this block entity instead of to a viewer's next packet.
     */
    public void markViewableChanged(final ViewableInventoryBridge viewable) {
        if (this.changedViewables == null) {
            this.changedViewables = new HashSet<>();
        }
        this.changedViewables.add(viewable);
    }

    public Set<ViewableInventoryBridge> changedViewables() {
        return this.changedViewables == null ? Collections.emptySet() : this.changedViewables;
    }

    @Override
    protected void reset() {
        super.reset();
        this.changedViewables = null;
    }

    @Override
    public TileEntityTickContext source(final Object owner) {
        super.source(owner);
        if (owner instanceof TrackableBridge mixinTileentity) {
            this.setBlockEvents(mixinTileentity.bridge$allowsBlockEventCreation())
                .setBulkBlockCaptures(mixinTileentity.bridge$allowsBlockBulkCaptures())
                .setEntitySpawnEvents(mixinTileentity.bridge$allowsEntityEventCreation())
                .setBulkEntityCaptures(mixinTileentity.bridge$allowsEntityBulkCaptures());
        }
        return this;
    }
}
