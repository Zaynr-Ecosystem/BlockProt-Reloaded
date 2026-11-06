/*
 * Copyright (C) 2021 - 2026 spnda
 * Modifications Copyright (C) 2025 - 2026 Zaynr (Zar)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 * Based on BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.enginehub.squirrelid.Profile;
import org.enginehub.squirrelid.cache.ProfileCache;
import org.enginehub.squirrelid.resolver.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Resolves player profiles using the server's own caches first (Bukkit/Paper),
 * falling back to the Mojang endpoint, and writes every result into the local cache.
 */
public class CachedProfileService implements ProfileService, ProfileCache {
    private final ProfileService resolver;
    private final ProfileCache cache;

    public CachedProfileService(ProfileCache cache) {
        final List<ProfileService> services = new ArrayList<>();
        services.add(BukkitPlayerService.getInstance());
        services.add(PaperPlayerService.getInstance());
        if (org.bukkit.Bukkit.getOnlineMode()) {
            services.add(HttpRepositoryService.forMinecraft());
        }
        services.removeIf(Objects::isNull);
        this.resolver = new CombinedProfileService(services);
        this.cache = cache;
    }

    @Override
    public int getIdealRequestLimit() {
        return this.resolver.getIdealRequestLimit();
    }

    @Nullable
    @Override
    public Profile findByName(String name) throws IOException, InterruptedException {
        final var profile = this.resolver.findByName(name);
        if (profile != null) {
            put(profile);
        }
        return profile;
    }

    @Override
    public ImmutableList<Profile> findAllByName(Iterable<String> names) throws IOException, InterruptedException {
        final var profiles = this.resolver.findAllByName(names);
        putAll(profiles);
        return profiles;
    }

    @Override
    public void findAllByName(Iterable<String> names, Predicate<Profile> predicate) throws IOException, InterruptedException {
        this.resolver.findAllByName(names, (input) -> {
            put(input);
            return predicate.test(input);
        });
    }

    @Nullable
    @Override
    public Profile findByUuid(UUID uuid) {
        var profile = getIfPresent(uuid);
        if (profile == null) {
            try {
                profile = this.resolver.findByUuid(uuid);
                if (profile != null) {
                    put(profile);
                }
            } catch (Exception e) {
                return profile;
            }
        }
        return profile;
    }

    @Override
    public ImmutableList<Profile> findAllByUuid(Iterable<UUID> uuids) {
        final var map = getAllPresent(uuids);

        if (map.isEmpty()) {
            final var profiles = new ArrayList<Profile>();
            for (final var uuid : uuids) {
                try {
                    final var profile = this.resolver.findByUuid(uuid);
                    // Guard: resolver may return null for offline/unknown UUIDs.
                    if (profile != null) {
                        put(profile);
                        profiles.add(profile);
                    }
                } catch (Exception e) {
                    return ImmutableList.copyOf(profiles);
                }
            }
            return ImmutableList.copyOf(profiles);
        }

        // The returned map doesn't include the UUIDs which were not found in the cache.
        final var lookup = new ArrayList<UUID>();
        for (final var uuid : uuids) {
            if (!map.containsKey(uuid) || map.get(uuid) == null) {
                lookup.add(uuid);
            }
        }

        if (!lookup.isEmpty()) {
            final var profiles = new ArrayList<Profile>();
            for (final var p : map.values()) {
                if (p != null) profiles.add(p);
            }
            for (final var uuid : lookup) {
                try {
                    final var profile = this.resolver.findByUuid(uuid);
                    // Guard: resolver may return null for offline/unknown UUIDs.
                    if (profile != null) {
                        put(profile);
                        profiles.add(profile);
                    }
                } catch (Exception e) {
                    return ImmutableList.copyOf(profiles);
                }
            }
            return ImmutableList.copyOf(profiles);
        }

        return map.values().stream()
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());
    }

    @Override
    public void findAllByUuid(Iterable<UUID> uuids, Predicate<Profile> predicate) throws IOException, InterruptedException {
        this.resolver.findAllByUuid(uuids, (input) -> {
            put(input);
            return predicate.test(input);
        });
    }

    @Override
    public void put(Profile profile) {
        this.cache.put(profile);
    }

    @Override
    public void putAll(Iterable<Profile> iterable) {
        this.cache.putAll(iterable);
    }

    @Nullable
    @Override
    public Profile getIfPresent(UUID uuid) {
        return this.cache.getIfPresent(uuid);
    }

    @Override
    public ImmutableMap<UUID, Profile> getAllPresent(Iterable<UUID> iterable) {
        return this.cache.getAllPresent(iterable);
    }
}