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
package org.spongepowered.common.hooks;

import org.spongepowered.api.util.Tristate;

import java.util.Optional;
import java.util.UUID;

/**
 * Lets a platform answer permission and option checks for players from its own permission system before Sponge's
 * built-in data and op-level fallback are consulted.
 */
public interface PermissionHooks {

    /**
     * @return the platform's value, or {@link Tristate#UNDEFINED} to fall back to Sponge's own data
     */
    default Tristate permissionValue(final UUID player, final String permission) {
        return Tristate.UNDEFINED;
    }

    /**
     * @return the platform's value, or empty to fall back to Sponge's own data
     */
    default Optional<String> option(final UUID player, final String option) {
        return Optional.empty();
    }
}
