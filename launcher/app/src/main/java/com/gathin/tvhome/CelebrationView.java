package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * Five minutes of Kaliady on Home when the countdown reaches its day (Celebration has the timing): the greeting with
 * party poppers, Ziuzia walking in through a blizzard, the goat, the wolf and the bear, Kuttsia, the Belarusian
 * ornament and a straw pavuk, a dance with fireworks, then everyone leaves and the timer comes back. Drawn on one
 * full-screen canvas at about 30 frames a second, only for those five minutes or a preview.
 */
final class CelebrationView extends View {
    interface Done {
        void run();
    }

    private static final int SNOW = 140, CONFETTI = 160;
    private static final int RED = 0xFFC8102E, CREAM = 0xFFF3EAD7, STRAW = 0xFFE6C36A;

    private final float d;
    private final String greeting;
    private final Done done;
    private final long startedAt;
    private final double offset;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final float[] snowX = new float[SNOW], snowY = new float[SNOW], snowS = new float[SNOW];
    private final float[] cX = new float[CONFETTI], cY = new float[CONFETTI], cVX = new float[CONFETTI],
            cVY = new float[CONFETTI], cBorn = new float[CONFETTI];
    private final int[] cCol = new int[CONFETTI];
    private final Random random = new Random(2027);
    private double skip;
    private float lastBurst = -10;
    private int nextConfetti;
    /** False while another app (or a panel over Home) is in front: no frames are drawn then. */
    private boolean running = true;

    /** Home calls this from onResume (true) and onPause (false); the scenes follow the clock, so they catch up. */
    void setRunning(boolean on) {
        running = on;
        if (on) invalidate();
    }

