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

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.server.permission.handler.IPermissionHandler;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionType;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.hooks.PermissionHooks;
import org.spongepowered.neoforge.accessor.neoforge.server.permission.PermissionAPIAccessor_Neo;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Answers Sponge permission and option checks from NeoForge's active permission handler, so a NeoForge permission mod
 * (LuckPerms) also manages Sponge plugins' permissions.
 *
 * <p>Sponge permissions are arbitrary strings, while {@code PermissionAPI#getPermission} only accepts nodes registered
 * during {@code PermissionGatherEvent.Nodes}. So this builds a node per permission on demand and asks the handler
 * directly. The nodes' default resolver returns {@code null}, which the handler passes back when it has no value
 * (LuckPerms does), and that maps to {@link Tristate#UNDEFINED} so Sponge's own data and op-level fallback still
 * apply. Permissions are {@code BOOLEAN} nodes; options are {@code STRING} nodes, which LuckPerms answers from meta.</p>
 */
public final class NeoPermissionHooks implements PermissionHooks {

    private static final PermissionNode.PermissionResolver<Boolean> UNSET_BOOLEAN = (player, uuid, contexts) -> null;
    private static final PermissionNode.PermissionResolver<String> UNSET_STRING = (player, uuid, contexts) -> null;

    // Optional.empty() marks strings that can't be expressed as a NeoForge node
    private final Map<String, Optional<PermissionNode<Boolean>>> permissionNodes = new ConcurrentHashMap<>();
    private final Map<String, Optional<PermissionNode<String>>> optionNodes = new ConcurrentHashMap<>();

    @Override
    public Tristate permissionValue(final UUID player, final String permission) {
        final Optional<PermissionNode<Boolean>> node = this.permissionNodes.computeIfAbsent(permission.toLowerCase(Locale.ROOT),
                key -> NeoPermissionHooks.node(key, PermissionTypes.BOOLEAN, NeoPermissionHooks.UNSET_BOOLEAN));
        if (node.isEmpty()) {
            return Tristate.UNDEFINED;
        }
        final @Nullable Boolean value = NeoPermissionHooks.query(player, node.get());
        return value == null ? Tristate.UNDEFINED : Tristate.fromBoolean(value);
    }

    @Override
    public Optional<String> option(final UUID player, final String option) {
        final Optional<PermissionNode<String>> node = this.optionNodes.computeIfAbsent(option.toLowerCase(Locale.ROOT),
                key -> NeoPermissionHooks.node(key, PermissionTypes.STRING, NeoPermissionHooks.UNSET_STRING));
        return node.map(n -> NeoPermissionHooks.query(player, n));
    }

    private static <T> @Nullable T query(final UUID uuid, final PermissionNode<T> node) {
        final @Nullable IPermissionHandler handler = PermissionAPIAccessor_Neo.accessor$activeHandler();
        if (handler == null) {
            return null; // the permission API is set up when the server starts
        }
        try {
            final @Nullable MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            final @Nullable ServerPlayer online = server == null ? null : server.getPlayerList().getPlayer(uuid);
            return online != null ? handler.getPermission(online, node) : handler.getOfflinePermission(uuid, node);
        } catch (final RuntimeException e) {
            SpongeCommon.logger().debug("Permission handler {} could not answer {}", handler.getIdentifier(), node.getNodeName(), e);
            return null;
        }
    }

    /**
     * NeoForge node names are {@code <namespace>.<path>} of an {@link Identifier}, so {@code nucleus.gamemode.base}
     * becomes namespace {@code nucleus}, path {@code gamemode.base}.
     */
    private static <T> Optional<PermissionNode<T>> node(final String permission, final PermissionType<T> type,
            final PermissionNode.PermissionResolver<T> resolver) {
        final int dot = permission.indexOf('.');
        if (dot <= 0 || dot == permission.length() - 1) {
            return Optional.empty();
        }
        final @Nullable Identifier id = Identifier.tryBuild(permission.substring(0, dot), permission.substring(dot + 1));
        return id == null ? Optional.empty() : Optional.of(new PermissionNode<>(id, type, resolver));
    }
}
