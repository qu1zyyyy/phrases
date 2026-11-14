package ru.phrases;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class PhraseWheelScreen extends Screen {
    private record Phrase(String id, String name, net.minecraft.sounds.SoundEvent sound) {}

    private static final List<Phrase> FIRST_WHEEL = List.of(
            new Phrase("borschish", "Борщищ", PhrasesClient.BORSCHISH),
            new Phrase("am_am_am", "Ам ам ам", PhrasesClient.AM_AM_AM),
            new Phrase("not_funny", "Не смешно", PhrasesClient.NOT_FUNNY),
            new Phrase("animal", "Животное ебанное", PhrasesClient.ANIMAL),
            new Phrase("vyvod", "Вывод", PhrasesClient.VYVOD),
            new Phrase("med_po_tely", "Мед по телу", PhrasesClient.MED_PO_TELY),
            new Phrase("baba_chai", "Баба чай", PhrasesClient.BABA_CHAI),
            new Phrase("dolboeb", "Долбоеб", PhrasesClient.DOLBOEB)
    );
    private static final List<Phrase> SECOND_WHEEL = List.of(
            new Phrase("eblan", "Еблан", PhrasesClient.EBLAN)
    );

    private static final int PANEL = 0xE8171A24;
    private static final int PANEL_EDGE = 0xFF3B4356;
    private static final int SEGMENT = 0xFF252B3A;
    private static final int SEGMENT_SELECTED = 0xFF4D6EA8;
    private static final int ACCENT = 0xFF8DB5FF;
    private static final int TEXT = 0xFFF2F5FF;
    private int wheelPage;
    private int selectedPhraseIndex = -1;

    public PhraseWheelScreen() {
        super(Component.translatable("key.phrases.open_wheel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        if (PhrasesClient.OPEN_WHEEL.matches(input) && this.minecraft != null) {
            List<Phrase> phrases = wheelPage == 0 ? FIRST_WHEEL : SECOND_WHEEL;
            if (selectedPhraseIndex >= 0 && selectedPhraseIndex < phrases.size()) {
                Phrase phrase = phrases.get(selectedPhraseIndex);
                PhrasesClient.playPhrase(phrase.sound(), phrase.name(), phrase.id(), true, this.minecraft.player);
            }
            this.minecraft.setScreen(null);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        if (click.button() != 0) {
            return false;
        }

        double mouseX = click.x();
        double mouseY = click.y();
        int centerY = this.height / 2;
        if (mouseY >= centerY - 28 && mouseY <= centerY + 28 && mouseX < this.width / 2 - 180) {
            wheelPage = 0;
            selectedPhraseIndex = -1;
            return true;
        }
        if (mouseY >= centerY - 28 && mouseY <= centerY + 28 && mouseX > this.width / 2 + 180) {
            wheelPage = 1;
            selectedPhraseIndex = -1;
            return true;
        }
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int outerRadius = Math.min(148, Math.min(this.width, this.height) / 3);
        int innerRadius = 56;
        List<Phrase> phrases = wheelPage == 0 ? FIRST_WHEEL : SECOND_WHEEL;
        int selected = selectedIndex(mouseX, mouseY, centerX, centerY);
        selectedPhraseIndex = selected;

        drawDisk(graphics, centerX, centerY, outerRadius + 12, PANEL);
        drawDisk(graphics, centerX, centerY, outerRadius + 13, PANEL_EDGE);
        drawDisk(graphics, centerX, centerY, outerRadius + 10, PANEL);

        for (int i = 0; i < phrases.size(); i++) {
            double angle = -Math.PI / 2 + (Math.PI * 2 * i / phrases.size());
            int x = centerX + (int) (Math.cos(angle) * (outerRadius - 14));
            int y = centerY + (int) (Math.sin(angle) * (outerRadius - 14));
            drawDisk(graphics, x, y, 39, i == selected ? SEGMENT_SELECTED : SEGMENT);
            drawDisk(graphics, x, y, 34, i == selected ? 0xFF5D83C2 : 0xFF30384A);
            drawCenteredWrapped(graphics, phrases.get(i).name(), x, y, 54, TEXT);
        }

        drawDisk(graphics, centerX, centerY, innerRadius + 2, ACCENT);
        drawDisk(graphics, centerX, centerY, innerRadius, 0xFF202636);
        SfProPhraseRenderer.drawGuiText(graphics, wheelPage == 0 ? "Фразы 1" : "Фразы 2",
                centerX, centerY - 18, 96, TEXT, 0.25F);
        SfProPhraseRenderer.drawGuiText(graphics, "Удерживай Y",
                centerX, centerY + 2, 100, 0xFFB6C0D8, 0.19F);
        drawChevron(graphics, centerX - outerRadius - 35, centerY, true, TEXT);
        drawChevron(graphics, centerX + outerRadius + 35, centerY, false, TEXT);
        SfProPhraseRenderer.drawGuiText(graphics, "Выбери направление мышью",
                centerX, centerY + outerRadius + 22, 260, 0xFFD8DEEC, 0.18F);
    }

    private int selectedIndex(int mouseX, int mouseY, int centerX, int centerY) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        if (dx * dx + dy * dy < 62 * 62) {
            return -1;
        }

        double angle = Math.atan2(dy, dx) + Math.PI / 2;
        if (angle < 0) {
            angle += Math.PI * 2;
        }
        int count = wheelPage == 0 ? FIRST_WHEEL.size() : SECOND_WHEEL.size();
        return (int) Math.floor((angle + Math.PI / count) / (Math.PI * 2 / count)) % count;
    }

    private void drawCenteredWrapped(GuiGraphics graphics, String text, int centerX, int centerY, int maxWidth, int color) {
        if (text.length() <= 11) {
            SfProPhraseRenderer.drawGuiText(graphics, text, centerX, centerY - 8, maxWidth, color, 0.22F);
            return;
        }

        String[] words = text.split(" ");
        String first = words[0];
        String second = text.substring(first.length()).trim();
        SfProPhraseRenderer.drawGuiText(graphics, first, centerX, centerY - 13, maxWidth, color, 0.20F);
        SfProPhraseRenderer.drawGuiText(graphics, second, centerX, centerY, maxWidth, color, 0.20F);
    }

    private static void drawChevron(GuiGraphics graphics, int centerX, int centerY, boolean left, int color) {
        for (int i = 0; i < 7; i++) {
            int x = left ? centerX + Math.abs(i - 3) : centerX - Math.abs(i - 3);
            graphics.fill(x, centerY - 3 + i, x + 2, centerY - 2 + i, color);
        }
    }

    private static void drawDisk(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int radiusSquared = radius * radius;
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int) Math.sqrt(radiusSquared - y * y);
            graphics.fill(centerX - halfWidth, centerY + y, centerX + halfWidth + 1, centerY + y + 1, color);
        }
    }
}
