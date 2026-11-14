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

package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.util.StringUtil;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Parses block family expressions used in blocks.yml and worlds.yml.
 *
 * <h3>Syntax summary</h3>
 * <pre>
 *   [*]                        -> all members of the family
 *   [* -CHEST]                 -> all members except the CHEST material specifically
 *   [*-CHEST]                  -> only the CHEST sub-family
 *   [*-CHEST -COPPER_CHEST]    -> all chest sub-family members except COPPER_CHEST
 *   [* -FLETCHING_TABLE]         -> ERROR: FLETCHING_TABLE not in TILE_ENTITIES family; discarded
 *   [CHEST BARREL]             -> only CHEST and BARREL (empty base, explicit inclusions)
 *   [-*SHULKERS WHITE_SHULKER_BOX] -> all family except shulkers, but WHITE_SHULKER_BOX added back
 * </pre>
 *
 * <h3>Token reference</h3>
 * <ul>
 *   <li>{@code *}      : include all members of the top-level family</li>
 *   <li>{@code *-TAG}  : include all members of the named sub-family</li>
 *   <li>{@code -*TAG}  : exclude all members of the named sub-family</li>
 *   <li>{@code NAME}   : include a specific material (must belong to this family)</li>
 *   <li>{@code -NAME}  : exclude a specific material (must belong to this family)</li>
 * </ul>
 *
 * <h3>Cross-family validation</h3>
 * {@code NAME} and {@code -NAME} tokens are validated against the family of the config key.
 * A material not belonging to the current family is rejected with a console warning and discarded.
 *
 * @see <a href="docs/MODERN SYNTAX AND LEGACY/BLOCK_FAMILY_SYNTAX.md">BLOCK_FAMILY_SYNTAX.md</a>
 */
public final class BlockFamilyParser {

    private BlockFamilyParser() {}

    public enum SubFamily {
        CHEST("CHEST", Family.TILE_ENTITIES),
        FURNACE("FURNACE", Family.TILE_ENTITIES),
        SHELF("SHELF", Family.TILE_ENTITIES),
        TRANSPORT("TRANSPORT", Family.TILE_ENTITIES),
        MISC("MISC", Family.TILE_ENTITIES),
        SIGN("SIGN", Family.TILE_ENTITIES),

        SHULKERS("SHULKERS", Family.SHULKER_BOXES),

        ANVIL("ANVIL", Family.BLOCKS),
        CAULDRON("CAULDRON", Family.BLOCKS),
        WORKSTATION("WORKSTATION", Family.BLOCKS),
        TRAPDOOR("TRAPDOOR", Family.BLOCKS),
        FENCE_GATE("FENCE_GATE", Family.BLOCKS),
        BED("BED", Family.BLOCKS),
        GRATE("GRATE", Family.BLOCKS),

        DOORS("DOORS", Family.DOORS),

        CHEST_BOATS("CHEST_BOATS", Family.ENTITIES),
        CHEST_MINECARTS("CHEST_MINECARTS", Family.ENTITIES),
        HOPPER_MINECARTS("HOPPER_MINECARTS", Family.ENTITIES),
        ITEM_FRAMES("ITEM_FRAMES", Family.ENTITIES);

        public final String tag;
        public final Family ownerFamily;

        SubFamily(String tag, Family ownerFamily) {
            this.tag = tag;
            this.ownerFamily = ownerFamily;
        }

        @Nullable
        public static SubFamily byTag(@NotNull String tag) {
            String upper = tag.toUpperCase(Locale.ROOT);
            for (SubFamily sf : values()) {
                if (sf.tag.equals(upper)) return sf;
            }
            return null;
        }
    }

    public enum Family {
        TILE_ENTITIES,
        SHULKER_BOXES,
        BLOCKS,
        DOORS,
        ENTITIES
    }

    private static final Map<Family, Set<Material>>    FAMILY_MEMBERS    = new EnumMap<>(Family.class);
    private static final Map<SubFamily, Set<Material>> SUBFAMILY_MEMBERS = new EnumMap<>(SubFamily.class);

    static { buildFamilies(); }

    private static final List<Material> SEARCH_UNIVERSE = buildSearchUniverse();

    private static List<Material> buildSearchUniverse() {
        LinkedHashSet<Material> all = new LinkedHashSet<>();
        for (Family family : Family.values()) {
            all.addAll(FAMILY_MEMBERS.getOrDefault(family, Collections.emptySet()));
        }
        return List.copyOf(all);
    }

