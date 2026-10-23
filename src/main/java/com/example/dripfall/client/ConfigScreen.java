package com.example.dripfall.client;

import com.example.dripfall.SettingsPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;

/** Własne, ładne menu moda (rysowane ręcznie, bez zwykłych przycisków Minecrafta). */
public class ConfigScreen extends Screen {

    // --- kolory (ARGB) ---
    private static final int ACCENT = 0xFFD9923F;
    private static final int ACCENT_DARK = 0xFF8A5A22;
    private static final int PANEL = 0xF0181A20;
    private static final int TRACK = 0xFF2A2D36;
    private static final int BTN = 0xFF2F333D;
    private static final int BTN_HOVER = 0xFF42475A;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A4B0;
    private static final int ON = 0xFF4CAF50;
    private static final int OFF = 0xFF5A5E6B;

    private static final float SPEED_MIN = 0.5f, SPEED_MAX = 10.0f;
    private static final String[] PRESET_NAMES = {"Wolno", "Normalnie", "Szybko", "MEGA"};
    private static final float[] PRESET_SPEEDS = {1.0f, 3.0f, 6.0f, 10.0f};

    private static final int PW = 300;
    private static final int PH = 278;

    private record Rect(int x, int y, int w, int h) {
        boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final Screen parent;

    // wartości
    private boolean enabled;
    private float speed;
    private int radius;
    private int interval;
    private boolean onlyOpen;

    // układ
    private int px, py;
    private Rect modToggle, openToggle, speedSlider, radiusSlider, intervalSlider, btnReset, btnDone;
    private final Rect[] presets = new Rect[4];

    private int dragging = -1; // 0 = prędkość, 1 = zasięg, 2 = odstęp

    public ConfigScreen(Screen parent) {
        super(Text.literal("DripFall"));
        this.parent = parent;
        SettingsPayload c = DripFallClient.current;
        enabled = c.enabled();
        speed = c.speed();
        radius = c.radius();
        interval = c.interval();
        onlyOpen = c.onlyOpen();
    }

    @Override
    protected void init() {
        px = width / 2 - PW / 2;
        py = Math.max(4, (height - PH) / 2);
        int cx = px + 16;
        int cw = PW - 32;

        modToggle = new Rect(cx, py + 46, cw, 18);
        speedSlider = new Rect(cx, py + 74, cw, 28);
        radiusSlider = new Rect(cx, py + 106, cw, 28);
        intervalSlider = new Rect(cx, py + 138, cw, 28);
        openToggle = new Rect(cx, py + 174, cw, 18);

        int gap = 6;
        int bw = (cw - gap * 3) / 4;
        for (int i = 0; i < 4; i++) {
            presets[i] = new Rect(cx + i * (bw + gap), py + 214, bw, 18);
        }

        int half = (cw - gap) / 2;
        btnReset = new Rect(cx, py + 244, half, 20);
        btnDone = new Rect(cx + half + gap, py + 244, half, 20);
    }

    // ------------------------------------------------------------------ rysowanie

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // tło

        // cień + panel + ramka
        ctx.fill(px - 3, py - 3, px + PW + 3, py + PH + 3, 0x66000000);
        ctx.fill(px, py, px + PW, py + PH, PANEL);
        ctx.drawBorder(px, py, PW, PH, ACCENT_DARK);

        // nagłówek
        ctx.fillGradient(px + 1, py + 1, px + PW - 1, py + 34, ACCENT, ACCENT_DARK);
        ctx.fill(px + 1, py + 34, px + PW - 1, py + 35, 0xFF000000);
        ctx.getMatrices().push();
        ctx.getMatrices().translate((float) (px + 14), (float) (py + 8), 0f);
        ctx.getMatrices().scale(1.6f, 1.6f, 1.0f);
        ctx.drawTextWithShadow(textRenderer, Text.literal("DripFall"), 0, 0, TEXT);
        ctx.getMatrices().pop();
        String sub = "klapy rzucaja dripstone";
        ctx.drawTextWithShadow(textRenderer, Text.literal(sub),
                px + PW - 14 - textRenderer.getWidth(sub), py + 14, 0xFFFFE9C8);

        // przełączniki
        drawToggle(ctx, modToggle, "Mod", enabled, mouseX, mouseY);
        drawToggle(ctx, openToggle, "Tylko otwarte klapy", onlyOpen, mouseX, mouseY);

        // suwaki
        drawSlider(ctx, speedSlider, "Prędkość spadania",
                String.format(Locale.ROOT, "%.1f bl/tick", speed),
                (speed - SPEED_MIN) / (SPEED_MAX - SPEED_MIN), mouseX, mouseY, dragging == 0);
        drawSlider(ctx, radiusSlider, "Zasięg wokół Ciebie",
                radius + " bloków",
                (radius - 2) / 14.0f, mouseX, mouseY, dragging == 1);
        drawSlider(ctx, intervalSlider, "Częstotliwość",
                String.format(Locale.ROOT, "co %d t (%.1f/s)", interval, 20.0f / interval),
                (40 - interval) / 39.0f, mouseX, mouseY, dragging == 2);

