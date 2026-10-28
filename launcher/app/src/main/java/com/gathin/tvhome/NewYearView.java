package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.Random;

/**
 * Local widget (show_new_year): a festive symbol, a title and the countdown in days and HH:MM:SS, every second.
 * new_year_style picks the symbol: a fir tree, the carollers' star (BY, UA, PL), the straw "pavuk" hung under the
 * ceiling (BY pavuk, PL pajak, LT sodas, LV puzurs, UA pavuk), the Ukrainian didukh sheaf, or falling snow.
 * new_year_target counts to 1 January, 25 December or 7 January; new_year_text picks the wording.
 * One canvas, about 380 x 100 dp; it moves at 10 frames a second only while Home is shown, and redraws once a
 * second with animations off.
 */
final class NewYearView extends View {
    private static final int SPARKLES = 16, FLAKES = 26;
    private final float d;
    private final boolean animate;
    private final String style, text;
    private final MonthDay target;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timer = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float[] sx = new float[FLAKES], sy = new float[FLAKES], phase = new float[FLAKES];
    private final int[] lights = {0xFFFF5A5A, 0xFFFFD54F, 0xFF64B5F6, 0xFFBA68C8, 0xFFFF8A65, 0xFF81C784};
    private Shader treeShade, starGlow;

    NewYearView(Context ctx, boolean animate, String style, String target, String text) {
        super(ctx);
        this.animate = animate;
        this.style = NewYear.style(style);
        this.target = NewYear.target(target);
        this.text = text == null ? "short" : text;
        d = ctx.getResources().getDisplayMetrics().density;
        float sp = ctx.getResources().getDisplayMetrics().scaledDensity;
        title.setColor(0xD9FFFFFF);
        title.setTextSize(16 * sp);
        title.setTypeface(UiFont.get(ctx, R.string.font_medium));
        title.setLetterSpacing(0.06f);
        timer.setColor(Color.WHITE);
        timer.setTextSize(26 * sp);
        timer.setTypeface(Typeface.create(UiFont.get(ctx, R.string.font_medium), Typeface.BOLD));
        timer.setFontFeatureSettings("tnum");   // the digits keep their width every second
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        Random r = new Random(2027);
        for (int i = 0; i < FLAKES; i++) {
            sx[i] = r.nextFloat();
            sy[i] = r.nextFloat();
            phase[i] = r.nextFloat() * 6.283f;
        }
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(Math.round(380 * d), Math.round(100 * d));
    }

    @Override
    protected void onDraw(Canvas c) {
        float t = animate ? SystemClock.uptimeMillis() / 1000f : 0f;
        switch (style) {
            case "star": drawStar(c, t); break;
            case "pavuk": drawPavuk(c, t); break;
            case "didukh": drawDidukh(c, t); break;
            case "snow": drawSnow(c, t); break;
            default: drawTree(c, t); break;
        }
        if (!"snow".equals(style)) drawSparkles(c, t);
        long left = NewYear.secondsLeft(LocalDateTime.now(), target);
        float x = 96 * d;
        if (left == 0) {
            c.drawText(getContext().getString(greeting()), x, 60 * d, timer);
        } else {
            NewYear.Left l = NewYear.split(left);
            c.drawText(getContext().getString(titleText()), x, 36 * d, title);
            String days = getResources().getQuantityString(R.plurals.new_year_days, (int) l.days, (int) l.days);
            c.drawText(days + "  " + l.clock, x, 74 * d, timer);
        }
        if (!isShown()) return;
        if (animate) postInvalidateDelayed(100);   // 10 frames a second: enough to twinkle, little CPU
        else postInvalidateDelayed(1000 - System.currentTimeMillis() % 1000);   // on the second
    }

    private boolean christmas() {
        return target.getMonthValue() != 1 || target.getDayOfMonth() != 1;
    }

    private int titleText() {
        boolean xmas = christmas();
        switch (text) {
            case "long": return xmas ? R.string.new_year_title_long_xmas : R.string.new_year_title_long;
            case "soon": return xmas ? R.string.new_year_title_soon_xmas : R.string.new_year_title_soon;
            default: return xmas ? R.string.new_year_title_xmas : R.string.new_year_until;
        }
    }

    private int greeting() {
        return christmas() ? R.string.new_year_happy_xmas : R.string.new_year_happy;
    }

    // ---- symbols, each in the 0..88 dp box on the left ----------------------------------------------

