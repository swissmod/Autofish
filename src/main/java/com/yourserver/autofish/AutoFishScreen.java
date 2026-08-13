package com.yourserver.autofish;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class AutoFishScreen extends Screen {

    private final AutoFishConfig config;

    private ButtonWidget toggleButton;
    private TextFieldWidget minDelayField;
    private TextFieldWidget maxDelayField;
    private TextFieldWidget minReactionField;
    private TextFieldWidget maxReactionField;
    private ButtonWidget randomizeReactionButton;
    private ButtonWidget pauseOnFullInvButton;
    private TextFieldWidget sensitivityField;
    private TextFieldWidget teleportDistanceField;

    public AutoFishScreen(AutoFishConfig config) {
        super(Text.literal("AutoFish Settings"));
        this.config = config;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 90;

        toggleButton = ButtonWidget.builder(toggleLabel(), btn -> {
                    config.enabled = !config.enabled;
                    btn.setMessage(toggleLabel());
                })
                .dimensions(centerX - 100, y, 200, 20)
                .build();
        addDrawableChild(toggleButton);
        y += 30;

        minDelayField = numberField(centerX - 100, y, config.minDelayMs);
        maxDelayField = numberField(centerX + 5, y, config.maxDelayMs);
        addDrawableChild(minDelayField);
        addDrawableChild(maxDelayField);
        y += 30;

        randomizeReactionButton = ButtonWidget.builder(reactionToggleLabel(), btn -> {
                    config.randomizeReactionTime = !config.randomizeReactionTime;
                    btn.setMessage(reactionToggleLabel());
                })
                .dimensions(centerX - 100, y, 200, 20)
                .build();
        addDrawableChild(randomizeReactionButton);
        y += 24;

        minReactionField = numberField(centerX - 100, y, config.minReactionMs);
        maxReactionField = numberField(centerX + 5, y, config.maxReactionMs);
        addDrawableChild(minReactionField);
        addDrawableChild(maxReactionField);
        y += 30;

        pauseOnFullInvButton = ButtonWidget.builder(pauseLabel(), btn -> {
                    config.pauseOnFullInventory = !config.pauseOnFullInventory;
                    btn.setMessage(pauseLabel());
                })
                .dimensions(centerX - 100, y, 200, 20)
                .build();
        addDrawableChild(pauseOnFullInvButton);
        y += 30;

        sensitivityField = new TextFieldWidget(this.textRenderer, centerX - 100, y, 90, 20, Text.literal(""));
        sensitivityField.setText(String.valueOf(config.biteSensitivity));
        sensitivityField.setTextPredicate(s -> s.isEmpty() || s.matches("\\d{0,3}(\\.\\d{0,4})?"));
        addDrawableChild(sensitivityField);
        y += 30;

        teleportDistanceField = new TextFieldWidget(this.textRenderer, centerX - 100, y, 90, 20, Text.literal(""));
        teleportDistanceField.setText(String.valueOf(config.teleportResetDistance));
        teleportDistanceField.setTextPredicate(s -> s.isEmpty() || s.matches("\\d{0,4}(\\.\\d{0,2})?"));
        addDrawableChild(teleportDistanceField);
        y += 30;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
                .dimensions(centerX - 100, y, 200, 20)
                .build());
    }

    private TextFieldWidget numberField(int x, int y, int initialValue) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, 90, 20, Text.literal(""));
        field.setText(String.valueOf(initialValue));
        field.setTextPredicate(s -> s.isEmpty() || s.matches("\\d{1,6}"));
        return field;
    }

    private Text toggleLabel() {
        return Text.literal("AutoFish: " + (config.enabled ? "ON" : "OFF"));
    }

    private Text reactionToggleLabel() {
        return Text.literal("Randomized Reaction: " + (config.randomizeReactionTime ? "ON" : "OFF"));
    }

    private Text pauseLabel() {
        return Text.literal("Pause When Inventory Full: " + (config.pauseOnFullInventory ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, this.height / 2 - 110, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Recast delay (ms)"), centerX - 100, this.height / 2 - 90 + 32, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.literal("min / max"), centerX + 5, this.height / 2 - 90 + 32, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Reaction time (ms) min / max"), centerX - 100, this.height / 2 - 90 + 86, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Bite sensitivity (lower = more sensitive)"), centerX - 100, this.height / 2 - 90 + 140, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Teleport reset distance (blocks)"), centerX - 100, this.height / 2 - 90 + 194, 0xA0A0A0);
    }

    private void applyFields() {
        config.minDelayMs = parseOrKeep(minDelayField.getText(), config.minDelayMs);
        config.maxDelayMs = parseOrKeep(maxDelayField.getText(), config.maxDelayMs);
        config.minReactionMs = parseOrKeep(minReactionField.getText(), config.minReactionMs);
        config.maxReactionMs = parseOrKeep(maxReactionField.getText(), config.maxReactionMs);
        config.biteSensitivity = parseOrKeepDouble(sensitivityField.getText(), config.biteSensitivity);
        config.teleportResetDistance = parseOrKeepDouble(teleportDistanceField.getText(), config.teleportResetDistance);
        config.clampValues();
    }

    private int parseOrKeep(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double parseOrKeepDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void close() {
        applyFields();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