        // presety prędkości
        ctx.drawTextWithShadow(textRenderer, Text.literal("Szybkie ustawienia prędkości"),
                px + 16, py + 202, TEXT_DIM);
        for (int i = 0; i < 4; i++) {
            boolean active = Math.abs(speed - PRESET_SPEEDS[i]) < 0.05f;
            drawButton(ctx, presets[i], PRESET_NAMES[i], mouseX, mouseY, active);
        }

        // dolne przyciski
        drawButton(ctx, btnReset, "Reset", mouseX, mouseY, false);
        drawButton(ctx, btnDone, "Gotowe", mouseX, mouseY, true);
    }

    private void drawToggle(DrawContext ctx, Rect r, String label, boolean on, int mx, int my) {
        boolean hover = r.hit(mx, my);
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), r.x(), r.y() + 5, hover ? TEXT : 0xFFE0E2EA);

        int tw = 34, th = 14;
        int tx = r.x() + r.w() - tw;
        int ty = r.y() + 2;
        ctx.fill(tx, ty, tx + tw, ty + th, on ? ON : OFF);
        ctx.drawBorder(tx, ty, tw, th, 0xFF000000);
        int kx = on ? tx + tw - th + 2 : tx + 2;
        ctx.fill(kx, ty + 2, kx + th - 4, ty + th - 2, TEXT);

        String state = on ? "WŁ." : "WYŁ.";
        ctx.drawTextWithShadow(textRenderer, Text.literal(state),
                tx - 8 - textRenderer.getWidth(state), r.y() + 5, on ? ON : TEXT_DIM);
    }

    private void drawSlider(DrawContext ctx, Rect r, String label, String value, float t,
                            int mx, int my, boolean active) {
        t = MathHelper.clamp(t, 0f, 1f);
        boolean hover = r.hit(mx, my) || active;
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), r.x(), r.y(), 0xFFE0E2EA);
        ctx.drawTextWithShadow(textRenderer, Text.literal(value),
                r.x() + r.w() - textRenderer.getWidth(value), r.y(), ACCENT);

        int ty = r.y() + 16;
        ctx.fill(r.x(), ty, r.x() + r.w(), ty + 6, TRACK);
        int fw = (int) (r.w() * t);
        ctx.fill(r.x(), ty, r.x() + fw, ty + 6, ACCENT);
        ctx.drawBorder(r.x(), ty, r.w(), 6, 0xFF000000);

        int kx = r.x() + fw;
        ctx.fill(kx - 3, ty - 3, kx + 3, ty + 9, hover ? 0xFFFFE9C8 : TEXT);
        ctx.drawBorder(kx - 3, ty - 3, 6, 12, 0xFF000000);
    }

    private void drawButton(DrawContext ctx, Rect r, String label, int mx, int my, boolean highlighted) {
        boolean hover = r.hit(mx, my);
        int bg = highlighted ? ACCENT_DARK : (hover ? BTN_HOVER : BTN);
        if (highlighted && hover) bg = ACCENT;
        ctx.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), bg);
        ctx.drawBorder(r.x(), r.y(), r.w(), r.h(), highlighted ? ACCENT : 0xFF000000);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(label),
                r.x() + r.w() / 2, r.y() + (r.h() - 8) / 2, TEXT);
    }

    // ------------------------------------------------------------------ myszka

    private void click() {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (modToggle.hit(mx, my)) { enabled = !enabled; click(); return true; }
            if (openToggle.hit(mx, my)) { onlyOpen = !onlyOpen; click(); return true; }

            if (speedSlider.hit(mx, my)) { dragging = 0; setSlider(0, mx); return true; }
            if (radiusSlider.hit(mx, my)) { dragging = 1; setSlider(1, mx); return true; }
            if (intervalSlider.hit(mx, my)) { dragging = 2; setSlider(2, mx); return true; }

            for (int i = 0; i < 4; i++) {
                if (presets[i].hit(mx, my)) { speed = PRESET_SPEEDS[i]; click(); return true; }
            }
            if (btnReset.hit(mx, my)) {
                SettingsPayload d = SettingsPayload.defaults();
                speed = d.speed();
                radius = d.radius();
                interval = d.interval();
                onlyOpen = d.onlyOpen();
                click();
                return true;
            }
            if (btnDone.hit(mx, my)) { click(); close(); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging >= 0) {
            setSlider(dragging, mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = -1;
        return super.mouseReleased(mx, my, button);
    }

    private void setSlider(int which, double mx) {
        Rect r = which == 0 ? speedSlider : which == 1 ? radiusSlider : intervalSlider;
        float t = MathHelper.clamp((float) ((mx - r.x()) / r.w()), 0f, 1f);
        switch (which) {
            case 0 -> speed = Math.round((SPEED_MIN + t * (SPEED_MAX - SPEED_MIN)) * 10f) / 10f;
            case 1 -> radius = 2 + Math.round(t * 14f);
            case 2 -> interval = 40 - Math.round(t * 39f);
        }
    }

    // ------------------------------------------------------------------ zamykanie

    @Override
    public void removed() {
        DripFallClient.current = new SettingsPayload(enabled, speed, radius, interval, onlyOpen).clamped();
        DripFallClient.send();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