    private static void buildFamilies() {
        Set<Material> tiles    = new LinkedHashSet<>();
        Set<Material> shulkers = new LinkedHashSet<>();
        Set<Material> blocks   = new LinkedHashSet<>();
        Set<Material> doors    = new LinkedHashSet<>();
        Set<Material> entities = new LinkedHashSet<>();

        Map<SubFamily, Set<Material>> sfAcc = new EnumMap<>(SubFamily.class);
        for (SubFamily sf : SubFamily.values()) sfAcc.put(sf, new LinkedHashSet<>());

        for (Material m : Material.values()) {
            if (m.isLegacy() || m.isAir()) continue;
            String n = m.name();

            if (n.contains("SHULKER_BOX")) {
                shulkers.add(m);
                sfAcc.get(SubFamily.SHULKERS).add(m);
                continue;
            }
            if (n.endsWith("_DOOR") && !n.contains("TRAP")) {
                doors.add(m);
                sfAcc.get(SubFamily.DOORS).add(m);
                continue;
            }
            if (isTileEntity(m)) {
                tiles.add(m);
                if (isChestMaterial(n))         sfAcc.get(SubFamily.CHEST).add(m);
                else if (isFurnaceMaterial(n))   sfAcc.get(SubFamily.FURNACE).add(m);
                else if (n.endsWith("_SHELF"))   sfAcc.get(SubFamily.SHELF).add(m);
                else if (isTransportMaterial(n)) sfAcc.get(SubFamily.TRANSPORT).add(m);
                else if (isSignMaterial(n))      sfAcc.get(SubFamily.SIGN).add(m);
                else                             sfAcc.get(SubFamily.MISC).add(m);
                continue;
            }
            if (isInteractiveBlock(m)) {
                blocks.add(m);
                if (n.contains("TRAPDOOR"))        sfAcc.get(SubFamily.TRAPDOOR).add(m);
                else if (n.contains("FENCE_GATE")) sfAcc.get(SubFamily.FENCE_GATE).add(m);
                else if (n.contains("ANVIL"))      sfAcc.get(SubFamily.ANVIL).add(m);
                else if (n.contains("CAULDRON"))   sfAcc.get(SubFamily.CAULDRON).add(m);
                else if (isWorkstationMaterial(n)) sfAcc.get(SubFamily.WORKSTATION).add(m);
                else if (isBedMaterial(n))         sfAcc.get(SubFamily.BED).add(m);
                else if (n.endsWith("_GRATE"))     sfAcc.get(SubFamily.GRATE).add(m);
                continue;
            }
            if (isEntityMaterial(n)) {
                entities.add(m);
                if (n.contains("CHEST_BOAT") || n.contains("CHEST_RAFT"))         sfAcc.get(SubFamily.CHEST_BOATS).add(m);
                else if (n.equals("CHEST_MINECART"))  sfAcc.get(SubFamily.CHEST_MINECARTS).add(m);
                else if (n.equals("HOPPER_MINECART")) sfAcc.get(SubFamily.HOPPER_MINECARTS).add(m);
                else if (n.equals("ITEM_FRAME") || n.equals("GLOW_ITEM_FRAME")) sfAcc.get(SubFamily.ITEM_FRAMES).add(m);
            }
        }

        FAMILY_MEMBERS.put(Family.TILE_ENTITIES, Collections.unmodifiableSet(tiles));
        FAMILY_MEMBERS.put(Family.SHULKER_BOXES, Collections.unmodifiableSet(shulkers));
        FAMILY_MEMBERS.put(Family.BLOCKS,        Collections.unmodifiableSet(blocks));
        FAMILY_MEMBERS.put(Family.DOORS,         Collections.unmodifiableSet(doors));
        FAMILY_MEMBERS.put(Family.ENTITIES,      Collections.unmodifiableSet(entities));

        for (SubFamily sf : SubFamily.values())
            SUBFAMILY_MEMBERS.put(sf, Collections.unmodifiableSet(sfAcc.get(sf)));
    }

    private static boolean isChestMaterial(@NotNull String n) {
        if (n.equals("CHEST") || n.equals("TRAPPED_CHEST") || n.equals("ENDER_CHEST")) return true;
        if (n.contains("COPPER")) {
            return n.endsWith("_CHEST") || n.endsWith("_TRAPPED_CHEST");
        }
        return false;
    }