    /** @param offset seconds already passed (Home opened during the celebration); {@code done} runs at the end. */
    CelebrationView(Context ctx, String greeting, double offset, Done done) {
        super(ctx);
        this.greeting = greeting;
        this.offset = offset;
        this.done = done;
        startedAt = SystemClock.uptimeMillis();
        d = ctx.getResources().getDisplayMetrics().density;
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create(UiFont.get(ctx, R.string.font_medium), Typeface.BOLD));
        for (int i = 0; i < SNOW; i++) {
            snowX[i] = random.nextFloat();
            snowY[i] = random.nextFloat();
            snowS[i] = 0.5f + random.nextFloat();
        }
        for (int i = 0; i < CONFETTI; i++) cBorn[i] = -100;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
    }

    /** Preview: jump forward or back by {@code seconds}. */
    void skip(double seconds) {
        skip = Math.max(-offset, skip + seconds);
        invalidate();
    }

    private double now() {
        return offset + skip + (SystemClock.uptimeMillis() - startedAt) / 1000.0;
    }

    @Override
    protected void onDraw(Canvas c) {
        double s = now();
        if (s >= Celebration.LENGTH) {
            if (done != null) done.run();
            return;
        }
        float w = getWidth(), h = getHeight();
        float t = (float) s;
        // Fade in over the first second and out over the last four.
        float fade = (float) Math.min(1, Math.min(s, (Celebration.LENGTH - s) / 4));
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(Math.round(150 * fade), 6, 10, 26));
        c.drawRect(0, 0, w, h, p);
        int saved = c.saveLayerAlpha(0, 0, w, h, Math.round(255 * fade));
        Celebration.Scene scene = Celebration.scene(s);
        snow(c, w, h, t, scene == Celebration.Scene.ZIUZIA ? 2.6f : 1f);
        if (scene.ordinal() >= Celebration.Scene.KUTTSIA.ordinal()) ornament(c, w, h, t, s);
        if (scene == Celebration.Scene.DANCE || scene == Celebration.Scene.FAREWELL) pavuk(c, w, h, t, s);
        poppers(c, w, h, t, scene);
        title(c, w, h, s);
        for (int i = 0; i < Celebration.FIGURES; i++) figure(c, i, s, w, h);
        c.restoreToCount(saved);
        if (running) postInvalidateDelayed(33);
    }

    private void title(Canvas c, float w, float h, double s) {
        Celebration.Scene scene = Celebration.scene(s);
        boolean big = scene == Celebration.Scene.GREETING || scene == Celebration.Scene.FAREWELL;
        float pop = scene == Celebration.Scene.GREETING ? Math.min(1, (float) s / 1.2f) : 1f;
        float pulse = 1 + 0.03f * (float) Math.sin(s * 3);
        text.setTextSize((big ? 96 : 54) * d / 1.5f * pop * pulse);
        text.setColor(0xFFFFF3C4);
        text.setShadowLayer(18, 0, 0, 0xCCFFB300);
        c.drawText(greeting, w / 2, big ? h * 0.36f : h * 0.17f, text);
        text.clearShadowLayer();
    }

    // ---- snow, blizzard ----------------------------------------------------------------------------

    private void snow(Canvas c, float w, float h, float t, float storm) {
        p.setColor(0xDDFFFFFF);
        for (int i = 0; i < SNOW; i++) {
            float fall = (40 + 60 * snowS[i]) * storm;
            float y = (snowY[i] * h + t * fall * d / 1.5f) % (h + 20);
            float wind = storm > 1 ? (float) Math.sin(t * 0.8 + i) * 120 + t * 90 : (float) Math.sin(t * 0.6 + i) * 30;
            float x = ((snowX[i] * w + wind) % w + w) % w;
            c.drawCircle(x, y, (1.5f + 2.5f * snowS[i]) * d / 1.5f, p);
        }
    }

    // ---- party poppers and fireworks ---------------------------------------------------------------

    private void poppers(Canvas c, float w, float h, float t, Celebration.Scene scene) {
        boolean bursting = scene == Celebration.Scene.GREETING || scene == Celebration.Scene.DANCE;
        if (bursting && t - lastBurst > 1.6f) {
            lastBurst = t;
            float x = w * (0.15f + 0.7f * random.nextFloat()), y = scene == Celebration.Scene.DANCE
                    ? h * (0.15f + 0.3f * random.nextFloat()) : h;
            boolean firework = scene == Celebration.Scene.DANCE;
            int[] colors = {RED, 0xFFFFD54F, 0xFF64B5F6, 0xFF81C784, 0xFFBA68C8, CREAM};
            for (int k = 0; k < 40; k++) {
                int i = nextConfetti++ % CONFETTI;
                double a = firework ? random.nextDouble() * Math.PI * 2 : -Math.PI / 2 + (random.nextDouble() - 0.5) * 1.2;
                float speed = (firework ? 160 : 520 + 260 * random.nextFloat()) * d / 1.5f * (0.6f + random.nextFloat());
                cX[i] = x;
                cY[i] = y;
                cVX[i] = (float) Math.cos(a) * speed;
                cVY[i] = (float) Math.sin(a) * speed;
                cBorn[i] = t;
                cCol[i] = colors[random.nextInt(colors.length)];
            }
        }
        for (int i = 0; i < CONFETTI; i++) {
            float age = t - cBorn[i];
            if (age < 0 || age > 3.2f) continue;
            float x = cX[i] + cVX[i] * age, y = cY[i] + cVY[i] * age + 0.5f * 420 * d / 1.5f * age * age;
            p.setColor((cCol[i] & 0x00FFFFFF) | (Math.round(255 * (1 - age / 3.2f)) << 24));
            c.save();
            c.rotate(age * 360 + i * 20, x, y);
            c.drawRect(x - 4, y - 2, x + 4, y + 2, p);
            c.restore();
        }
    }

    // ---- Belarusian ornament and the straw pavuk ---------------------------------------------------

    /** Bands of red rhombs with hooked ends, as on a ruchnik, sliding in along the top and bottom. */
    private void ornament(Canvas c, float w, float h, float t, double s) {
        float in = Math.min(1, (float) (s - 165) / 4);
        float size = 26 * d / 1.5f, band = size * 1.4f;
        float shift = (t * 30) % (size * 2);
        for (int edge = 0; edge < 2; edge++) {
            float cy = edge == 0 ? band / 2 : h - band / 2;
            p.setColor(Color.argb(Math.round(200 * in), 243, 234, 215));
            c.drawRect(0, cy - band / 2, w * in, cy + band / 2, p);
            p.setColor(RED);
            for (float x = -size * 2 + (edge == 0 ? shift : -shift); x < w * in; x += size * 2) {
                rhomb(c, x + size, cy, size * 0.55f);
            }
        }
    }

    private void rhomb(Canvas c, float x, float y, float r) {
        path.reset();
        path.moveTo(x, y - r);
        path.lineTo(x + r, y);
        path.lineTo(x, y + r);
        path.lineTo(x - r, y);
        path.close();
        c.drawPath(path, p);
        // The small hooks ("rogi") at the side corners, and a cream eye in the middle.
        p.setStrokeWidth(2.5f);
        p.setStyle(Paint.Style.STROKE);
        c.drawLine(x + r, y, x + r * 1.5f, y - r * 0.5f, p);
        c.drawLine(x - r, y, x - r * 1.5f, y + r * 0.5f, p);
        p.setStyle(Paint.Style.FILL);
        int was = p.getColor();
        p.setColor(CREAM);
        c.drawRect(x - r * 0.22f, y - r * 0.22f, x + r * 0.22f, y + r * 0.22f, p);
        p.setColor(was);
    }

    private void pavuk(Canvas c, float w, float h, float t, double s) {
        float drop = Math.min(1, (float) (s - 210) / 6);
        float cx = w / 2, cy = h * 0.42f * drop;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(0x99FFFFFF);
        p.setStrokeWidth(1.5f);
        c.drawLine(cx, 0, cx, cy - 70 * d / 1.5f, p);
        p.setColor(STRAW);
        p.setStrokeWidth(3.5f);
        NewYearView.octahedron(c, p, cx, cy, 80 * d / 1.5f, t * 0.5f);
        p.setStrokeWidth(2f);
        for (int k = 0; k < 4; k++) {
            double ang = t * 0.5f - Math.PI / 2 * k;
            float px = cx + (float) Math.cos(ang) * 80 * d / 1.5f;
            float py = cy + (float) (Math.sin(ang) * Math.sin(0.35)) * 80 * d / 1.5f;
            NewYearView.octahedron(c, p, px, py + 40 * d / 1.5f, 18 * d / 1.5f, -t);
        }
        p.setStyle(Paint.Style.FILL);
    }

    // ---- the procession ------------------------------------------------------------------------------

    private void figure(Canvas c, int i, double s, float w, float h) {
        float x = Celebration.figureX(i, s, w);
        if (x < -w * 0.1f || x > w * 1.1f) return;
        float k = h / 1080f * 1.4f;   // drawn for 1080 px high, shown 1.4 times larger to read across the room
        float ground = h * 0.9f - Celebration.hop(i, s) * 46 * k;
        c.save();
        c.translate(x, ground);
        c.scale(k, k);
        c.rotate((float) Math.sin(s * 3 + i) * 5);
        switch (i) {
            case 0: ziuzia(c, s); break;
            case 1: goat(c, s); break;
            case 2: wolf(c, s); break;
            case 3: bear(c, s); break;
            default: kuttsia(c, s); break;
        }
        c.restore();
    }

    /** Ziuzia: the winter lord, white fur coat, long white beard, bare feet, an iron mace. */
    private void ziuzia(Canvas c, double s) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFE8C6A8);
        c.drawOval(-38, -6, -14, 6, p);
        c.drawOval(14, -6, 38, 6, p);
        p.setColor(0xFFF4F7FB);
        path.reset();
        path.moveTo(-28, -150);
        path.lineTo(28, -150);
        path.lineTo(62, -6);
        path.lineTo(-62, -6);
        path.close();
        c.drawPath(path, p);
        p.setColor(0xFFB9C9DA);
        c.drawRect(-62, -18, 62, -6, p);
        p.setColor(0xFFE8C6A8);
        c.drawCircle(0, -178, 30, p);
        p.setColor(Color.WHITE);
        c.drawCircle(0, -196, 30, p);   // the white hair
        p.setColor(0xFFE8C6A8);
        c.drawOval(-24, -192, 24, -160, p);
        p.setColor(0xFF3A2E2A);
        c.drawCircle(-9, -180, 3.5f, p);
        c.drawCircle(9, -180, 3.5f, p);
        p.setColor(Color.WHITE);
        path.reset();
        path.moveTo(-24, -170);
        path.quadTo(0, -60, 24, -170);
        path.close();
        c.drawPath(path, p);   // the beard
        // The mace, raised and lowered in time.
        float lift = (float) Math.sin(s * 3) * 20;
        p.setColor(0xFF5D4037);
        p.setStrokeWidth(8);
        p.setStyle(Paint.Style.STROKE);
        c.drawLine(52, -90, 70, -200 - lift, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF78909C);
        c.drawCircle(72, -212 - lift, 16, p);
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 4 * k;
            c.drawCircle(72 + (float) Math.cos(a) * 18, -212 - lift + (float) Math.sin(a) * 18, 4, p);
        }
    }

    /** The Kaliady goat: dark, with ribbons, horns, a beard and a bell. */
    private void goat(Canvas c, double s) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF2B2B2B);
        float step = (float) Math.sin(s * 6) * 10;
        c.drawRect(-46, -50, -36, 0 + step, p);
        c.drawRect(-20, -50, -10, 0 - step, p);
        c.drawRect(20, -50, 30, 0 + step, p);
        c.drawRect(40, -50, 50, 0 - step, p);
        oval.set(-60, -110, 64, -40);
        c.drawOval(oval, p);
        int[] ribbons = {RED, 0xFFFFD54F, 0xFF43A047, 0xFF1E88E5};
        for (int k = 0; k < ribbons.length; k++) {
            p.setColor(ribbons[k]);
            float x = -40 + k * 26;
            float sway = (float) Math.sin(s * 4 + k) * 8;
            c.drawRect(x, -100, x + 8, -40 + sway, p);
        }
        p.setColor(0xFF2B2B2B);
        c.drawOval(48, -150, 96, -104, p);   // head
        p.setColor(0xFFE0E0E0);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(7);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(60, -146, 46, -182, p);
        c.drawLine(78, -148, 84, -186, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFBDBDBD);
        path.reset();
        path.moveTo(84, -108);
        path.lineTo(96, -108);
        path.lineTo(90, -84);
        path.close();
        c.drawPath(path, p);   // beard
        p.setColor(Color.WHITE);
        c.drawCircle(82, -132, 4, p);
        p.setColor(0xFFFFC107);
        c.drawCircle(58, -98, 7, p);   // the bell
    }

    private void wolf(Canvas c, double s) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF78808A);
        float step = (float) Math.sin(s * 6 + 1) * 10;
        c.drawRect(-44, -46, -34, 0 + step, p);
        c.drawRect(-18, -46, -8, 0 - step, p);
        c.drawRect(18, -46, 28, 0 + step, p);
        c.drawRect(38, -46, 48, 0 - step, p);
        c.drawOval(-58, -100, 62, -36, p);
        path.reset();
        path.moveTo(-56, -76);
        path.quadTo(-100, -90 - (float) Math.sin(s * 5) * 14, -96, -40);
        path.lineTo(-52, -60);
        path.close();
        c.drawPath(path, p);   // tail
        c.drawCircle(64, -110, 30, p);
        path.reset();
        path.moveTo(80, -124);
        path.lineTo(122, -104);
        path.lineTo(80, -94);
        path.close();
        c.drawPath(path, p);   // snout
        path.reset();
        path.moveTo(46, -132);
        path.lineTo(52, -168);
        path.lineTo(66, -136);
        path.moveTo(68, -136);
        path.lineTo(80, -168);
        path.lineTo(86, -128);
        c.drawPath(path, p);   // ears
        p.setColor(0xFFFFD54F);
        c.drawCircle(72, -116, 4, p);
        p.setColor(0xFF263238);
        c.drawCircle(120, -104, 5, p);
    }

    private void bear(Canvas c, double s) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF6D4C41);
        float arm = (float) Math.sin(s * 4) * 24;
        c.drawOval(-30, -30, -6, 0, p);
        c.drawOval(6, -30, 30, 0, p);
        c.drawOval(-52, -160, 52, -20, p);
        c.drawOval(-74, -140 - arm, -40, -90 - arm, p);
        c.drawOval(40, -140 + arm, 74, -90 + arm, p);
        c.drawCircle(0, -184, 40, p);
        c.drawCircle(-30, -216, 14, p);
        c.drawCircle(30, -216, 14, p);
        p.setColor(0xFFA1887F);
        c.drawOval(-24, -120, 24, -50, p);
        c.drawOval(-16, -180, 16, -156, p);
        p.setColor(0xFF1B1B1B);
        c.drawCircle(-14, -192, 5, p);
        c.drawCircle(14, -192, 5, p);
        c.drawCircle(0, -172, 6, p);
    }

    /** Kuttsia: the Kaliady Eve pot of grain and honey, dancing, steam rising, its spoon leaping. */
    private void kuttsia(Canvas c, double s) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFB5652B);
        path.reset();
        path.moveTo(-60, -110);
        path.quadTo(-78, -40, -40, 0);
        path.lineTo(40, 0);
        path.quadTo(78, -40, 60, -110);
        path.close();
        c.drawPath(path, p);
        p.setColor(0xFF8D4A1E);
        c.drawRect(-66, -122, 66, -108, p);
        p.setColor(0xFFF0D9A8);
        c.drawOval(-58, -132, 58, -112, p);   // the grain
        p.setColor(RED);
        for (int k = -2; k <= 2; k++) rhomb(c, k * 22, -60, 7);
        p.setColor(Color.WHITE);
        c.drawCircle(-20, -86, 8, p);
        c.drawCircle(20, -86, 8, p);
        p.setColor(0xFF3A2E2A);
        c.drawCircle(-20, -86, 4, p);
        c.drawCircle(20, -86, 4, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        p.setColor(0xFF3A2E2A);
        oval.set(-20, -86, 20, -60);
        c.drawArc(oval, 20, 140, false, p);
        p.setColor(0x88FFFFFF);
        p.setStrokeWidth(4);
        for (int k = 0; k < 3; k++) {
            float x = -24 + k * 24, rise = (float) ((s * 30 + k * 20) % 60);
            path.reset();
            path.moveTo(x, -136 - rise);
            path.quadTo(x + 10, -152 - rise, x, -168 - rise);
            c.drawPath(path, p);
        }
        float leap = (float) Math.abs(Math.sin(s * 3)) * 40;
        p.setColor(0xFFD7B56D);
        p.setStrokeWidth(7);
        c.drawLine(40, -150 - leap, 70, -200 - leap, p);
        p.setStyle(Paint.Style.FILL);
        c.drawOval(64, -214 - leap, 84, -196 - leap, p);
    }
}
