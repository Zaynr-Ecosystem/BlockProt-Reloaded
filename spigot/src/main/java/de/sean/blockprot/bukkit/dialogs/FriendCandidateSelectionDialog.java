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

package de.sean.blockprot.bukkit.dialogs;

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.util.PlayerLookup.ScoredMatch;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Dialog counterpart of {@code FriendCandidateSelectionInventory}: lets the
 * owner pick which player they meant from a fuzzy search result list.
 */
public final class FriendCandidateSelectionDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xF0E6A0);

    private static final int PER_PAGE = 6;

    private FriendCandidateSelectionDialog() {}

    public static void show(
            @NotNull Player player,
            @NotNull List<ScoredMatch> candidates,
            @NotNull Consumer<ScoredMatch> onSelect,
            @NotNull Runnable onCancel
    ) {
        show(player, candidates, onSelect, onCancel, 0);
    }

    public static void show(
            @NotNull Player player,
            @NotNull List<ScoredMatch> candidates,
            @NotNull Consumer<ScoredMatch> onSelect,
            @NotNull Runnable onCancel,
            int page
    ) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        int totalPages = Math.max(1, (int) Math.ceil(candidates.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, candidates.size());
        List<ScoredMatch> pageCandidates = candidates.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__SELECT_CANDIDATE)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__SELECT_CANDIDATE_HINT)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                .replace("{current}", String.valueOf(safePage + 1))
                .replace("{total}", String.valueOf(totalPages)),
            TextColor.color(0x888888))));

        List<DialogButton> buttons = new ArrayList<>();
        for (ScoredMatch match : pageCandidates) {
            int pct = (int) Math.round(match.similarity() * 100.0);
            buttons.add(new DialogButton("candidate_" + match.uuid(),
                Component.text(match.name(), NamedTextColor.WHITE),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__SELECT_CANDIDATE_SCORE))
                    .replace("{score}", String.valueOf(pct)), TextColor.color(0x888888)),
                p -> onSelect.accept(match)));
        }

        List<DialogButton> navButtons = new ArrayList<>();
        if (safePage > 0) {
            int prevPage = safePage - 1;
            navButtons.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(player, candidates, onSelect, onCancel, prevPage)));
        }
        if (safePage + 1 < totalPages) {
            int nextPage = safePage + 1;
            navButtons.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(player, candidates, onSelect, onCancel, nextPage)));
        }
        BpDialogStyles.padToRowEnd(buttons, 2);
        buttons.addAll(navButtons);

        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> onCancel.run());

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }
}