    private static boolean isFurnaceMaterial(@NotNull String n) {
        return n.equals("FURNACE") || n.equals("SMOKER") || n.equals("BLAST_FURNACE");
    }

    private static boolean isTransportMaterial(@NotNull String n) {
        return n.equals("HOPPER") || n.equals("DISPENSER") || n.equals("DROPPER");
    }

    private static boolean isSignMaterial(@NotNull String n) {
        return n.endsWith("_SIGN") || n.endsWith("_WALL_SIGN")
            || n.endsWith("_HANGING_SIGN") || n.endsWith("_WALL_HANGING_SIGN");
    }

    private static boolean isWorkstationMaterial(@NotNull String n) {
        return n.equals("GRINDSTONE") || n.equals("STONECUTTER") || n.equals("LOOM")
            || n.equals("CARTOGRAPHY_TABLE") || n.equals("SMITHING_TABLE")
            || n.equals("ENCHANTING_TABLE") || n.equals("FLETCHING_TABLE");
    }

    private static boolean isBedMaterial(@NotNull String n) {
        return n.endsWith("_BED");
    }

    private static boolean isEntityMaterial(@NotNull String n) {
        return n.contains("CHEST_BOAT") || n.contains("CHEST_RAFT") || n.equals("CHEST_MINECART") || n.equals("HOPPER_MINECART")
            || n.equals("ITEM_FRAME") || n.equals("GLOW_ITEM_FRAME");
    }

    private static boolean isTileEntity(@NotNull Material m) {
        String n = m.name();
        if (isChestMaterial(n)) return true;
        if (n.endsWith("_SHELF")) return true;
        if (isFurnaceMaterial(n)) return true;
        if (n.equals("HOPPER") || n.equals("DISPENSER") || n.equals("DROPPER")) return true;
        if (n.equals("BARREL") || n.equals("BREWING_STAND")) return true;
        if (n.equals("DECORATED_POT") || n.equals("CHISELED_BOOKSHELF") || n.equals("CRAFTER")) return true;
        if (n.equals("LECTERN") || n.equals("BEEHIVE") || n.equals("BEE_NEST")) return true;
        if (n.equals("JUKEBOX")) return true;
        if (n.equals("BEACON")) return true;
        if (isSignMaterial(n)) return true;
        return false;
    }

    private static boolean isInteractiveBlock(@NotNull Material m) {
        String n = m.name();
        if (n.equals("DRAGON_EGG")) return true;
        if (n.equals("COMPOSTER") || n.equals("BELL") || n.equals("NOTE_BLOCK")) return true;
        if (n.contains("CAULDRON")) return true;
        if (n.contains("ANVIL")) return true;
        if (isWorkstationMaterial(n)) return true;
        if (n.contains("FENCE_GATE")) return true;
        if (n.contains("TRAPDOOR")) return true;
        if (isBedMaterial(n)) return true;
        if (n.endsWith("_GRATE")) return true;
        return false;
    }

    @NotNull
    public static Set<Material> getFamilyMembers(@NotNull Family family) {
        return FAMILY_MEMBERS.getOrDefault(family, Collections.emptySet());
    }

    @NotNull
    public static Set<Material> getSubFamilyMembers(@NotNull SubFamily subFamily) {
        return SUBFAMILY_MEMBERS.getOrDefault(subFamily, Collections.emptySet());
    }

    @Nullable
    public static SubFamily subFamilyOf(@NotNull Material material) {
        for (Map.Entry<SubFamily, Set<Material>> e : SUBFAMILY_MEMBERS.entrySet()) {
            if (e.getValue().contains(material)) return e.getKey();
        }
        return null;
    }