    private void drawTree(Canvas c, float t) {
        float cx = 42 * d;
        if (treeShade == null) {
            treeShade = new LinearGradient(0, 12 * d, 0, 82 * d, 0xFF43A047, 0xFF1B5E20, Shader.TileMode.CLAMP);
        }
        fill.setShader(treeShade);
        // Three tiers, each wider and lower: {apex, half width, base} in dp.
        float[][] tiers = {{16, 14, 40}, {30, 20, 60}, {46, 27, 82}};
        for (float[] tier : tiers) {
            path.reset();
            path.moveTo(cx, tier[0] * d);
            path.lineTo(cx - tier[1] * d, tier[2] * d);
            path.lineTo(cx + tier[1] * d, tier[2] * d);
            path.close();
            c.drawPath(path, fill);
        }
        fill.setShader(null);
        fill.setColor(0xFF6D4C41);
        c.drawRect(cx - 4 * d, 82 * d, cx + 4 * d, 92 * d, fill);
        fill.setColor(Color.argb(Math.round(255 * pulse(t, 2.4f, 0.75f)), 255, 213, 79));
        star(c, cx, 14 * d, 7 * d, 3 * d, 4, 0);
        float[][] at = {{-5, 30}, {6, 36}, {-11, 50}, {4, 54}, {13, 57}, {-19, 76}, {-5, 72}, {10, 78}, {20, 79}};
        for (int i = 0; i < at.length; i++) {
            float on = animate ? 0.35f + 0.65f * Math.max(0f, (float) Math.sin(t * 3f + i * 1.3f)) : 1f;
            int col = lights[i % lights.length];
            fill.setColor(Color.argb(Math.round(255 * on), Color.red(col), Color.green(col), Color.blue(col)));
            c.drawCircle(cx + at[i][0] * d, at[i][1] * d, 2.6f * d, fill);
        }
    }

    /** The carollers' star on its pole, turning slowly, with ribbons. */
    private void drawStar(Canvas c, float t) {
        float cx = 42 * d, cy = 42 * d;
        line.setStrokeWidth(3 * d);
        line.setColor(0xFF8D6E63);
        c.drawLine(cx, cy + 10 * d, cx, 96 * d, line);
        int[] ribbons = {0xFFE53935, 0xFFFDD835, 0xFF1E88E5};
        line.setStrokeWidth(2 * d);
        for (int i = 0; i < ribbons.length; i++) {
            line.setColor(ribbons[i]);
            float sway = (float) Math.sin(t * 2f + i) * 5 * d;
            path.reset();
            path.moveTo(cx + (i - 1) * 6 * d, cy + 18 * d);
            path.quadTo(cx + (i - 1) * 10 * d + sway, cy + 34 * d, cx + (i - 1) * 8 * d - sway, cy + 50 * d);
            c.drawPath(path, line);
        }
        if (starGlow == null) {
            starGlow = new RadialGradient(cx, cy, 40 * d, 0x66FFD54F, 0x00FFD54F, Shader.TileMode.CLAMP);
        }
        fill.setShader(starGlow);
        c.drawCircle(cx, cy, 40 * d, fill);
        fill.setShader(null);
        fill.setColor(0xFFFFC107);
        star(c, cx, cy, 30 * d, 12 * d, 8, t * 18);
        fill.setColor(0xFFFFF59D);
        star(c, cx, cy, 18 * d, 8 * d, 8, t * 18 + 22.5f);
        fill.setColor(0xFFD32F2F);
        c.drawCircle(cx, cy, 6 * d, fill);
    }

    /** The straw pavuk: an octahedron of straws turning under the ceiling, small ones hanging from its corners. */
    private void drawPavuk(Canvas c, float t) {
        float cx = 42 * d, cy = 46 * d;
        line.setColor(0x99FFFFFF);
        line.setStrokeWidth(1 * d);
        c.drawLine(cx, 0, cx, cy - 26 * d, line);
        line.setColor(0xFFE6C36A);
        line.setStrokeWidth(2 * d);
        octahedron(c, line, cx, cy, 28 * d, t * 0.6f);
        for (int k = 0; k < 4; k++) {
            // The big one's four side corners, where octahedron() puts them.
            double ang = t * 0.6f - Math.PI / 2 * k;
            float px = cx + (float) Math.cos(ang) * 28 * d;
            float py = cy + (float) (Math.sin(ang) * Math.sin(0.35)) * 28 * d;
            line.setColor(0x99FFFFFF);
            line.setStrokeWidth(0.8f * d);
            c.drawLine(px, py, px, py + 10 * d, line);
            line.setColor(0xFFE6C36A);
            line.setStrokeWidth(1.4f * d);
            octahedron(c, line, px, py + 17 * d, 7 * d, -t);
        }
    }

    /** An octahedron's 12 edges, turned by {@code angle} around the vertical and tipped a little towards the eye. */
    static void octahedron(Canvas c, Paint line, float cx, float cy, float r, float angle) {
        float[][] v = {{0, -1, 0}, {0, 1, 0}, {1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 0, -1}};
        float[] x = new float[6], y = new float[6];
        double cos = Math.cos(angle), sin = Math.sin(angle), tilt = 0.35;
        for (int i = 0; i < 6; i++) {
            double rx = v[i][0] * cos + v[i][2] * sin, rz = -v[i][0] * sin + v[i][2] * cos;
            double ry = v[i][1] * Math.cos(tilt) - rz * Math.sin(tilt);
            x[i] = cx + (float) rx * r;
            y[i] = cy + (float) ry * r;
        }
        for (int e = 2; e < 6; e++) {
            int next = e == 5 ? 2 : e + 1;
            c.drawLine(x[0], y[0], x[e], y[e], line);
            c.drawLine(x[1], y[1], x[e], y[e], line);
            c.drawLine(x[e], y[e], x[next], y[next], line);
        }
    }

