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

package de.sean.blockprot.bukkit.dialogs.impl;

import de.sean.blockprot.bukkit.dialogs.DialogBodyEntry;
import de.sean.blockprot.bukkit.dialogs.DialogBridge;
import de.sean.blockprot.bukkit.dialogs.DialogButton;
import de.sean.blockprot.bukkit.dialogs.DialogTextField;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PaperDialogBridge implements DialogBridge {

    private static final ClickCallback.Options CLICK_OPTIONS =
        ClickCallback.Options.builder().uses(1).build();

    private static volatile boolean closeDialogChecked = false;
    private static volatile @Nullable Method closeDialogMethod = null;

    /*
     * Audience#closeDialog() was added in Adventure 4.24.0 (Minecraft 1.21.6);
     * paper-api 1.21.7 bundles an older Adventure without it. Resolved by
     * reflection against the runtime server; falls back to closeInventory()
     * on a bare 1.21.7 server.
     */
    @Override
    public void closeDialog(@NotNull Player player) {
        closeDialogOrInventory(player);
    }

    private static void closeDialogOrInventory(@NotNull Player player) {
        if (!closeDialogChecked) {
            try {
                closeDialogMethod = player.getClass().getMethod("closeDialog");
            } catch (NoSuchMethodException e) {
                closeDialogMethod = null;
            }
            closeDialogChecked = true;
        }
        Method method = closeDialogMethod;
        if (method != null) {
            try {
                method.invoke(player);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        player.closeInventory();
    }

    @Override
    public void showNotice(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<Component> body,
        @Nullable DialogButton ok
    ) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title)
                .body(body.stream().map(DialogBody::plainMessage).toList())
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .build()
            )
            .type(ok != null
                ? DialogType.notice(toActionButton(ok))
                : DialogType.notice())
        );
        player.showDialog(dialog);
    }

    @Override
    public void showConfirmation(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<Component> body,
        @NotNull DialogButton yes,
        @NotNull DialogButton no
    ) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title)
                .body(body.stream().map(DialogBody::plainMessage).toList())
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .build()
            )
            .type(DialogType.confirmation(toActionButton(yes), toActionButton(no)))
        );
        player.showDialog(dialog);
    }

    @Override
    public void showMultiAction(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<Component> body,
        @NotNull List<DialogButton> actions
    ) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title)
                .body(body.stream().map(DialogBody::plainMessage).toList())
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .build()
            )
            .type(DialogType.multiAction(
                actions.stream().map(this::toActionButton).toList()).build())
        );
        player.showDialog(dialog);
    }

    @Override
    public void showMultiAction(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<DialogBodyEntry> body,
        @NotNull List<DialogButton> actions,
        @Nullable DialogButton exit,
        int columns
    ) {
        List<io.papermc.paper.registry.data.dialog.body.DialogBody> dialogBody
            = new ArrayList<>(body.size());
        for (DialogBodyEntry entry : body) {
            if (entry.text() != null) {
                dialogBody.add(DialogBody.plainMessage(entry.text()));
            } else if (entry.item() != null) {
                dialogBody.add(DialogBody.item(entry.item()).build());
            }
        }
        List<DialogButton> effectiveActions = new ArrayList<>(actions);
        if (effectiveActions.isEmpty() && exit != null) {
            effectiveActions.add(exit);
            exit = null;
        }
        ActionButton exitAction = exit != null ? toActionButton(exit) : null;
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title)
                .body(dialogBody)
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .build()
            )
            .type(DialogType.multiAction(
                effectiveActions.stream().map(this::toActionButton).toList(),
                exitAction,
                columns))
        );
        player.showDialog(dialog);
    }

    @Override
    public void showValueInput(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<DialogBodyEntry> body,
        @NotNull DialogTextField field,
        @NotNull Consumer<String> onSubmit,
        @Nullable DialogButton back
    ) {
        List<io.papermc.paper.registry.data.dialog.body.DialogBody> dialogBody
            = new ArrayList<>(body.size());
        for (DialogBodyEntry entry : body) {
            if (entry.text() != null) {
                dialogBody.add(DialogBody.plainMessage(entry.text()));
            } else if (entry.item() != null) {
                dialogBody.add(DialogBody.item(entry.item()).build());
            }
        }

        TextDialogInput textInput = DialogInput.text(
            field.key(),
            field.width() > 0 ? field.width() : DialogTextField.DEFAULT_WIDTH,
            field.label(),
            true,
            field.initialValue(),
            field.maxLength() > 0 ? field.maxLength() : 32,
            null
        );

        DialogAction confirmAction = DialogAction.customClick(
            (view, audience) -> {
                if (!(audience instanceof Player p)) return;
                String value = view.getText(field.key());
                onSubmit.accept(value != null ? value : field.initialValue());
            },
            CLICK_OPTIONS
        );
        var confirmButton = ActionButton.builder(Component.text(field.confirmLabel())).action(confirmAction).build();

        ActionButton backAction = back != null ? toActionButton(back) : null;

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title)
                .body(dialogBody)
                .inputs(List.of(textInput))
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .build()
            )
            .type(DialogType.multiAction(
                List.of(confirmButton),
                backAction,
                1))
        );
        player.showDialog(dialog);
    }

    @Override
    public void showSearchDialog(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<DialogBodyEntry> body,
        @NotNull DialogTextField field,
        @NotNull List<DialogButton> actions,
        @Nullable DialogButton exit,
        int columns,
        @NotNull Consumer<String> onSearch
    ) {
        List<io.papermc.paper.registry.data.dialog.body.DialogBody> dialogBody
            = new ArrayList<>(body.size());
        for (DialogBodyEntry entry : body) {
            if (entry.text() != null) {
                dialogBody.add(DialogBody.plainMessage(entry.text()));
            } else if (entry.item() != null) {
                dialogBody.add(DialogBody.item(entry.item()).build());
            }
        }

        TextDialogInput textInput = DialogInput.text(
            field.key(),
            field.width() > 0 ? field.width() : DialogTextField.DEFAULT_WIDTH,
            field.label(),
            true,
            field.initialValue(),
            field.maxLength() > 0 ? field.maxLength() : 32,
            null
        );

        List<ActionButton> actionButtons = new ArrayList<>(actions.size());
        for (DialogButton btn : actions) {
            actionButtons.add(toSearchDialogActionButton(btn, field.key(), onSearch));
        }

        ActionButton exitAction = exit != null ? toActionButton(exit) : null;

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title)
                .body(dialogBody)
                .inputs(List.of(textInput))
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .build()
            )
            .type(DialogType.multiAction(actionButtons, exitAction, columns))
        );
        player.showDialog(dialog);
    }

    private @NotNull ActionButton toSearchDialogActionButton(
        @NotNull DialogButton btn,
        @NotNull String fieldKey,
        @NotNull Consumer<String> onSearch
    ) {
        var handler = btn.onClick();
        DialogAction action = DialogAction.customClick(
            (view, audience) -> {
                if (!(audience instanceof Player player)) return;
                if ("search".equals(btn.id())) {
                    String currentText = view.getText(fieldKey);
                    final String query = currentText != null ? currentText : "";
                    de.sean.blockprot.bukkit.util.RegionTasks.runFor(
                        player,
                        () -> onSearch.accept(query)
                    );
                } else if (handler != null) {
                    de.sean.blockprot.bukkit.util.RegionTasks.runFor(
                        player,
                        () -> handler.handle(player)
                    );
                } else {
                    de.sean.blockprot.bukkit.util.RegionTasks.runFor(
                        player,
                        () -> closeDialogOrInventory(player)
                    );
                }
            },
            CLICK_OPTIONS
        );
        var builder = ActionButton.builder(btn.label()).action(action);
        if (btn.tooltip() != null) {
            builder.tooltip(btn.tooltip());
        }
        return builder.build();
    }

    private @NotNull ActionButton toActionButton(@NotNull DialogButton btn) {
        var handler = btn.onClick();
        DialogAction action;
        if (handler == null) {
            action = DialogAction.customClick(
                (view, audience) -> {
                    if (audience instanceof Player player) {
                        de.sean.blockprot.bukkit.util.RegionTasks.runFor(player, () -> closeDialogOrInventory(player));
                    }
                },
                CLICK_OPTIONS
            );
        } else {
            action = DialogAction.customClick(
                (view, audience) -> {
                    if (audience instanceof Player player) {
                        de.sean.blockprot.bukkit.util.RegionTasks.runFor(player, () -> handler.handle(player));
                    }
                },
                CLICK_OPTIONS
            );
        }
        var builder = ActionButton.builder(btn.label()).action(action);
        if (btn.tooltip() != null) {
            builder.tooltip(btn.tooltip());
        }
        return builder.build();
    }
}