    /**
     * Searches every family member with a creative-style query ranking:
     * exact match, starts-with, word-prefix, contains, word-contains,
     * then Levenshtein distance of at most 2. Multi-word queries must
     * match on every word; scores add. Ties break on shorter name first,
     * then alphabetically.
     */
    @NotNull
    public static List<Material> searchMaterials(@NotNull String query) {
        String[] rawWords = query.trim().toUpperCase(Locale.ROOT).split("[\\s_]+");
        if (rawWords.length == 0 || rawWords[0].isEmpty()) return Collections.emptyList();

        String[] words = new String[rawWords.length];
        for (int i = 0; i < rawWords.length; i++) {
            words[i] = normalizeQueryToken(rawWords[i]);
        }

        List<Scored> results = new ArrayList<>();
        for (Material m : SEARCH_UNIVERSE) {
            String name = m.name().replace("_", " ");
            int score = 0;
            for (String word : words) {
                int wordScore = scoreWord(word, name);
                if (wordScore == 0) { score = 0; break; }
                score += wordScore;
            }
            if (score > 0) results.add(new Scored(m, score));
        }

        results.sort((a, b) -> {
            if (a.score != b.score) return Integer.compare(b.score, a.score);
            int len = Integer.compare(a.material.name().length(), b.material.name().length());
            if (len != 0) return len;
            return a.material.name().compareTo(b.material.name());
        });

        List<Material> out = new ArrayList<>(results.size());
        for (Scored s : results) out.add(s.material);
        return out;
    }

    private static int scoreWord(@NotNull String word, @NotNull String name) {
        if (name.equals(word)) return 10000;
        if (name.startsWith(word)) return 5000;
        String[] tokens = name.split("\\s+");
        for (String token : tokens) {
            if (token.startsWith(word)) return 4000;
        }
        if (name.contains(word)) return 3000;
        for (String token : tokens) {
            if (token.contains(word)) return 2000;
        }
        int distance = StringUtil.levenshtein(word, name);
        if (distance <= 2) return 1000 - distance;
        return 0;
    }

    private static String normalizeQueryToken(@NotNull String token) {
        return Translator.normalizeSearchToken(token);
    }

    private record Scored(@NotNull Material material, int score) {}

    @NotNull
    public static Set<Material> parse(@Nullable Object raw, @NotNull Family family) {
        if (raw == null) return new LinkedHashSet<>();

        if (raw instanceof List<?> list) {
            if (list.isEmpty()) return new LinkedHashSet<>();
            Set<Material> result = new LinkedHashSet<>();
            for (Object o : list) {
                if (o instanceof String s) {
                    String trimmed = s.trim();
                    if (isFamilyExpression(trimmed)) {
                        result.addAll(parseFamilyExpression(trimmed, family));
                    } else {
                        Material m = Material.matchMaterial(trimmed);
                        if (m != null) addMaterialWithPlacementVariants(m, result);
                    }
                }
            }
            return result;
        }

        if (raw instanceof String s) {
            String trimmed = s.trim();
            if (isFamilyExpression(trimmed)) return new LinkedHashSet<>(parseFamilyExpression(trimmed, family));
            Material m = Material.matchMaterial(trimmed);
            if (m != null) {
                Set<Material> result = new LinkedHashSet<>();
                addMaterialWithPlacementVariants(m, result);
                return result;
            }
            return new LinkedHashSet<>();
        }

        return new LinkedHashSet<>();
    }

    public static boolean isFamilyExpression(@NotNull String s) {
        return s.startsWith("[") && s.endsWith("]");
    }

    public static void addMaterialWithPlacementVariants(@NotNull Material material, @NotNull Set<Material> out) {
        out.add(material);
        addPlacementVariant(material.name(), out, "_WALL_HANGING_SIGN", "_HANGING_SIGN");
        addPlacementVariant(material.name(), out, "_WALL_SIGN", "_SIGN");
        addPlacementVariant(material.name(), out, "_WALL_BANNER", "_BANNER");
        addPlacementVariant(material.name(), out, "_WALL_HEAD", "_HEAD");
        addPlacementVariant(material.name(), out, "_WALL_SKULL", "_SKULL");
    }

    private static void addPlacementVariant(@NotNull String name, @NotNull Set<Material> out,
                                            @NotNull String wallSuffix, @NotNull String standingSuffix) {
        String variantName = null;
        if (name.endsWith(wallSuffix)) {
            variantName = name.substring(0, name.length() - wallSuffix.length()) + standingSuffix;
        } else if (name.endsWith(standingSuffix) && !name.endsWith(wallSuffix)) {
            variantName = name.substring(0, name.length() - standingSuffix.length()) + wallSuffix;
        }
        if (variantName == null) return;
        Material variant = Material.matchMaterial(variantName);
        if (variant != null) out.add(variant);
    }

