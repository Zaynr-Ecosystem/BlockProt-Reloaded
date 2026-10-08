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

import de.sean.blockprot.bukkit.dialogs.DialogBodyEntry;
import de.sean.blockprot.bukkit.dialogs.DialogBridge;
import de.sean.blockprot.bukkit.dialogs.DialogButton;
import de.sean.blockprot.bukkit.dialogs.DialogTextField;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

final class NoopDialogBridge implements DialogBridge {
    @Override
    public void closeDialog(Player player) {
    }

    @Override
    public void showNotice(Player player, Component title, List<Component> body, DialogButton ok) {
    }

    @Override
    public void showConfirmation(Player player, Component title, List<Component> body, DialogButton yes, DialogButton no) {
    }

    @Override
    public void showMultiAction(Player player, Component title, List<Component> body, List<DialogButton> actions) {
    }

    @Override
    public void showMultiAction(Player player, Component title, List<DialogBodyEntry> body, List<DialogButton> actions, DialogButton exit, int columns) {
    }

    @Override
    public void showValueInput(Player player, Component title, List<DialogBodyEntry> body, DialogTextField field, Consumer<String> onSubmit, DialogButton back) {
    }

    @Override
    public void showSearchDialog(Player player, Component title, List<DialogBodyEntry> body, DialogTextField field, List<DialogButton> actions, DialogButton exit, int columns, Consumer<String> onSearch) {
    }
}
