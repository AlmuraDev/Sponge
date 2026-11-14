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
package org.spongepowered.neoforge.hook;

import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.neoforge.launch.plugin.NeoPluginContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import org.spongepowered.common.hooks.GeneralHooks;

import java.util.Optional;

public class NeoGeneralHooks implements GeneralHooks {

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final String SPONGE_MODULE = "spongeneo";

    // module name -> mod; built once mods are loaded
    private volatile @Nullable Map<String, ModContainer> modsByModule;

    /**
     * Mods register Brigadier commands from {@code RegisterCommandsEvent} with no plugin in the cause, so their
     * commands were owned by "unknown". The first class on the stack outside SpongeNeo that lives in a mod's module is
     * the registering mod.
     */
    @Override
    public Optional<PluginContainer> findCallingPlugin() {
        final Map<String, ModContainer> mods = this.modsByModule();
        if (mods.isEmpty()) {
            return Optional.empty();
        }
        return NeoGeneralHooks.STACK_WALKER.walk(frames -> frames
                .map(frame -> frame.getDeclaringClass().getModule().getName())
                .filter(name -> name != null && !NeoGeneralHooks.SPONGE_MODULE.equals(name))
                .map(mods::get)
                .filter(Objects::nonNull)
                .findFirst())
            .flatMap(NeoPluginContainer::of);
    }

    private Map<String, ModContainer> modsByModule() {
        @Nullable Map<String, ModContainer> mods = this.modsByModule;
        if (mods == null) {
            final @Nullable ModList modList = ModList.get();
            if (modList == null) {
                return Map.of();
            }
            final Map<String, ModContainer> built = new HashMap<>();
            modList.forEachModContainer((id, container) -> {
                if (container.getModInfo().getOwningFile().getFile() instanceof final ModFile file
                        && file.getModuleDescriptor() != null) {
                    // A file can hold several mods; its first one owns the module
                    built.putIfAbsent(file.getModuleDescriptor().name(), container);
                }
            });
            mods = Map.copyOf(built);
            this.modsByModule = mods;
        }
        return mods;
    }

    public ServerStatus createServerStatus(ServerStatus originalStatus, Component description, Optional<ServerStatus.Players> players, Optional<ServerStatus.Version> version, Optional<ServerStatus.Favicon> favicon) {
        return new ServerStatus(description, players, version, favicon, originalStatus.enforcesSecureChat(), originalStatus.isModded());
    }
}
