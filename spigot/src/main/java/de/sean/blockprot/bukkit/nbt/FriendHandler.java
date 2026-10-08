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

package de.sean.blockprot.bukkit.nbt;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import org.jetbrains.annotations.NotNull;

/**
 * The friend handler used by {@link BlockNBTHandler} to handle
 * each of the {@link NBTCompound} used in the "friends" sub-tag of each block.
 * A single {@link FriendHandler} itself only handles a *single friend*
 * in the list of friends.
 *
 * @since 0.3.0
 */
public final class FriendHandler extends NBTHandler<NBTCompound> {
    public static final String LEVEL_ATTRIBUTE = "bp_level";
    public static final String FLAGS_ATTRIBUTE = "bp_flags";

    public static final int LEVEL_BASIC = 1;
    public static final int LEVEL_OPERATOR = 2;
    public static final int LEVEL_MANAGER = 3;
    public static final int LEVEL_CUSTOM = 4;

    public static final int FLAG_OPEN_MENU = 1 << 0;
    public static final int FLAG_EDIT_SETTINGS = 1 << 1;
    public static final int FLAG_EDIT_NAME = 1 << 2;
    public static final int FLAG_VIEW_AUDIT = 1 << 3;
    public static final int FLAG_INSPECT = 1 << 4;
    public static final int FLAG_MANAGE_FRIENDS = 1 << 5;
    public static final int FLAG_DEPOSIT = 1 << 6;
    public static final int FLAG_WITHDRAW = 1 << 7;
    public static final int FLAG_REDSTONE = 1 << 8;
    public static final int FLAG_OPEN_CONTAINER = 1 << 9;

    public FriendHandler(@NotNull final NBTCompound compound) {
        super();
        this.container = compound;
    }

    @NotNull
    public String getName() {
        String name = container.getName();
        return name == null ? "" : name;
    }

    public boolean doesRepresentPublic() {
        return getName().equals(FriendSupportingHandler.publicUuid.toString());
    }

    public int getLevel() {
        if (container.hasTag(LEVEL_ATTRIBUTE)) {
            int lvl = container.getInteger(LEVEL_ATTRIBUTE);
            if (lvl >= LEVEL_BASIC && lvl <= LEVEL_CUSTOM) return lvl;
        }
        return LEVEL_BASIC;
    }

    public void setLevel(int level) {
        container.setInteger(LEVEL_ATTRIBUTE, level);
    }

    public int getFlags() {
        if (container.hasTag(FLAGS_ATTRIBUTE)) {
            return container.getInteger(FLAGS_ATTRIBUTE);
        }
        return 0;
    }

    public void setFlags(int flags) {
        container.setInteger(FLAGS_ATTRIBUTE, flags);
    }

    public boolean hasFlag(int flag) {
        return (getFlags() & flag) != 0;
    }

    public void setFlag(int flag, boolean enabled) {
        int current = getFlags();
        setFlags(enabled ? (current | flag) : (current & ~flag));
    }

    public boolean canOpenContainer() {
        return getLevel() != LEVEL_CUSTOM || hasFlag(FLAG_OPEN_CONTAINER);
    }

    public boolean canDeposit() {
        return getLevel() != LEVEL_CUSTOM || hasFlag(FLAG_DEPOSIT);
    }

    public boolean canWithdraw() {
        return getLevel() != LEVEL_CUSTOM || hasFlag(FLAG_WITHDRAW);
    }

    public boolean canUseRedstone() {
        return getLevel() != LEVEL_CUSTOM || hasFlag(FLAG_REDSTONE);
    }

    public boolean canOpenMenu() {
        return switch (getLevel()) {
            case LEVEL_OPERATOR, LEVEL_MANAGER -> true;
            case LEVEL_CUSTOM -> hasFlag(FLAG_OPEN_MENU);
            default -> false;
        };
    }

    public boolean canEditSettings() {
        return switch (getLevel()) {
            case LEVEL_OPERATOR, LEVEL_MANAGER -> true;
            case LEVEL_CUSTOM -> hasFlag(FLAG_EDIT_SETTINGS);
            default -> false;
        };
    }

    public boolean canEditName() {
        return switch (getLevel()) {
            case LEVEL_OPERATOR, LEVEL_MANAGER -> true;
            case LEVEL_CUSTOM -> hasFlag(FLAG_EDIT_NAME);
            default -> false;
        };
    }

    public boolean canViewAudit() {
        return switch (getLevel()) {
            case LEVEL_OPERATOR, LEVEL_MANAGER -> true;
            case LEVEL_CUSTOM -> hasFlag(FLAG_VIEW_AUDIT);
            default -> false;
        };
    }

    public boolean canInspect() {
        return switch (getLevel()) {
            case LEVEL_OPERATOR, LEVEL_MANAGER -> true;
            case LEVEL_CUSTOM -> hasFlag(FLAG_INSPECT);
            default -> false;
        };
    }

    public boolean canManageFriends() {
        return switch (getLevel()) {
            case LEVEL_MANAGER -> true;
            case LEVEL_CUSTOM -> hasFlag(FLAG_MANAGE_FRIENDS);
            default -> false;
        };
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return true;
    }

    public boolean isManager() {
        return canManageFriends();
    }

    @Override
    public void mergeHandler(@NotNull NBTHandler<?> handler) {
        if (handler instanceof FriendHandler fh) {
            setLevel(fh.getLevel());
            setFlags(fh.getFlags());
        }
    }
}