    /** The didukh: a sheaf of wheat bound with a red band, swaying a little. */
    private void drawDidukh(Canvas c, float t) {
        float baseX = 42 * d, baseY = 94 * d, band = 66 * d;
        int stalks = 9;
        for (int i = 0; i < stalks; i++) {
            float f = (i - (stalks - 1) / 2f) / ((stalks - 1) / 2f);
            float sway = (float) Math.sin(t * 1.2f + i * 0.4f) * 2.5f * d;
            float topX = baseX + f * 30 * d + sway, topY = (20 + Math.abs(f) * 12) * d;
            line.setColor(0xFFD7B56D);
            line.setStrokeWidth(1.6f * d);
            path.reset();
            path.moveTo(baseX + f * 9 * d, baseY);
            path.lineTo(baseX + f * 4 * d, band);
            path.quadTo(baseX + f * 14 * d, (band + topY) / 2, topX, topY);
            c.drawPath(path, line);
            fill.setColor(0xFFE8C677);
            for (int g = 0; g < 4; g++) {
                float gy = topY - g * 4.2f * d;
                c.drawOval(topX - 2.6f * d, gy - 2.4f * d, topX + 2.6f * d, gy + 2.4f * d, fill);
            }
        }
        fill.setColor(0xFFC62828);
        c.drawRoundRect(baseX - 12 * d, band - 3 * d, baseX + 12 * d, band + 4 * d, 3 * d, 3 * d, fill);
    }

    /** Snow over the whole widget, with one big flake turning on the left. */
    private void drawSnow(Canvas c, float t) {
        float w = getWidth(), h = getHeight();
        line.setColor(0xEEFFFFFF);
        line.setStrokeWidth(2.2f * d);
        flake(c, 42 * d, 48 * d, 30 * d, t * 12);
        for (int i = 0; i < FLAKES; i++) {
            float speed = 10 + (i % 5) * 4;
            float y = (sy[i] * h + t * speed * d) % h;
            float x = sx[i] * w + (float) Math.sin(t + phase[i]) * 6 * d;
            if (i % 4 == 0) {
                line.setStrokeWidth(1 * d);
                flake(c, x, y, 4 * d, t * 30 + i);
            } else {
                fill.setColor(0xCCFFFFFF);
                c.drawCircle(x, y, (1 + i % 3 * 0.6f) * d, fill);
            }
        }
    }

    private void flake(Canvas c, float x, float y, float r, float degrees) {
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(degrees + 60 * k);
            c.drawLine(x, y, x + (float) Math.cos(a) * r, y + (float) Math.sin(a) * r, line);
            float mx = x + (float) Math.cos(a) * r * 0.6f, my = y + (float) Math.sin(a) * r * 0.6f;
            for (int s = -1; s <= 1; s += 2) {
                double b = a + s * Math.PI / 4;
                c.drawLine(mx, my, mx + (float) Math.cos(b) * r * 0.3f, my + (float) Math.sin(b) * r * 0.3f, line);
            }
        }
    }

    private void drawSparkles(Canvas c, float t) {
        float w = getWidth(), h = getHeight();
        for (int i = 0; i < SPARKLES; i++) {
            float a = animate ? Math.max(0f, (float) Math.sin(t * (1.4f + i % 4 * 0.35f) + phase[i])) : 0.5f;
            if (a < 0.05f) continue;
            fill.setColor(Color.argb(Math.round(220 * a), 255, 255, 255));
            star(c, sx[i] * w, 4 * d + sy[i] * (h - 8 * d), (1.5f + 2.5f * a) * d, 0.6f * d, 4, 0);
        }
    }

    private float pulse(float t, float speed, float min) {
        return animate ? min + (1 - min) * (0.5f + 0.5f * (float) Math.sin(t * speed)) : 1f;
    }

    /** A star with {@code points} points, turned by {@code degrees}. */
    private void star(Canvas c, float x, float y, float outer, float inner, int points, float degrees) {
        path.reset();
        for (int k = 0; k < points * 2; k++) {
            double ang = Math.PI / points * k - Math.PI / 2 + Math.toRadians(degrees);
            float r = k % 2 == 0 ? outer : inner;
            float px = x + (float) Math.cos(ang) * r, py = y + (float) Math.sin(ang) * r;
            if (k == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        c.drawPath(path, fill);
    }
}
