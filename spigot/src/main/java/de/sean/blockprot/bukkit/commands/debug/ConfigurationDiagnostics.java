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

package de.sean.blockprot.bukkit.commands.debug;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ConfigurationDiagnostics {

    private ConfigurationDiagnostics() {
    }

    public static void run(@NotNull DiagnosticsContext ctx) {
        DiagnosticsContext.runDomain("2/10", "CONFIGURATION & BLOCK FAMILIES");
        checkLockableMaterials(ctx);
        checkAutoDrop(ctx);
        checkLockableEntities(ctx);
        checkItemFrameProtection(ctx);
        checkRaidDetection(ctx);
        checkVillagerWorkstationProtection(ctx);
    }

    private static void checkLockableMaterials(@NotNull DiagnosticsContext ctx) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            Material[] check = {
                Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
                Material.FURNACE, Material.HOPPER, Material.DROPPER, Material.DISPENSER,
                Material.SHULKER_BOX, Material.OAK_DOOR, Material.OAK_TRAPDOOR,
                Material.BLAST_FURNACE, Material.SMOKER
            };
            StringBuilder sb = new StringBuilder();
            for (Material m : check) sb.append(m.name()).append("=").append(cfg.isLockable(m)).append(" ");
            BlockProtLogger.pass("Lockable blocks: " + sb.toString().trim());
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("Lockable blocks", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkAutoDrop(@NotNull DiagnosticsContext ctx) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean enabled = cfg.isAutoDropToInventoryEnabled();
            StringBuilder sb = new StringBuilder("enabled=" + enabled + " ");
            if (enabled) {
                Material[] check = {
                    Material.CHEST, Material.FURNACE, Material.OAK_DOOR, Material.RED_BED
                };
                for (Material m : check) {
                    sb.append(m.name()).append("=").append(cfg.isAutoDropToInventory(m)).append(" ");
                }
            }
            BlockProtLogger.pass("AutoDrop: " + sb.toString().trim());
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("AutoDrop", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkLockableEntities(@NotNull DiagnosticsContext ctx) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            Set<Material> entityFamily = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.ENTITIES);
            if (entityFamily.isEmpty()) {
                BlockProtLogger.fail("Lockable entities", "ENTITIES family is empty (no entity materials registered)");
                ctx.fail();
                return;
            }

            List<Material> active = new ArrayList<>();
            List<Material> inactive = new ArrayList<>();
            for (Material m : entityFamily) {
                if (cfg.isLockableEntity(m)) active.add(m);
                else inactive.add(m);
            }

            boolean configEmpty = active.isEmpty();
            if (configEmpty) {
                if (cfg.isLockableEntity(Material.CHEST_MINECART)) {
                    BlockProtLogger.fail("Lockable entities", "CHEST_MINECART reports lockable but lockable_entities is empty");
                    ctx.fail();
                } else {
                    BlockProtLogger.pass("Lockable entities: empty (vehicle/frame protection disabled per blocks.yml, CHEST_MINECART spot-check OK)");
                    ctx.pass();
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (Material m : active) sb.append(m.name()).append(" ");
                BlockProtLogger.pass("Lockable entities: active=[" + sb.toString().trim() + "] count=" + active.size());
                ctx.pass();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("Lockable entities", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkItemFrameProtection(@NotNull DiagnosticsContext ctx) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean frameActive = cfg.isLockableEntity(Material.ITEM_FRAME);
            boolean glowActive = cfg.isLockableEntity(Material.GLOW_ITEM_FRAME);

            BlockProtLogger.pass("Item frame protection: "
                + (frameActive ? "enabled" : "disabled")
                + " (ITEM_FRAME=" + (frameActive ? "ACTIVE" : "INACTIVE")
                + " GLOW_ITEM_FRAME=" + (glowActive ? "ACTIVE" : "INACTIVE") + ")");
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("Item frame protection", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkRaidDetection(@NotNull DiagnosticsContext ctx) {
        try {
            boolean enabled = BlockProt.getInstance().getConfig().getBoolean("raid_detection.enabled", false);
            boolean explosionProtect = BlockProt.getDefaultConfig().shouldProtectLockedBlocksFromExplosions();
            AuditLogger auditPresent = BlockProt.getAuditLogger();

            BlockProtLogger.pass("Raid detection: enabled=" + enabled
                + " explosionProtect=" + explosionProtect
                + " auditLogger=" + (auditPresent != null ? "active" : "disabled")
                + " RAID_EXPLOSION action=" + AuditLogger.Action.RAID_EXPLOSION.name());
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("Raid detection", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkVillagerWorkstationProtection(@NotNull DiagnosticsContext ctx) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean enabled = cfg.isVillagerWorkstationProtectionEnabled();
            int radius = cfg.getVillagerWorkstationProtectionRadius();
            int vRadius = cfg.getVillagerWorkstationProtectionVerticalRadius();

            boolean radiusInBounds = radius >= 0 && radius <= 8;
            boolean vRadiusInBounds = vRadius >= 0 && vRadius <= 4;

            if (!radiusInBounds || !vRadiusInBounds) {
                BlockProtLogger.fail("Villager workstation protection",
                    "radius/vertical_radius out of documented bounds: radius=" + radius + " vRadius=" + vRadius);
                ctx.fail();
                return;
            }

            BlockProtLogger.pass("Villager workstation protection: enabled=" + enabled
                + " radius=" + radius + " verticalRadius=" + vRadius
                + " (independent toggle from entity_protection.enabled=" + cfg.isEntityProtectionEnabled() + ")");
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("Villager workstation protection", e.getMessage());
            ctx.fail();
        }
    }
}