    /**
     * Parses a bracket expression and resolves it against the given family.
     *
     * <h4>Token types</h4>
     * <ul>
     *   <li>{@code *}      : include all members of the top-level family</li>
     *   <li>{@code *-TAG}  : include all members of the named sub-family</li>
     *   <li>{@code -*TAG}  : exclude all members of the named sub-family</li>
     *   <li>{@code NAME}   : include a specific material (must belong to this family)</li>
     *   <li>{@code -NAME}  : exclude a specific material (must belong to this family)</li>
     * </ul>
     *
     * Individual {@code NAME} and {@code -NAME} tokens are validated against the current family.
     * A material that does not belong to it is logged as a warning and discarded.
     */
    @NotNull
    public static Set<Material> parseFamilyExpression(@NotNull String expr, @NotNull Family family) {
        String inner = expr.substring(1, expr.length() - 1).trim();
        if (inner.isEmpty()) return new LinkedHashSet<>();

        Set<Material> allMembers = FAMILY_MEMBERS.getOrDefault(family, Collections.emptySet());

        List<String> tokens = new ArrayList<>(Arrays.asList(inner.split("\\s+")));
        boolean hasGlobalStar = tokens.contains("*");

        Set<SubFamily> enabledSubFamilies  = new LinkedHashSet<>();
        Set<SubFamily> disabledSubFamilies = new LinkedHashSet<>();
        Set<Material>  explicitInclusions  = new LinkedHashSet<>();
        Set<Material>  explicitExclusions  = new LinkedHashSet<>();
        // For -* disabled sub-families: individual NAME tokens that re-include one member
        Map<SubFamily, Set<Material>> disabledSubFamilyExceptions = new LinkedHashMap<>();

        for (String token : tokens) {
            if (token.equals("*")) continue;

            // *-TAG: enable sub-family
            if (token.startsWith("*-")) {
                String tag = token.substring(2).toUpperCase(Locale.ROOT);
                SubFamily sf = SubFamily.byTag(tag);
                if (sf == null) {
                    BlockProtLogger.warn("Unknown sub-family tag '" + tag + "' in expression: " + expr);
                    continue;
                }
                if (sf.ownerFamily != family) {
                    BlockProtLogger.warn("Sub-family '" + tag + "' belongs to '"
                        + sf.ownerFamily.name().toLowerCase(Locale.ROOT)
                        + "', not '" + family.name().toLowerCase(Locale.ROOT)
                        + "': ignored in: " + expr);
                    continue;
                }
                enabledSubFamilies.add(sf);
                continue;
            }

            // -*TAG: disable sub-family
            if (token.startsWith("-*")) {
                String tag = token.substring(2).toUpperCase(Locale.ROOT);
                SubFamily sf = SubFamily.byTag(tag);
                if (sf == null) {
                    BlockProtLogger.warn("Unknown sub-family tag '" + tag + "' in expression: " + expr);
                    continue;
                }
                if (sf.ownerFamily != family) {
                    BlockProtLogger.warn("Sub-family '" + tag + "' belongs to '"
                        + sf.ownerFamily.name().toLowerCase(Locale.ROOT)
                        + "', not '" + family.name().toLowerCase(Locale.ROOT)
                        + "': ignored in: " + expr);
                    continue;
                }
                disabledSubFamilies.add(sf);
                disabledSubFamilyExceptions.putIfAbsent(sf, new LinkedHashSet<>());
                continue;
            }

            // -NAME: explicit exclusion
            if (token.startsWith("-") && token.length() > 1) {
                String name = token.substring(1).toUpperCase(Locale.ROOT);
                Material m = Material.matchMaterial(name);
                if (m == null) {
                    BlockProtLogger.warn("Unknown material '" + name + "' in expression: " + expr);
                    continue;
                }
                if (!allMembers.contains(m)) {
                    BlockProtLogger.warn("Material '" + name + "' does not belong to family '"
                        + family.name().toLowerCase(Locale.ROOT)
                        + "': ignored in: " + expr);
                    continue;
                }
                explicitExclusions.add(m);
                continue;
            }

            // NAME: explicit inclusion
            if (!token.isEmpty()) {
                String name = token.toUpperCase(Locale.ROOT);
                Material m = Material.matchMaterial(name);
                if (m == null) {
                    BlockProtLogger.warn("Unknown material '" + name + "' in expression: " + expr);
                    continue;
                }
                if (!allMembers.contains(m)) {
                    BlockProtLogger.warn("Material '" + name + "' does not belong to family '"
                        + family.name().toLowerCase(Locale.ROOT)
                        + "': ignored in: " + expr);
                    continue;
                }
                // If this material belongs to a disabled sub-family, treat it as an exception
                boolean routedToException = false;
                for (SubFamily dsf : disabledSubFamilies) {
                    if (SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet()).contains(m)) {
                        disabledSubFamilyExceptions.get(dsf).add(m);
                        routedToException = true;
                        break;
                    }
                }
                if (!routedToException) explicitInclusions.add(m);
            }
        }

