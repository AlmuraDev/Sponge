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
package org.spongepowered.neoforge.mixin.api.minecraft.server.packs.repository;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link org.spongepowered.api.resource.pack.PackRepository#pack(PluginContainer)} for NeoForge.
 *
 * <p>SpongeVanilla adds a {@code plugin-<id>} pack for every plugin. On NeoForge plugins are mods, so their data is
 * already loaded through NeoForge's combined {@code mod_data} pack. This builds the same {@code plugin-<id>} pack on
 * demand from NeoForge's per-mod resources, so plugins can keep using {@code ResourcePath.of(pack.id(), path)}, without
 * adding the plugin's data to the repository a second time.</p>
 */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin_Neo_API implements org.spongepowered.api.resource.pack.PackRepository {

    @Unique private static final PackSelectionConfig neo$PLUGIN_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.BOTTOM, false);

    @Unique private final Map<String, Pack> neo$pluginPacks = new ConcurrentHashMap<>();

    @Override
    public org.spongepowered.api.resource.pack.Pack pack(final PluginContainer container) {
        final String pluginId = Objects.requireNonNull(container, "container").metadata().id();
        return (org.spongepowered.api.resource.pack.Pack) this.neo$pluginPacks.computeIfAbsent(pluginId, PackRepositoryMixin_Neo_API::neo$createPluginPack);
    }

    @Unique
    private static Pack neo$createPluginPack(final String pluginId) {
        final Pack.ResourcesSupplier resources = ResourcePackLoader.getPackFor(pluginId)
                .orElseThrow(() -> new IllegalArgumentException("No mod file is loaded for plugin " + pluginId));
        // The same id SpongeVanilla uses, which is also the namespace a plugin's bundled data lives under
        final String packId = "plugin-" + pluginId;
        final PackLocationInfo info = new PackLocationInfo(packId, Component.literal(packId), PackSource.DEFAULT, Optional.empty());
        try {
            // Plugin jars usually have no pack.mcmeta; NeoForge's reader copes with that, Pack#readMetaAndCreate doesn't
            return ResourcePackLoader.readWithOptionalMeta(info, resources, PackType.SERVER_DATA, PackRepositoryMixin_Neo_API.neo$PLUGIN_SELECTION_CONFIG);
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not read the resources of plugin " + pluginId, e);
        }
    }
}