        Set<Material> result = new LinkedHashSet<>();

        if (hasGlobalStar) {
            // Base: everything in the family
            for (Material m : allMembers) {
                boolean inDisabledSF = false;
                for (SubFamily dsf : disabledSubFamilies) {
                    Set<Material> dsMembers = SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet());
                    if (dsMembers.contains(m)) {
                        Set<Material> exc = disabledSubFamilyExceptions.get(dsf);
                        if (exc == null || !exc.contains(m)) { inDisabledSF = true; break; }
                    }
                }
                if (!inDisabledSF && !explicitExclusions.contains(m)) result.add(m);
            }
            result.addAll(explicitInclusions);

        } else if (!enabledSubFamilies.isEmpty()) {
            // Base: union of enabled sub-families
            for (SubFamily sf : enabledSubFamilies) {
                for (Material m : SUBFAMILY_MEMBERS.getOrDefault(sf, Collections.emptySet())) {
                    if (!explicitExclusions.contains(m)) result.add(m);
                }
            }
            // Re-include exceptions from disabled sub-families
            for (Set<Material> exc : disabledSubFamilyExceptions.values()) result.addAll(exc);
            result.addAll(explicitInclusions);

        } else if (!disabledSubFamilies.isEmpty()) {
            // Base: all family members minus disabled sub-families
            for (Material m : allMembers) {
                boolean inDisabled = false;
                for (SubFamily dsf : disabledSubFamilies) {
                    if (SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet()).contains(m)) {
                        inDisabled = true; break;
                    }
                }
                if (!inDisabled) result.add(m);
            }
            for (Set<Material> exc : disabledSubFamilyExceptions.values()) result.addAll(exc);
            result.addAll(explicitInclusions);
            result.removeAll(explicitExclusions);

        } else {
            // No star, no sub-families: explicit inclusions only
            result.addAll(explicitInclusions);
        }

        return result;
    }

    /**
     * Converts a flat list of Materials into the most compact sub-family-aware
     * family expression for the given family key.
     * Emits {@code NAME} for individual inclusions and {@code -NAME} for exclusions.
     */
    @Nullable
    public static String toFamilyExpression(
            @NotNull Collection<Material> materials,
            @NotNull Family family) {

        Set<Material> all = FAMILY_MEMBERS.getOrDefault(family, Collections.emptySet());
        if (all.isEmpty()) return null;

        Set<Material> present = new HashSet<>(materials);
        Set<Material> missing = new LinkedHashSet<>();
        for (Material m : all) { if (!present.contains(m)) missing.add(m); }

        if (missing.isEmpty()) return "[*]";

        List<SubFamily> sfs = new ArrayList<>();
        for (SubFamily sf : SubFamily.values()) { if (sf.ownerFamily == family) sfs.add(sf); }

        Map<SubFamily, SfStatus> sfStatus = new EnumMap<>(SubFamily.class);
        for (SubFamily sf : sfs) {
            Set<Material> sfMembers = SUBFAMILY_MEMBERS.getOrDefault(sf, Collections.emptySet());
            if (sfMembers.isEmpty()) { sfStatus.put(sf, SfStatus.FULL); continue; }
            long active = sfMembers.stream().filter(present::contains).count();
            sfStatus.put(sf, active == sfMembers.size() ? SfStatus.FULL
                           : active == 0               ? SfStatus.NONE
                                                       : SfStatus.PARTIAL);
        }

        Set<Material> ungrouped = new LinkedHashSet<>();
        for (Material m : all) {
            boolean inSf = false;
            for (SubFamily sf : sfs) {
                if (SUBFAMILY_MEMBERS.getOrDefault(sf, Collections.emptySet()).contains(m)) {
                    inSf = true; break;
                }
            }
            if (!inSf) ungrouped.add(m);
        }
        Set<Material> ungroupedPresent = new LinkedHashSet<>();
        Set<Material> ungroupedMissing = new LinkedHashSet<>();
        for (Material m : ungrouped) {
            if (present.contains(m)) ungroupedPresent.add(m); else ungroupedMissing.add(m);
        }

        boolean allClean = sfs.stream().allMatch(sf -> sfStatus.get(sf) != SfStatus.PARTIAL);
        if (allClean) {
            List<SubFamily> activeSfs   = sfs.stream().filter(sf -> sfStatus.get(sf) == SfStatus.FULL).toList();
            List<SubFamily> inactiveSfs = sfs.stream().filter(sf -> sfStatus.get(sf) == SfStatus.NONE).toList();
            if (inactiveSfs.isEmpty()) {
                StringBuilder sb = new StringBuilder("[*");
                for (Material m : ungroupedMissing) sb.append(" -").append(m.name());
                sb.append("]");
                return sb.toString();
            }
            boolean useStar = activeSfs.size() > inactiveSfs.size();
            StringBuilder sb = new StringBuilder("[");
            if (useStar) {
                sb.append("*");
                for (SubFamily sf : inactiveSfs) sb.append(" -*").append(sf.tag);
                for (Material m : ungroupedMissing) sb.append(" -").append(m.name());
            } else {
                boolean first = true;
                for (SubFamily sf : activeSfs) {
                    if (!first) sb.append(" ");
                    sb.append("*-").append(sf.tag);
                    first = false;
                }
                for (Material m : ungroupedPresent) {
                    if (!first) sb.append(" ");
                    sb.append(m.name());
                    first = false;
                }
            }
            sb.append("]");
            return sb.toString();
        }

        if (missing.size() <= present.size()) {
            StringBuilder sb = new StringBuilder("[*");
            for (SubFamily sf : sfs) {
                if (sfStatus.get(sf) == SfStatus.NONE) sb.append(" -*").append(sf.tag);
            }
            for (SubFamily sf : sfs) {
                if (sfStatus.get(sf) == SfStatus.PARTIAL) {
                    appendPartialSubFamily(sb, sf, present, false, true);
                }
            }
            for (Material m : ungroupedMissing) sb.append(" -").append(m.name());
            sb.append("]");
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (SubFamily sf : sfs) {
                if (sfStatus.get(sf) == SfStatus.FULL) {
                    if (!first) sb.append(" ");
                    sb.append("*-").append(sf.tag);
                    first = false;
                }
            }
            for (SubFamily sf : sfs) {
                if (sfStatus.get(sf) == SfStatus.PARTIAL) {
                    appendPartialSubFamily(sb, sf, present, first, false);
                    first = false;
                }
            }
            for (Material m : ungroupedPresent) {
                if (!first) sb.append(" ");
                sb.append(m.name());
                first = false;
            }
            sb.append("]");
            return first ? null : sb.toString();
        }
    }

    /**
     * Appends the compact representation of a PARTIAL sub-family: whichever of
     * exclusion form ({@code *-TAG -missing...}) or direct inclusion form
     * ({@code present1 present2...}) is shorter for that sub-family alone.
     * {@code isFirstToken} controls whether a separating space is written before
     * the first token in the builder. {@code globalStarContext} must be true when
     * the enclosing expression already opens with a global {@code *}: the bare
     * inclusion form only excludes the missing members correctly when there is no
     * surrounding {@code *}, so under a global star this always falls back to the
     * exclusion form regardless of which form is shorter.
     */
    private static void appendPartialSubFamily(
            @NotNull StringBuilder sb, @NotNull SubFamily sf,
            @NotNull Set<Material> present, boolean isFirstToken, boolean globalStarContext) {
        Set<Material> sfMembers = SUBFAMILY_MEMBERS.getOrDefault(sf, Collections.emptySet());
        List<Material> sfPresent = new ArrayList<>();
        List<Material> sfMissing = new ArrayList<>();
        for (Material m : sfMembers) {
            if (present.contains(m)) sfPresent.add(m); else sfMissing.add(m);
        }

        if (!isFirstToken) sb.append(" ");

        if (globalStarContext || sfMissing.size() <= sfPresent.size()) {
            sb.append("*-").append(sf.tag);
            for (Material m : sfMissing) sb.append(" -").append(m.name());
        } else {
            boolean localFirst = true;
            for (Material m : sfPresent) {
                if (!localFirst) sb.append(" ");
                sb.append(m.name());
                localFirst = false;
            }
        }
    }

    private enum SfStatus { FULL, NONE, PARTIAL }

    /**
     * Same as {@link #parseFamilyExpression(String, Family)} but suppresses all
     * cross-family warnings. Use this when iterating all families to resolve a
     * multi-family expression (e.g. auto_drop_to_inventory) so that tokens like
     * {@code *-SHULKERS} do not produce spurious WARN lines when tested against
     * TILE_ENTITIES, BLOCKS, DOORS, and ENTITIES.
     */
    @NotNull
    public static Set<Material> parseFamilyExpressionSilent(@NotNull String expr, @NotNull Family family) {
        String inner = expr.substring(1, expr.length() - 1).trim();
        if (inner.isEmpty()) return new LinkedHashSet<>();

        Set<Material> allMembers = FAMILY_MEMBERS.getOrDefault(family, Collections.emptySet());
        List<String>  tokens     = new ArrayList<>(Arrays.asList(inner.split("\\s+")));
        boolean hasGlobalStar = tokens.contains("*");

        Set<SubFamily> enabledSF   = new LinkedHashSet<>();
        Set<SubFamily> disabledSF  = new LinkedHashSet<>();
        Set<Material>  inclusions  = new LinkedHashSet<>();
        Set<Material>  exclusions  = new LinkedHashSet<>();
        Map<SubFamily, Set<Material>> sfExceptions = new LinkedHashMap<>();

        for (String token : tokens) {
            if (token.equals("*")) continue;
            if (token.startsWith("*-")) {
                String tag = token.substring(2).toUpperCase(Locale.ROOT);
                SubFamily sf = SubFamily.byTag(tag);
                if (sf == null || sf.ownerFamily != family) continue; // silent
                enabledSF.add(sf);
            } else if (token.startsWith("-*")) {
                String tag = token.substring(2).toUpperCase(Locale.ROOT);
                SubFamily sf = SubFamily.byTag(tag);
                if (sf == null || sf.ownerFamily != family) continue; // silent
                disabledSF.add(sf);
                sfExceptions.putIfAbsent(sf, new LinkedHashSet<>());
            } else if (token.startsWith("-") && token.length() > 1) {
                Material m = Material.matchMaterial(token.substring(1).toUpperCase(Locale.ROOT));
                if (m != null && allMembers.contains(m)) exclusions.add(m);
            } else if (!token.isEmpty()) {
                Material m = Material.matchMaterial(token.toUpperCase(Locale.ROOT));
                if (m == null || !allMembers.contains(m)) continue;
                boolean routed = false;
                for (SubFamily dsf : disabledSF) {
                    if (SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet()).contains(m)) {
                        sfExceptions.get(dsf).add(m); routed = true; break;
                    }
                }
                if (!routed) inclusions.add(m);
            }
        }

        Set<Material> result = new LinkedHashSet<>();
        if (hasGlobalStar) {
            for (Material m : allMembers) {
                boolean blocked = false;
                for (SubFamily dsf : disabledSF) {
                    Set<Material> ds = SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet());
                    if (ds.contains(m)) {
                        Set<Material> exc = sfExceptions.get(dsf);
                        if (exc == null || !exc.contains(m)) { blocked = true; break; }
                    }
                }
                if (!blocked && !exclusions.contains(m)) result.add(m);
            }
            result.addAll(inclusions);
        } else if (!enabledSF.isEmpty()) {
            for (SubFamily sf : enabledSF)
                for (Material m : SUBFAMILY_MEMBERS.getOrDefault(sf, Collections.emptySet()))
                    if (!exclusions.contains(m)) result.add(m);
            for (Set<Material> exc : sfExceptions.values()) result.addAll(exc);
            result.addAll(inclusions);
        } else if (!disabledSF.isEmpty()) {
            for (Material m : allMembers) {
                boolean inDis = false;
                for (SubFamily dsf : disabledSF)
                    if (SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet()).contains(m)) { inDis = true; break; }
                if (!inDis) result.add(m);
            }
            for (Set<Material> exc : sfExceptions.values()) result.addAll(exc);
            result.addAll(inclusions);
            result.removeAll(exclusions);
        } else {
            result.addAll(inclusions);
        }
        return result;
    }
}
