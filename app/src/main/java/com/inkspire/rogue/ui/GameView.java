package com.inkspire.rogue.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import com.inkspire.rogue.BuildConfig;
import com.inkspire.rogue.ai.TacticalAI;
import com.inkspire.rogue.content.CardLibrary;
import com.inkspire.rogue.content.CharacterLibrary;
import com.inkspire.rogue.content.EnemyLibrary;
import com.inkspire.rogue.content.EventLibrary;
import com.inkspire.rogue.content.PotionLibrary;
import com.inkspire.rogue.content.RelicLibrary;
import com.inkspire.rogue.content.ShopService;
import com.inkspire.rogue.core.Cheats;
import com.inkspire.rogue.core.CombatEngine;
import com.inkspire.rogue.core.CombatState;
import com.inkspire.rogue.core.EnemyState;
import com.inkspire.rogue.core.MapGenerator;
import com.inkspire.rogue.core.MapNode;
import com.inkspire.rogue.core.NodeType;
import com.inkspire.rogue.core.PotionState;
import com.inkspire.rogue.core.RunState;
import com.inkspire.rogue.core.SaveStore;
import com.inkspire.rogue.model.CardDef;
import com.inkspire.rogue.model.CardInstance;
import com.inkspire.rogue.model.CardType;
import com.inkspire.rogue.model.EnemyDef;
import com.inkspire.rogue.model.PotionDef;
import com.inkspire.rogue.model.RelicDef;
import com.inkspire.rogue.model.Status;
import com.inkspire.rogue.model.Target;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** A single custom-drawn, deliberately animation-free surface for the whole game. */
public final class GameView extends View {
    private static final int PAPER = 0xFFF4F0E5;
    private static final int PAPER_2 = 0xFFEAE4D5;
    private static final int PAPER_3 = 0xFFDDD5C3;
    private static final int INK = 0xFF171512;
    private static final int INK_2 = 0xFF4C473D;
    private static final int GRAY = 0xFF655F55;
    private static final int LIGHT = 0xFFB7AF9D;
    private static final int WHITE = 0xFFFBF8EF;

    private enum Screen { MENU, CHARACTER, MAP, COMBAT, REWARD, SHOP, REST, EVENT, TREASURE, GAME_OVER, VICTORY, DECK, RELICS, HELP, SETTINGS, CHEAT }
    private enum Modal { NONE, CARD, POTIONS, CONFIRM }
    private enum DeckMode { VIEW, UPGRADE, REMOVE }
    private enum DetailMode { VIEW, REWARD }

    private static final class Hit {
        final RectF r;        // 精确命中区（绘制出来的那个框）
        final RectF pad;      // 外扩后的触摸区；null 表示不外扩
        final boolean backdrop; // 整屏垫底（点空白处收提示），不参与精确命中
        final Runnable action;
        Hit(RectF r, RectF pad, boolean backdrop, Runnable action) {
            this.r = r; this.pad = pad; this.backdrop = backdrop; this.action = action;
        }
    }

    private final ArrayList<Hit> hits = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Typeface sans = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface sansBold = Typeface.create("sans-serif", Typeface.BOLD);
    private final Typeface serif = Typeface.create("serif", Typeface.NORMAL);
    private final Typeface serifBold = Typeface.create("serif", Typeface.BOLD);
    private final Random visualRng = new Random(42);
    private final Context context;

    private float w, h, u, pad;
    private Screen screen = Screen.MENU;
    private Screen previousScreen = Screen.MENU;
    private Screen helpBack = Screen.MENU;
    private Screen settingsBack = Screen.MENU;
    private Screen cheatBack = Screen.MAP;
    private Screen deckBack = Screen.MAP;
    private Modal modal = Modal.NONE;
    private RunState run;
    private CombatEngine engine;
    private EnemyState targetPreview;
    private boolean saveExists;
    private boolean hintOn;
    private String hintLine = "";
    private String suggestedUid = "";
    private String toast;
    private String toastShown;
    private final Runnable toastClear = () -> {
        if (toast != null) { toast = null; invalidate(); }
    };
    /** 托管：每张牌之间留出的间隔（毫秒），让血条/能量/日志的变化看得见。 */
    private static final int AUTO_STEP_MS = 300;
    /** 战斗结束后定格的时间（毫秒），看清最后一击再进奖励。 */
    private static final int AUTO_VICTORY_MS = 850;
    private boolean autoRunning;
    private int autoPlayed;
    private int autoSteps;
    private final Runnable autoTick = () -> autoStep();
    private int deckPage;
    private int helpPage;
    private int shopTab;
    private int rewardGold;
    private String rewardRelicId;
    private String rewardPotionId;
    private List<CardInstance> rewardChoices = new ArrayList<>();
    private boolean rewardTaken;
    private String rewardTakenName = "";
    private ShopService shop;
    private EventLibrary.EventDef event;
    private String eventResult;
    private long eventSeed;
    private int treasureGold;
    private String treasureRelic;
    private String treasurePotion;
    private CardInstance detailCard;
    private DetailMode detailMode = DetailMode.VIEW;
    private CardInstance targetingCard;
    private DeckMode deckMode = DeckMode.VIEW;
    private String deckTitle = "牌组";
    private int confirmAction;
    private String confirmText = "";
    private long runStartedAt;
    private String lastRunSummary = "";
    private int lastRunScore;
    private boolean restUsed;
    private String restResult = "";
    private String bonusRewardText = "";
    private Throwable fatalError;
    private boolean fatalLogged;

    public GameView(Context context) {
        super(context);
        this.context = context;
        setFocusable(true);
        setClickable(true);
        hintOn = SaveStore.hints(context);
        saveExists = SaveStore.has(context);
        if (BuildConfig.CHEAT) Cheats.reset();
    }

    public void onHostPause() {
        autoRunning = false;
        removeCallbacks(autoTick);
        saveIfSafe();
    }

    @Override
    protected void onDetachedFromWindow() {
        autoRunning = false;
        removeCallbacks(autoTick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int nw, int nh, int ow, int oh) {
        super.onSizeChanged(nw, nh, ow, oh);
        w = nw; h = nh;
        u = Math.min(w / 25f, h / 40f);
        pad = u * 0.65f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        hits.clear();
        if (w <= 0 || h <= 0) return;
        if (fatalError != null) { drawFatal(c, fatalError); return; }
        try {
            c.drawColor(PAPER);
            switch (screen) {
                case MENU: drawMenu(c); break;
                case CHARACTER: drawCharacter(c); break;
                case MAP: drawMap(c); break;
                case COMBAT: drawCombat(c); break;
                case REWARD: drawReward(c); break;
                case SHOP: drawShop(c); break;
                case REST: drawRest(c); break;
                case EVENT: drawEvent(c); break;
                case TREASURE: drawTreasure(c); break;
                case GAME_OVER: drawGameOver(c, false); break;
                case VICTORY: drawGameOver(c, true); break;
                case DECK: drawDeck(c); break;
                case RELICS: drawRelics(c); break;
                case HELP: drawHelp(c); break;
                case SETTINGS: drawSettings(c); break;
                case CHEAT: drawCheat(c); break;
                default: drawMenu(c); break;
            }
            if (modal == Modal.CARD) drawCardModal(c);
            else if (modal == Modal.POTIONS) drawPotionsModal(c);
            else if (modal == Modal.CONFIRM) drawConfirm(c);
            if (toast != null) drawToast(c);
        } catch (Throwable error) {
            fatalError = error;
            drawFatal(c, error);
        }
    }

    private void drawFatal(Canvas c, Throwable error) {
        hits.clear();
        c.drawColor(PAPER);
        if (!fatalLogged) { fatalLogged = true; logCrash(error); }
        text(c, "墨塔 · 绘制异常", pad, pad + px(1.6f), px(1.0f), INK, serifBold, Paint.Align.LEFT);
        String message = error.getClass().getSimpleName() + ": " + (error.getMessage() == null ? "" : error.getMessage());
        wrap(c, message, pad, pad + px(2.2f), w - pad * 2, px(0.65f), INK_2, px(0.95f), 4);
        StringBuilder st = new StringBuilder();
        StackTraceElement[] trace = error.getStackTrace();
        for (int i = 0; i < Math.min(8, trace.length); i++) st.append(trace[i].toString()).append("\n");
        wrap(c, st.toString(), pad, pad + px(6.0f), w - pad * 2, px(0.5f), GRAY, px(0.75f), 10);
        text(c, "已写入 Android/data/com.inkspire.rogue/files/last-crash.txt", pad, h - px(1.2f), px(0.5f), GRAY, sans, Paint.Align.LEFT);
        // A large, harmless redraw target keeps the activity alive for screenshots/logs.
        RectF retry = new RectF(pad, h - px(3.2f), w - pad, h - px(1.9f));
        button(c, retry, "重试绘制", false, true, () -> { fatalError = null; fatalLogged = false; invalidate(); });
    }

    private void logCrash(Throwable error) {
        try {
            java.io.File dir = context.getExternalFilesDir(null);
            if (dir == null) dir = context.getFilesDir();
            java.io.File out = new java.io.File(dir, "last-crash.txt");
            java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(out, false));
            pw.println("Ink Spire view crash: " + new java.util.Date());
            error.printStackTrace(pw);
            pw.close();
        } catch (Throwable ignored) { }
    }

    // =====================================================================
    // drawing primitives
    // =====================================================================
    private float px(float units) { return units * u; }

    private void text(Canvas c, String s, float x, float baseline, float size, int color,
                      Typeface face, Paint.Align align) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTypeface(face);
        paint.setTextAlign(align);
        c.drawText(s, x, baseline, paint);
    }

    private void text(Canvas c, String s, float x, float baseline, float size, int color) {
        text(c, s, x, baseline, size, color, sans, Paint.Align.LEFT);
    }

    private float textWidth(String s, float size, Typeface face) {
        paint.setTypeface(face);
        paint.setTextSize(size);
        return paint.measureText(s);
    }

    private String fit(String s, float size, Typeface face, float maxW) {
        if (s == null) return "";
        if (textWidth(s, size, face) <= maxW) return s;
        String dots = "…";
        float dw = textWidth(dots, size, face);
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (textWidth(s.substring(0, mid), size, face) + dw <= maxW) lo = mid;
            else hi = mid - 1;
        }
        return lo <= 0 ? dots : s.substring(0, lo) + dots;
    }

    private void textCenter(Canvas c, String s, float centerX, float baseline, float size,
                            int color, Typeface face) {
        text(c, s, centerX, baseline, size, color, face, Paint.Align.CENTER);
    }

    private void textRight(Canvas c, String s, float rightX, float baseline, float size,
                           int color, Typeface face) {
        text(c, s, rightX, baseline, size, color, face, Paint.Align.RIGHT);
    }

    /** Draws wrapped Chinese text; returns the y after the last line. */
    private float wrap(Canvas c, String s, float x, float top, float maxW, float size,
                       int color, float lineH, int maxLines) {
        if (s == null || s.length() == 0 || maxLines <= 0) return top;
        paint.setTypeface(sans);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setStyle(Paint.Style.FILL);
        ArrayList<String> lines = new ArrayList<>();
        String[] paras = s.split("\\n");
        for (String para : paras) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < para.length(); i++) {
                char ch = para.charAt(i);
                String test = line.toString() + ch;
                if (paint.measureText(test) > maxW && line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                    if (lines.size() >= maxLines) break;
                }
                line.append(ch);
            }
            if (lines.size() >= maxLines) break;
            if (line.length() > 0) lines.add(line.toString());
        }
        float y = top + size;
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            String line = lines.get(i);
            if (i == maxLines - 1 && lines.size() > maxLines) line = fit(line, size, sans, maxW);
            c.drawText(line, x, y, paint);
            y += lineH;
        }
        return y;
    }

    private void line(Canvas c, float x1, float y1, float x2, float y2, int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setColor(color);
        paint.setStrokeCap(Paint.Cap.BUTT);
        c.drawLine(x1, y1, x2, y2, paint);
    }

    private void border(Canvas c, RectF r, int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setColor(color);
        c.drawRect(r, paint);
    }

    private void fillRound(Canvas c, RectF r, int color, float radius) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawRoundRect(r, radius, radius, paint);
    }

    private void strokeRound(Canvas c, RectF r, int color, float width, float radius) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setColor(color);
        c.drawRoundRect(r, radius, radius, paint);
    }

    private void button(Canvas c, RectF r, String label, boolean primary, boolean enabled, Runnable action) {
        if (primary) {
            fillRound(c, r, enabled ? INK : LIGHT, px(0.18f));
            strokeRound(c, r, enabled ? INK : LIGHT, px(0.07f), px(0.18f));
            textCenter(c, label, r.centerX(), r.centerY() + px(0.32f), px(0.7f),
                    enabled ? PAPER : PAPER_2, sansBold);
        } else {
            fillRound(c, r, PAPER, px(0.18f));
            strokeRound(c, r, enabled ? INK : LIGHT, px(0.09f), px(0.18f));
            textCenter(c, label, r.centerX(), r.centerY() + px(0.32f), px(0.7f),
                    enabled ? INK : GRAY, sansBold);
        }
        if (enabled) hitPad(r, action);
    }

    private void chip(Canvas c, float x, float y, String label, boolean dark) {
        paint.setTypeface(sans);
        paint.setTextSize(px(0.58f));
        float tw = paint.measureText(label);
        RectF r = new RectF(x, y, x + tw + px(0.65f), y + px(1.05f));
        fillRound(c, r, dark ? INK : PAPER_2, px(0.16f));
        strokeRound(c, r, dark ? INK : LIGHT, px(0.06f), px(0.16f));
        textCenter(c, label, r.centerX(), r.centerY() + px(0.24f), px(0.56f),
                dark ? PAPER : INK, sansBold);
    }

    private void hit(RectF r, Runnable action) {
        hits.add(new Hit(new RectF(r), null, false, action));
    }

    /**
     * 给小按钮加大触摸余量：图标和排布一点不动，只是把「判定区」往外扩一圈。
     * 点中别的按钮的精确区时，永远优先那个按钮；只有点在空地时才按「离谁最近」兜底判定，
     * 所以扩出来的区域彼此重叠也不会互相抢。
     */
    /** 点到矩形的距离；在矩形内为 0。用于「扩出来的触摸区」按最近者判定。 */
    private static float distanceTo(RectF r, float x, float y) {
        float dx = x < r.left ? r.left - x : (x > r.right ? x - r.right : 0f);
        float dy = y < r.top ? r.top - y : (y > r.bottom ? y - r.bottom : 0f);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void hitPad(RectF r, Runnable action) {
        float grow = px(0.42f);
        RectF padded = new RectF(Math.max(0f, r.left - grow), Math.max(0f, r.top - grow),
                Math.min(w, r.right + grow), Math.min(h, r.bottom + grow));
        hits.add(new Hit(new RectF(r), padded, false, action));
    }

    private void fullBackdrop(Canvas c) {
        c.drawColor(PAPER);
        hits.add(new Hit(new RectF(0, 0, w, h), null, true, () -> { }));
    }

    private void hairline(Canvas c, float y) {
        line(c, pad, y, w - pad, y, LIGHT, px(0.055f));
    }

    private void header(Canvas c, String title, String rightLabel, Runnable rightAction) {
        float y = pad + px(0.15f);
        text(c, title, pad, y + px(1.05f), px(0.95f), INK, serifBold, Paint.Align.LEFT);
        if (rightLabel != null) {
            RectF r = new RectF(w - pad - px(2.6f), y, w - pad, y + px(1.35f));
            button(c, r, rightLabel, false, true, rightAction);
        }
        hairline(c, y + px(1.55f));
    }

    // =====================================================================
    // menu
    // =====================================================================
    private void drawMenu(Canvas c) {
        saveExists = SaveStore.has(context);
        float top = h * 0.10f;
        textCenter(c, "A ROGUELIKE PRINTED IN INK", w / 2f, top, px(0.55f), GRAY, sans);
        textCenter(c, "墨 塔", w / 2f, top + px(4.1f), px(3.9f), INK, serifBold);
        textCenter(c, "纸 牌 深 渊", w / 2f, top + px(5.2f), px(1.02f), INK_2, serif);
        if (BuildConfig.CHEAT) {
            chip(c, w - pad - px(4.4f), pad, "作弊版 · 已解锁", true);
        }

        drawTower(c, w / 2f, top + px(14.2f), Math.min(w * 0.44f, h * 0.23f));

        float by = h * 0.64f;
        float bh = Math.max(px(2.2f), h * 0.055f);
        float bw = Math.min(w * 0.66f, px(11.5f));
        float bx = (w - bw) / 2f;
        if (saveExists) {
            RectF cont = new RectF(bx, by, bx + bw, by + bh);
            button(c, cont, "继续攀登", true, true, this::continueRun);
            RectF fresh = new RectF(bx, by + bh + px(0.65f), bx + bw, by + 2 * bh + px(0.65f));
            button(c, fresh, "新的攀登", false, true, this::askNewRun);
            by += 2 * bh + px(0.65f);
        } else {
            RectF fresh = new RectF(bx, by, bx + bw, by + bh);
            button(c, fresh, "开始攀登", true, true, this::openCharacterSelect);
            by += bh;
        }
        float half = (bw - px(0.55f)) / 2f;
        RectF help = new RectF(bx, by + px(0.65f), bx + half, by + px(0.65f) + bh * 0.82f);
        RectF set = new RectF(bx + half + px(0.55f), by + px(0.65f), bx + bw, by + px(0.65f) + bh * 0.82f);
        button(c, help, "玩法", false, true, () -> { helpBack = Screen.MENU; screen = Screen.HELP; helpPage = 0; invalidate(); });
        button(c, set, "设置", false, true, () -> { settingsBack = Screen.MENU; screen = Screen.SETTINGS; invalidate(); });

        String foot = "离线 · 无动画 · 32 位兼容 · v1.3";
        textCenter(c, foot, w / 2f, h - pad * 0.8f, px(0.5f), GRAY, sans);
        if (lastRunSummary.length() > 0) {
            textCenter(c, lastRunSummary, w / 2f, h - pad * 0.8f - px(0.95f), px(0.55f), INK_2, serif);
        }
    }

    /** 角色选择：三个角色并排，写明生命、初始遗物与玩法。 */
    private void drawCharacter(Canvas c) {
        float top = pad + px(0.4f);
        text(c, "选择执笔人", pad, top + px(1.5f), px(1.5f), INK, serifBold, Paint.Align.LEFT);
        text(c, "每一支笔，写出来的故事都不一样。", pad, top + px(2.3f), px(0.62f), GRAY, serif, Paint.Align.LEFT);
        RectF back = new RectF(w - pad - px(2.6f), top + px(0.1f), w - pad, top + px(1.45f));
        button(c, back, "返回", false, true, () -> { screen = Screen.MENU; invalidate(); });

        List<CharacterLibrary.CharacterDef> chars = CharacterLibrary.all();
        float listTop = top + px(2.9f);
        float cardH = Math.min(px(10.2f), (h - listTop - px(1.4f)) / Math.max(1, chars.size()) - px(0.55f));
        cardH = Math.max(px(7.4f), cardH);
        for (int i = 0; i < chars.size(); i++) {
            CharacterLibrary.CharacterDef d = chars.get(i);
            RectF r = new RectF(pad, listTop + i * (cardH + px(0.55f)),
                    w - pad, listTop + i * (cardH + px(0.55f)) + cardH);
            fillRound(c, r, PAPER, px(0.2f));
            strokeRound(c, r, INK_2, px(0.08f), px(0.2f));
            // 大大的角色印章
            float sealR = Math.min(r.height() * 0.30f, px(2.3f));
            float sx = r.left + px(0.85f) + sealR;
            float sy = r.top + px(0.85f) + sealR;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(INK);
            c.drawCircle(sx, sy, sealR, paint);
            textCenter(c, d.glyph, sx, sy + sealR * 0.42f, sealR * 1.05f, PAPER, serifBold);
            text(c, fit(d.name, px(1.15f), serifBold, r.width() - px(5.0f)), sx + sealR + px(0.7f),
                    r.top + px(1.35f), px(1.15f), INK, serifBold, Paint.Align.LEFT);
            text(c, fit(d.title, px(0.6f), sans, r.width() - px(5.0f)), sx + sealR + px(0.7f),
                    r.top + px(2.15f), px(0.6f), GRAY, sans, Paint.Align.LEFT);
            wrap(c, "「" + d.blurb + "」", r.left + px(5.4f), r.top + px(3.0f),
                    r.width() - px(6.3f), px(0.62f), INK_2, px(0.95f), 2);
            String line = "生命 " + d.maxHp + "   ·   " + d.playstyle;
            text(c, fit(line, px(0.58f), sansBold, r.width() - px(8.0f)), r.left + px(0.85f),
                    r.bottom - px(1.35f), px(0.58f), INK, sansBold, Paint.Align.LEFT);
            RelicDef sr = RelicLibrary.get(d.starterRelic);
            if (sr != null) {
                text(c, fit("初始遗物 " + sr.name, px(0.55f), sans, r.width() - px(10.0f)),
                        r.left + px(0.85f), r.bottom - px(0.6f), px(0.55f), GRAY, sans, Paint.Align.LEFT);
            }
            RectF pick = new RectF(r.right - px(4.6f), r.bottom - px(2.6f), r.right - px(0.7f), r.bottom - px(0.75f));
            button(c, pick, "开始", true, true, () -> startCharacter(d.id));
        }
        textCenter(c, "新的一局会覆盖当前存档。", w / 2f, h - px(0.75f), px(0.52f), GRAY, sans);
    }

    private void openCharacterSelect() {
        if (run != null) {
            confirmAction = 3;
            confirmText = "开始新的攀登会覆盖当前存档。确定吗？";
            modal = Modal.CONFIRM;
            invalidate();
            return;
        }
        screen = Screen.CHARACTER;
        invalidate();
    }

    private void startCharacter(String characterId) {
        Cheats.reset();
        SaveStore.clear(context);
        run = RunState.newRun(System.currentTimeMillis(), characterId);
        engine = null;
        screen = Screen.MAP;
        modal = Modal.NONE;
        targetingCard = null;
        restUsed = false;
        saveIfSafe();
        saveExists = true;
        CharacterLibrary.CharacterDef d = CharacterLibrary.get(characterId);
        toast = "「" + d.name + "」翻开了第一页。";
        invalidate();
    }

    private void drawTower(Canvas c, float cx, float baseY, float size) {
        // A deliberately static woodcut tower: cards, a spire, and an ink sun.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(px(0.08f));
        paint.setColor(LIGHT);
        c.drawCircle(cx, baseY - size * 0.50f, size * 0.34f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PAPER_2);
        c.drawCircle(cx, baseY - size * 0.50f, size * 0.31f, paint);

        Path tower = new Path();
        tower.moveTo(cx - size * 0.20f, baseY);
        tower.lineTo(cx - size * 0.115f, baseY - size * 0.42f);
        tower.lineTo(cx - size * 0.055f, baseY - size * 0.57f);
        tower.lineTo(cx, baseY - size * 0.82f);
        tower.lineTo(cx + size * 0.055f, baseY - size * 0.57f);
        tower.lineTo(cx + size * 0.115f, baseY - size * 0.42f);
        tower.lineTo(cx + size * 0.20f, baseY);
        tower.close();
        fillRound(c, new RectF(cx - size * 0.30f, baseY - size * 0.02f, cx + size * 0.30f, baseY + size * 0.05f), INK, 0);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INK);
        c.drawPath(tower, paint);
        for (int i = 1; i <= 4; i++) {
            float yy = baseY - size * 0.12f * i;
            line(c, cx - size * 0.18f + i * size * 0.012f, yy,
                    cx + size * 0.18f - i * size * 0.012f, yy, PAPER, px(0.06f));
        }
        // two cards leaning at the foot
        drawLeaningCard(c, cx - size * 0.23f, baseY - size * 0.02f, -0.16f, size * 0.22f);
        drawLeaningCard(c, cx + size * 0.23f, baseY - size * 0.02f, 0.16f, size * 0.22f);
        // a small seal
        RectF seal = new RectF(cx - size * 0.045f, baseY - size * 0.28f, cx + size * 0.045f, baseY - size * 0.19f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(PAPER);
        paint.setStrokeWidth(px(0.05f));
        c.drawRect(seal, paint);
        textCenter(c, "墨", cx, seal.centerY() + px(0.22f), px(0.38f), PAPER, serifBold);
    }

    private void drawLeaningCard(Canvas c, float x, float y, float angle, float s) {
        c.save();
        c.rotate((float) Math.toDegrees(angle), x, y);
        RectF r = new RectF(x - s * 0.45f, y - s * 1.45f, x + s * 0.45f, y);
        fillRound(c, r, WHITE, px(0.12f));
        strokeRound(c, r, INK, px(0.07f), px(0.12f));
        textCenter(c, "墨", r.centerX(), r.centerY() + px(0.24f), px(0.5f), INK, serifBold);
        c.restore();
    }

    // =====================================================================
    // map
    // =====================================================================
    private void drawMap(Canvas c) {
        if (run == null) { screen = Screen.MENU; drawMenu(c); return; }
        float titleY = pad + px(0.1f);
        text(c, MapGenerator.actName(run.act), pad, titleY + px(1.35f), px(1.25f), INK, serifBold, Paint.Align.LEFT);
        text(c, MapGenerator.actSubtitle(run.act), pad, titleY + px(2.25f), px(0.6f), GRAY, serif, Paint.Align.LEFT);
        CharacterLibrary.CharacterDef who = CharacterLibrary.get(run.characterId);
        textRight(c, who.glyph + " " + who.name, w - pad, titleY + px(2.25f), px(0.6f), GRAY, serif);
        textRight(c, "第 " + Math.max(1, run.floor) + " / " + MapGenerator.ROWS + " 层", w - pad,
                titleY + px(1.28f), px(0.72f), INK_2, sansBold);

        float sy = titleY + px(2.75f);
        RectF status = new RectF(pad, sy, w - pad, sy + px(1.85f));
        fillRound(c, status, PAPER_2, px(0.15f));
        strokeRound(c, status, LIGHT, px(0.055f), px(0.15f));
        float hx = status.left + px(0.55f);
        text(c, "生命 " + run.hp + "/" + run.maxHp, hx, status.centerY() + px(0.34f), px(0.65f), INK, sansBold, Paint.Align.LEFT);
        text(c, "金币 " + run.gold, hx + px(4.7f), status.centerY() + px(0.34f), px(0.65f), INK_2, sans, Paint.Align.LEFT);
        text(c, "牌组 " + run.deck.size(), hx + px(8.6f), status.centerY() + px(0.34f), px(0.65f), INK_2, sans, Paint.Align.LEFT);
        textRight(c, "遗物 " + run.relics.size(), status.right - px(0.55f),
                status.centerY() + px(0.34f), px(0.65f), INK_2, sans);

        // compact legend replaces per-node labels, which would collide at 13 rows
        float lx = pad;
        String[] legend = {"战 战斗", "精 精英", "事 事件", "火 营火", "店 商店", "宝 宝箱", "王 首领"};
        float legendY = sy + px(2.85f);
        text(c, "图例", lx, legendY, px(0.52f), GRAY, sans, Paint.Align.LEFT);
        float lxx = lx + px(1.4f);
        for (String item : legend) {
            float tw = textWidth(item, px(0.52f), sans) + px(0.5f);
            if (lxx + tw > w - pad) break;
            text(c, item, lxx, legendY, px(0.52f), GRAY, sans, Paint.Align.LEFT);
            lxx += tw;
        }
        float mapTop = sy + px(4.15f);
        float mapBottom = h - px(4.65f);
        float rowH = (mapBottom - mapTop) / 12f;
        float laneStep = Math.min(w * 0.26f, px(5.6f));
        List<MapNode> available = run.availableNodes();
        MapNode current = run.currentNode();
        // edges
        for (MapNode n : run.map) {
            for (int nid : n.next) {
                MapNode to = run.node(nid);
                if (to == null) continue;
                float x1 = w / 2f + (n.lane - 1) * laneStep;
                float y1 = mapBottom - n.row * rowH;
                float x2 = w / 2f + (to.lane - 1) * laneStep;
                float y2 = mapBottom - to.row * rowH;
                boolean active = current != null && n.id == current.id && contains(available, to.id);
                if (n.visited) line(c, x1, y1, x2, y2, INK_2, px(0.15f));
                else if (active) line(c, x1, y1, x2, y2, INK, px(0.10f));
                else line(c, x1, y1, x2, y2, LIGHT, px(0.05f));
            }
        }
        for (MapNode n : run.map) {
            float x = w / 2f + (n.lane - 1) * laneStep;
            float y = mapBottom - n.row * rowH;
            boolean avail = contains(available, n.id);
            boolean isCurrent = current != null && current.id == n.id;
            float r = n.type == NodeType.BOSS ? px(1.1f) : px(0.82f);
            if (isCurrent) {
                // 当前节点：粗外环 + 实心反白，墨水屏上一眼能找到「我在哪」
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(px(0.2f));
                paint.setColor(INK);
                c.drawCircle(x, y, r + px(0.42f), paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(isCurrent ? INK : (n.visited ? PAPER_3 : (avail ? WHITE : PAPER)));
            c.drawCircle(x, y, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(avail ? px(0.14f) : px(0.055f));
            paint.setColor(avail || isCurrent ? INK : LIGHT);
            c.drawCircle(x, y, r, paint);
            int gc = isCurrent ? PAPER : (avail ? INK : (n.visited ? GRAY : LIGHT));
            textCenter(c, n.type.glyph, x, y + px(0.42f), n.type == NodeType.BOSS ? px(1.15f) : px(0.9f), gc, serifBold);
            if (avail) {
                RectF hr = new RectF(x - r - px(1.0f), y - r - px(1.0f), x + r + px(1.0f), y + r + px(1.0f));
                MapNode target = n;
                hit(hr, () -> enterNode(target));
            }
        }
        if (available.isEmpty()) {
            textCenter(c, "本幕已走完。", w / 2f, mapTop + (mapBottom - mapTop) / 2f, px(0.8f), GRAY, serif);
        }
        // footer
        hairline(c, h - px(3.1f));
        float fy = h - px(2.65f);
        float bw = (w - 2 * pad - px(0.55f) * 3f) / 4f;
        RectF b1 = new RectF(pad, fy, pad + bw, fy + px(1.7f));
        button(c, b1, "牌组", false, true, () -> openDeck(DeckMode.VIEW, Screen.MAP, "牌组"));
        RectF b2 = new RectF(pad + bw + px(0.55f), fy, pad + 2 * bw + px(0.55f), fy + px(1.7f));
        button(c, b2, "遗物", false, true, () -> { screen = Screen.RELICS; deckPage = 0; invalidate(); });
        RectF b3 = new RectF(pad + 2 * (bw + px(0.55f)), fy, pad + 3 * bw + 2 * px(0.55f), fy + px(1.7f));
        button(c, b3, "帮助", false, true, () -> { helpBack = Screen.MAP; screen = Screen.HELP; helpPage = 0; invalidate(); });
        RectF b4 = new RectF(pad + 3 * (bw + px(0.55f)), fy, w - pad, fy + px(1.7f));
        if (BuildConfig.CHEAT) button(c, b4, "作弊", true, true, () -> { cheatBack = Screen.MAP; screen = Screen.CHEAT; invalidate(); });
        else button(c, b4, "设置", false, true, () -> { settingsBack = Screen.MAP; screen = Screen.SETTINGS; invalidate(); });
    }

    private boolean contains(List<MapNode> list, int id) {
        for (MapNode n : list) if (n.id == id) return true;
        return false;
    }

    private void enterNode(MapNode n) {
        if (run == null) return;
        toast = null;
        run.enterNode(n);
        switch (n.type) {
            case BATTLE:
            case ELITE:
            case BOSS:
                startCombat(n.type);
                break;
            case EVENT:
                event = EventLibrary.random(run);
                eventResult = null;
                screen = Screen.EVENT;
                invalidate();
                break;
            case SHOP:
                shop = ShopService.generate(run);
                shopTab = 0;
                screen = Screen.SHOP;
                invalidate();
                break;
            case REST:
                restUsed = false;
                restResult = "";
                screen = Screen.REST;
                invalidate();
                break;
            case TREASURE:
                openTreasure();
                break;
            default:
                screen = Screen.MAP;
                invalidate();
                break;
        }
    }

    private void startCombat(NodeType type) {
        List<EnemyState> foes = new ArrayList<>();
        Random rng = run.rng;
        if (type == NodeType.BATTLE) {
            foes = EnemyLibrary.rollEncounter(run.act, rng, run.floor);
        } else if (type == NodeType.ELITE) {
            foes = EnemyLibrary.rollElite(run.act, rng);
        } else if (type == NodeType.BOSS) {
            EnemyDef def = EnemyLibrary.boss(run.act);
            foes.add(new EnemyState(def, 0, rng));
        }
        if (foes.isEmpty()) {
            toast = "这一层空无一人。";
            screen = Screen.MAP;
            invalidate();
            return;
        }
        CombatState cs = new CombatState(run, foes, new Random(run.seed ^ System.nanoTime()));
        cs.boss = type == NodeType.BOSS;
        cs.elite = type == NodeType.ELITE;
        engine = new CombatEngine(cs);
        engine.start();
        autoRunning = false;
        autoPlayed = 0;
        autoSteps = 0;
        removeCallbacks(autoTick);
        screen = Screen.COMBAT;
        targetingCard = null;
        suggestedUid = "";
        hintLine = "";
        refreshHint();
        afterCombatAction();
        invalidate();
    }

    private void openTreasure() {
        treasureGold = 45 + run.rng.nextInt(50);
        treasureRelic = null;
        treasurePotion = null;
        int roll = run.rng.nextInt(100);
        if (roll < 55) {
            RelicDef r = RelicLibrary.random(run.rng, run.relics);
            if (r != null) { treasureRelic = r.id; run.addRelic(r.id); }
        } else if (roll < 85) {
            PotionDef p = PotionLibrary.random(run.rng);
            if (run.addPotion(p.id)) treasurePotion = p.id;
        }
        run.gainGold(treasureGold);
        screen = Screen.TREASURE;
        invalidate();
    }

    private void drawTreasure(Canvas c) {
        float top = h * 0.17f;
        textCenter(c, "宝 箱", w / 2f, top, px(1.8f), INK, serifBold);
        textCenter(c, "锁扣上积着一层薄灰。", w / 2f, top + px(1.1f), px(0.65f), GRAY, serif);
        float boxW = Math.min(w * 0.68f, px(12f));
        RectF box = new RectF((w - boxW) / 2f, top + px(2.1f), (w + boxW) / 2f, top + px(4.8f));
        fillRound(c, box, PAPER_2, px(0.25f));
        strokeRound(c, box, INK, px(0.1f), px(0.25f));
        textCenter(c, "宝", w / 2f, box.centerY() + px(0.75f), px(2.2f), INK, serifBold);
        float y = box.bottom + px(1.1f);
        textCenter(c, "获得 " + treasureGold + " 金币", w / 2f, y, px(0.9f), INK, sansBold);
        y += px(1.1f);
        if (treasureRelic != null) {
            RelicDef rd = RelicLibrary.get(treasureRelic);
            if (rd != null) {
                textCenter(c, "遗物 · " + rd.name + " " + rd.glyph, w / 2f, y, px(0.9f), INK, serifBold);
                y += px(0.7f);
                wrap(c, rd.description, pad * 2, y, w - pad * 4, px(0.65f), GRAY, px(0.95f), 2);
            }
        }
        if (treasurePotion != null) {
            PotionDef pd = PotionLibrary.get(treasurePotion);
            if (pd != null) textCenter(c, "药水 · " + pd.name + " " + pd.glyph, w / 2f, y + px(0.5f), px(0.9f), INK_2, serifBold);
        }
        RectF btn = new RectF(w / 2f - px(4f), h - px(4.2f), w / 2f + px(4f), h - px(2.5f));
        button(c, btn, "继续", true, true, () -> { finishNode(); });
        textCenter(c, run.hp + " / " + run.maxHp + " 生命", w / 2f, h - px(1.3f), px(0.58f), GRAY, sans);
    }

    private void finishNode() {
        if (run == null) return;
        screen = Screen.MAP;
        toast = null;
        saveIfSafe();
        invalidate();
    }

    // =====================================================================
    // deck / relics / help / settings
    // =====================================================================
    private void openDeck(DeckMode mode, Screen back, String title) {
        if (run == null) return;
        deckMode = mode; deckBack = back; deckTitle = title; deckPage = 0;
        screen = Screen.DECK;
        invalidate();
    }

    private void drawDeck(Canvas c) {
        header(c, deckTitle, "返回", () -> { screen = deckBack; invalidate(); });
        if (run == null) return;

        // 一页看全：按 攻击 → 技能 → 能力 → 诅咒 → 状态，再按费用/名字排序
        List<CardInstance> view = new ArrayList<>(run.deck);
        java.util.Collections.sort(view, (a, b) -> {
            int ta = typeOrder(a.def.type);
            int tb = typeOrder(b.def.type);
            if (ta != tb) return ta - tb;
            if (a.cost() != b.cost()) return a.cost() - b.cost();
            return a.name().compareTo(b.name());
        });

        String note;
        if (deckMode == DeckMode.UPGRADE) note = "点一张未升级的牌 · 共 " + view.size() + " 张";
        else if (deckMode == DeckMode.REMOVE) note = "点一张牌移除 · " + (shop == null ? 75 : shop.removalPrice) + " 金";
        else note = "共 " + view.size() + " 张 · 点牌看详情（一页全览，不翻页）";
        text(c, fit(note, px(0.6f), sans, w - pad * 2), pad, pad + px(2.3f), px(0.6f), GRAY, sans, Paint.Align.LEFT);

        float top = pad + px(2.8f);
        float bottom = h - px(0.9f);
        float avail = Math.max(px(2f), bottom - top);
        if (view.isEmpty()) {
            textCenter(c, "牌组是空的。", w / 2f, top + px(3f), px(0.8f), GRAY, serif);
            return;
        }
        int cols = (int) Math.ceil(view.size() / 24.0);
        if (cols < 3) cols = 3;
        if (cols > 6) cols = 6;
        int rows = Math.max(1, (int) Math.ceil(view.size() / (double) cols));
        float gapX = px(0.26f);
        float gapY = px(0.2f);
        float cellW = (w - pad * 2 - gapX * (cols - 1)) / cols;
        float cellH = (avail - gapY * (rows - 1)) / rows;
        if (cellH > px(3.0f)) cellH = px(3.0f);
        if (cellH < px(0.9f)) cellH = px(0.9f);
        for (int i = 0; i < view.size(); i++) {
            CardInstance card = view.get(i);
            int col = i % cols;
            int row = i / cols;
            float x = pad + col * (cellW + gapX);
            float y = top + row * (cellH + gapY);
            RectF r = new RectF(x, y, x + cellW, y + cellH);
            boolean dim = deckMode == DeckMode.UPGRADE && card.upgraded;
            drawDeckCell(c, r, card, dim);
            if (!dim) hit(r, () -> onDeckCard(card));
        }
    }

    private int typeOrder(CardType t) {
        switch (t) {
            case ATTACK: return 0;
            case SKILL: return 1;
            case POWER: return 2;
            case CURSE: return 3;
            default: return 4;
        }
    }

    /** 单页牌组里的一格：费用 + 名字 + 类型标记（升级的牌名加粗并带 ↑）。 */
    private void drawDeckCell(Canvas c, RectF r, CardInstance card, boolean dim) {
        boolean up = card.upgraded;
        fillRound(c, r, dim ? PAPER_2 : PAPER, px(0.12f));
        strokeRound(c, r, dim ? LIGHT : INK_2, px(0.055f), px(0.12f));
        float d = Math.min(r.height() * 0.56f, r.width() * 0.32f);
        float cx = r.left + px(0.13f) + d / 2f;
        float cy = r.centerY();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(dim ? LIGHT : INK);
        c.drawCircle(cx, cy, d / 2f, paint);
        textCenter(c, card.def.xCost ? "X" : "" + card.cost(), cx, cy + d * 0.26f,
                Math.min(px(0.56f), d * 0.8f), PAPER, sansBold);
        float tx = cx + d / 2f + px(0.14f);
        text(c, fit(card.name(), px(0.55f), sansBold, r.right - tx - px(0.5f)),
                tx, r.centerY() + px(0.19f), px(0.55f), dim ? GRAY : INK, sansBold, Paint.Align.LEFT);
        textRight(c, card.def.type.label.substring(0, 1) + (up ? "↑" : ""),
                r.right - px(0.12f), r.centerY() + px(0.17f), px(0.44f), dim ? LIGHT : GRAY, sans);
    }

    private void onDeckCard(CardInstance card) {
        if (deckMode == DeckMode.UPGRADE) {
            if (card.upgraded) { toast = "这张牌已经升级过了。"; invalidate(); return; }
            card.upgraded = true;
            toast = "「" + card.name() + "」已升级。";
            run.lastNodeSummary = "在营火升级了「" + card.name() + "」";
            restUsed = true;
            restResult = "「" + card.name() + "」被重新誊写，墨色更深了一层。";
            screen = Screen.REST;
            invalidate();
        } else if (deckMode == DeckMode.REMOVE) {
            if (shop == null || shop.removalUsed) { toast = "本次已经移除过一张牌。"; invalidate(); return; }
            if (run.gold < shop.removalPrice) { toast = "金币不够。"; invalidate(); return; }
            run.gold -= shop.removalPrice;
            run.deck.remove(card);
            shop.removalUsed = true;
            toast = "「" + card.name() + "」被从牌组中移除。";
            screen = Screen.SHOP;
            saveIfSafe();
            invalidate();
        } else {
            detailCard = card; detailMode = DetailMode.VIEW; modal = Modal.CARD; invalidate();
        }
    }

    private void drawRelics(Canvas c) {
        header(c, "遗物", "返回", () -> { screen = Screen.MAP; invalidate(); });
        if (run == null) return;
        CharacterLibrary.CharacterDef who = CharacterLibrary.get(run.characterId);
        text(c, who.glyph + " " + who.name + " · 共 " + run.relics.size() + " 件", pad,
                pad + px(2.3f), px(0.66f), GRAY, sans, Paint.Align.LEFT);
        float top = pad + px(2.85f);
        float bottom = h - px(0.9f);
        if (run.relics.isEmpty()) {
            textCenter(c, "还没有遗物。", w / 2f, (top + bottom) / 2f, px(0.8f), GRAY, serif);
            return;
        }
        int cols = run.relics.size() > 24 ? 3 : 2;
        int rows = (int) Math.ceil(run.relics.size() / (double) cols);
        float gap = px(0.35f);
        float cellW = (w - pad * 2 - gap * (cols - 1)) / cols;
        float cellH = (bottom - top - gap * (rows - 1)) / rows;
        if (cellH < px(1.5f)) cellH = px(1.5f);
        if (cellH > px(5.4f)) cellH = px(5.4f);
        for (int i = 0; i < run.relics.size(); i++) {
            RelicDef d = RelicLibrary.get(run.relics.get(i));
            if (d == null) continue;
            int col = i % cols;
            int row = i / cols;
            float x = pad + col * (cellW + gap);
            float y = top + row * (cellH + gap);
            RectF r = new RectF(x, y, x + cellW, y + cellH);
            fillRound(c, r, PAPER, px(0.14f));
            strokeRound(c, r, LIGHT, px(0.055f), px(0.14f));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(INK);
            float gx = r.left + px(0.62f);
            float gy = r.top + px(0.62f);
            c.drawCircle(gx, gy, px(0.44f), paint);
            textCenter(c, d.glyph, gx, gy + px(0.24f), px(0.56f), PAPER, serifBold);
            text(c, fit(d.name, px(0.62f), sansBold, r.width() - px(1.6f)),
                    r.left + px(1.25f), r.top + px(0.82f), px(0.62f), INK, sansBold, Paint.Align.LEFT);
            wrap(c, d.description, r.left + px(0.45f), r.top + px(1.15f), r.width() - px(0.9f),
                    px(0.52f), GRAY, px(0.72f), 3);
        }
    }

    private void drawHelp(Canvas c) {
        header(c, "玩法", "返回", () -> { screen = helpBack; invalidate(); });
        float top = pad + px(2.6f);
        if (helpPage == 0) {
            text(c, "目标", pad, top + px(0.4f), px(0.95f), INK, serifBold, Paint.Align.LEFT);
            wrap(c, "穿过四幕、每幕十二层的墨塔。每层选择一条路线，战斗、事件、商店、精英与营火都会改变你的牌组。击败每一幕的首领，翻到最后一页。",
                    pad, top + px(1.2f), w - pad * 2, px(0.72f), INK_2, px(1.15f), 5);
            text(c, "战斗", pad, top + px(6.6f), px(0.95f), INK, serifBold, Paint.Align.LEFT);
            wrap(c, "每回合获得 3 点能量并抽 5 张牌。攻击牌造成伤害，技能牌提供格挡或效果，能力牌持续生效。点「结束回合」后敌人按头顶的意图行动。格挡抵消伤害，回合结束清零（壁垒除外）。",
                    pad, top + px(7.4f), w - pad * 2, px(0.72f), INK_2, px(1.15f), 7);
            text(c, "三支笔", pad, top + px(14.2f), px(0.95f), INK, serifBold, Paint.Align.LEFT);
            wrap(c, "铁刻：力量与厚盾，越打越重。\n游丝：敏捷与虚弱，用抽牌和格挡把节奏拆细。\n灰烬：拿生命换能量，自伤养出力量，再一次性还回去。",
                    pad, top + px(15.0f), w - pad * 2, px(0.72f), INK_2, px(1.15f), 5);
            text(c, "托管与军师", pad, top + px(21.0f), px(0.95f), INK, serifBold, Paint.Align.LEFT);
            wrap(c, "战斗右上角的「托管」让军师一张一张替你出牌：每一步的血量、能量和日志都看得见，中途点「停」可以随时接手。\n军师会先算完整回合的出牌顺序（先铺易伤与力量，再集中补刀，能杀就杀），该喝药才喝，没有正收益的牌就停手。左下角「提示」只推荐一张，并说明理由。",
                    pad, top + px(21.8f), w - pad * 2, px(0.72f), INK_2, px(1.15f), 6);
        } else if (helpPage == 1) {
            text(c, "状态", pad, top + px(0.4f), px(0.95f), INK, serifBold, Paint.Align.LEFT);
            float y = top + px(1.1f);
            String[][] rows = {
                    {"力量", "每点让每段攻击 +1 伤。"}, {"敏捷", "每点让格挡 +1。"},
                    {"易伤", "受到的攻击伤害 +50%。"}, {"虚弱", "造成的攻击伤害 -25%。"},
                    {"脆弱", "获得的格挡 -25%。"}, {"荆棘", "受到攻击时反伤。"},
                    {"金属化", "回合结束获得格挡。"}, {"再生", "回合开始回复生命。"},
                    {"中毒", "自己回合开始失血，然后减1层。"}, {"枷锁", "力量 -N，其回合末恢复。"},
                    {"恶魔形态", "每回合开始获得力量。"}, {"腐化", "技能牌 0 费并被消耗。"},
                    {"壁垒", "回合结束保留格挡。"}, {"无惧", "消耗牌时获得格挡。"},
                    {"黑暗之拥", "消耗牌时抽牌。"}, {"双发", "下一张攻击打两次。"},
                    {"进化", "抽到状态牌时再抽牌。"}, {"撕裂", "每失去生命就 +力量。"},
                    {"火焰屏障", "受攻击反伤，逐层递减。"}, {"残暴", "每回合失 1 血抽 1 张。"}
            };
            for (int i = 0; i < rows.length; i++) {
                float col = i % 2;
                float row = i / 2;
                float x = pad + col * (w - pad * 2) / 2f;
                float yy = y + row * px(2.45f);
                text(c, rows[i][0], x, yy + px(0.62f), px(0.72f), INK, sansBold, Paint.Align.LEFT);
                wrap(c, rows[i][1], x, yy + px(0.74f), (w - pad * 2) / 2f - px(0.7f), px(0.55f), GRAY, px(0.76f), 2);
            }
        } else {
            text(c, "关键词", pad, top + px(0.4f), px(0.95f), INK, serifBold, Paint.Align.LEFT);
            wrap(c, "消耗：打出后离开本场战斗。\n虚无：回合结束时若还在手上，直接消耗。\n固有：起手必在手牌。\n保留：回合结束不弃掉。\nX费：消耗你全部能量，X 就是消耗掉的点数。\n凝神：本回合不能再抽牌。",
                    pad, top + px(1.2f), w - pad * 2, px(0.72f), INK_2, px(1.2f), 8);
            text(c, "给第一次攀登的人", pad, top + px(9.4f), px(0.95f), INK, serifBold, Paint.Align.LEFT);
            wrap(c, "· 先删掉多余的打击，再考虑加牌。\n· 一费抽牌和格挡比想象中更值钱。\n· 看到精英前，留一点生命给意外。\n· 营火既可以休息，也可以升级关键牌。\n· 药水留着过首领，别在走廊里喝光。\n· 托管是参考，不是答案。",
                    pad, top + px(10.2f), w - pad * 2, px(0.72f), INK_2, px(1.15f), 7);
            text(c, "关于", pad, top + px(16.6f), px(0.95f), INK, serifBold, Paint.Align.LEFT);
            wrap(c, "《墨塔》v1.3 · 离线单机 · 纯 Java 自绘 · 无动画 · 面向墨水屏与 32 位设备。卡牌机制借鉴《杀戮尖塔》，全部文本与绘制为原创。",
                    pad, top + px(17.4f), w - pad * 2, px(0.66f), GRAY, px(1.05f), 4);
        }
        float by = h - px(2.7f);
        if (helpPage > 0) {
            button(c, new RectF(pad, by, pad + px(3.3f), by + px(1.7f)), "上一页", false, true,
                    () -> { helpPage--; invalidate(); });
        }
        if (helpPage < 2) {
            button(c, new RectF(w - pad - px(3.3f), by, w - pad, by + px(1.7f)), "下一页", true, true,
                    () -> { helpPage++; invalidate(); });
        } else {
            button(c, new RectF(w - pad - px(3.3f), by, w - pad, by + px(1.7f)), "完成", true, true,
                    () -> { screen = helpBack; invalidate(); });
        }
        textCenter(c, (helpPage + 1) + " / 3", w / 2f, by + px(1.15f), px(0.58f), GRAY, sans);
    }

    private void drawSettings(Canvas c) {
        header(c, "设置", "返回", () -> { screen = settingsBack; invalidate(); });
        float top = pad + px(3.0f);
        text(c, "智能提示", pad, top, px(0.78f), INK, sansBold, Paint.Align.LEFT);
        wrap(c, "在战斗中推荐下一张牌，并在牌上标记「荐」。", pad, top + px(0.35f), w - pad * 2 - px(5.5f), px(0.6f), GRAY, px(0.9f), 2);
        RectF toggle = new RectF(w - pad - px(3.4f), top - px(0.65f), w - pad, top + px(0.45f));
        button(c, toggle, hintOn ? "已开启" : "已关闭", hintOn, true, () -> {
            hintOn = !hintOn;
            SaveStore.setHints(context, hintOn);
            if (hintOn) refreshHint(); else { suggestedUid = ""; hintLine = ""; }
            invalidate();
        });
        hairline(c, top + px(2.0f));
        text(c, "存档", pad, top + px(3.2f), px(0.78f), INK, sansBold, Paint.Align.LEFT);
        wrap(c, "游戏在安全节点自动保存。战斗中退出会回到上一层安全节点。", pad, top + px(3.55f), w - pad * 2, px(0.6f), GRAY, px(0.9f), 2);
        RectF clear = new RectF(pad, top + px(4.75f), pad + px(5.0f), top + px(6.3f));
        button(c, clear, "清除存档", false, SaveStore.has(context), () -> {
            confirmAction = 1; confirmText = "清除当前存档？此操作不可撤销。"; modal = Modal.CONFIRM; invalidate();
        });
        hairline(c, top + px(7.2f));
        text(c, "墨水屏", pad, top + px(8.3f), px(0.78f), INK, sansBold, Paint.Align.LEFT);
        wrap(c, "本作没有动画与音效。建议在系统中开启快速刷新；如出现残影，手动全刷一次即可。屏幕常亮已开启。",
                pad, top + px(8.65f), w - pad * 2, px(0.6f), GRAY, px(0.95f), 3);
        text(c, "关于", pad, top + px(11.2f), px(0.78f), INK, sansBold, Paint.Align.LEFT);
        wrap(c, "墨塔 v1.3 · 离线 · 32 位兼容\n卡牌机制致敬《杀戮尖塔》，全部文本与绘制为原创。\n构建版本：" + (BuildConfig.CHEAT ? "作弊版" : "标准版"),
                pad, top + px(11.55f), w - pad * 2, px(0.62f), GRAY, px(0.98f), 4);
        if (BuildConfig.CHEAT) {
            RectF cheat = new RectF(w - pad - px(4.4f), h - px(4.8f), w - pad, h - px(3.1f));
            button(c, cheat, "打开作弊", true, true, () -> { cheatBack = Screen.SETTINGS; screen = Screen.CHEAT; invalidate(); });
        }
    }

    private void drawRelicButton(Canvas c, RectF r, RelicDef d, Runnable action) {
        fillRound(c, r, PAPER_2, px(0.16f));
        strokeRound(c, r, LIGHT, px(0.06f), px(0.16f));
        textCenter(c, d.glyph, r.left + px(0.75f), r.centerY() + px(0.38f), px(0.9f), INK, serifBold);
        text(c, fit(d.name, px(0.62f), sansBold, r.width() - px(1.7f)), r.left + px(1.45f), r.centerY() + px(0.27f), px(0.62f), INK_2, sansBold, Paint.Align.LEFT);
        hitPad(r, action);
    }

    // =====================================================================
    // combat
    // =====================================================================
    private void drawCombat(Canvas c) {
        if (engine == null || screen != Screen.COMBAT) return;
        CombatState s = engine.c;
        float headerY = pad;
        text(c, "第 " + Math.max(1, run.floor) + " 层 · " + (s.boss ? "首领" : s.elite ? "精英" : "战斗")
                        + " · 回合 " + s.turn,
                pad, headerY + px(1.15f), px(0.9f), INK, serifBold, Paint.Align.LEFT);
        RectF deckBtn = new RectF(w - pad - px(5.3f), headerY, w - pad - px(2.5f), headerY + px(1.35f));
        button(c, deckBtn, "牌组", false, true, () -> openDeck(DeckMode.VIEW, Screen.COMBAT, "牌组"));
        RectF autoBtn = new RectF(w - pad - px(8.8f), headerY, w - pad - px(5.75f), headerY + px(1.35f));
        button(c, autoBtn, autoRunning ? "停" : "托管", autoRunning,
                engine != null && !engine.c.over && (autoRunning || engine.c.playerTurn),
                this::autoplayTurn);
        if (BuildConfig.CHEAT) {
            RectF cheatBtn = new RectF(w - pad - px(2.0f), headerY, w - pad, headerY + px(1.35f));
            button(c, cheatBtn, "作", true, true, () -> { cheatBack = Screen.COMBAT; screen = Screen.CHEAT; invalidate(); });
        }
        hairline(c, headerY + px(1.55f));

        float enemyTop = headerY + px(2.6f);
        float enemyH = Math.min(px(8.7f), h * 0.315f);
        drawEnemies(c, enemyTop, enemyH);

        float py = enemyTop + enemyH + px(0.4f);
        float ph = px(3.4f);
        drawPlayerPanel(c, py, ph);

        float logTop = py + ph + px(0.22f);
        float logH = px(1.72f);
        drawCombatLog(c, logTop, logH);

        float handTop = logTop + logH + px(0.2f);
        float footerTop = h - px(2.85f);
        drawHand(c, handTop, footerTop);
        drawCombatFooter(c, footerTop);

        if (targetingCard != null) {
            RectF banner = new RectF(pad, headerY + px(1.62f), w - pad, headerY + px(2.48f));
            fillRound(c, banner, INK, px(0.12f));
            textCenter(c, "选择目标 · 点选敌人", banner.centerX(), banner.centerY() + px(0.27f), px(0.62f), PAPER, sansBold);
        }
    }

    private void drawEnemies(Canvas c, float top, float height) {
        List<EnemyState> live = engine.c.living();
        if (live.isEmpty()) return;
        int n = Math.min(3, live.size());
        float gap = px(0.5f);
        float totalW = w - pad * 2;
        float ew = n == 1 ? Math.min(totalW * 0.64f, px(15.0f)) : (totalW - gap * (n - 1)) / n;
        float startX = (w - (ew * n + gap * (n - 1))) / 2f;
        for (int i = 0; i < n; i++) {
            EnemyState e = live.get(i);
            RectF r = new RectF(startX + i * (ew + gap), top, startX + i * (ew + gap) + ew, top + height);
            drawEnemyPanel(c, r, e, targetingCard != null);
        }
    }

    private void drawEnemyPanel(Canvas c, RectF r, EnemyState e, boolean targetable) {
        fillRound(c, r, targetable ? WHITE : PAPER_2, px(0.2f));
        strokeRound(c, r, targetable ? INK : LIGHT, targetable ? px(0.14f) : px(0.07f), px(0.2f));
        float cx = r.centerX();
        float glyphR = Math.min(Math.min(r.width() * 0.22f, r.height() * 0.16f), px(1.9f));
        float gy = r.top + glyphR + px(0.45f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(px(0.07f));
        paint.setColor(INK_2);
        c.drawCircle(cx, gy, glyphR, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INK);
        c.drawCircle(cx, gy, glyphR - px(0.06f), paint);
        textCenter(c, e.def.glyph, cx, gy + glyphR * 0.45f, glyphR * 1.05f, PAPER, serifBold);
        float nameY = gy + glyphR + px(0.9f);
        textCenter(c, fit(e.def.name, px(0.78f), sansBold, r.width() - px(0.5f)),
                cx, nameY, px(0.78f), INK, sansBold);
        float barTop = nameY + px(0.5f);
        float barW = r.width() - px(1.2f);
        float barH = px(0.42f);
        RectF bar = new RectF(r.left + px(0.6f), barTop, r.left + px(0.6f) + barW, barTop + barH);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PAPER_3);
        c.drawRect(bar, paint);
        if (e.maxHp > 0) {
            RectF fill = new RectF(bar.left, bar.top, bar.left + barW * Math.max(0f, e.hp / (float) e.maxHp), bar.bottom);
            paint.setColor(INK);
            c.drawRect(fill, paint);
        }
        strokeRound(c, new RectF(bar), LIGHT, px(0.04f), 0);
        textCenter(c, e.hp + " / " + e.maxHp, cx, barTop + barH + px(0.7f), px(0.62f), INK_2, sansBold);
        float infoY = barTop + barH + px(1.25f);
        String block = e.block > 0 ? "盾 " + e.block : "";
        String status = block;
        if (e.statuses.size() > 0) {
            StringBuilder sb = new StringBuilder(status);
            int count = 0;
            for (Map.Entry<Status, Integer> en : e.statuses.entrySet()) {
                if (count >= 2) { sb.append(" …"); break; }
                if (sb.length() > 0) sb.append("  ");
                sb.append(en.getKey().label).append(en.getValue());
                count++;
            }
            status = sb.toString();
        }
        if (status.length() > 0) {
            textCenter(c, fit(status, px(0.56f), sans, r.width() - px(0.3f)), cx, infoY, px(0.56f), INK_2, sans);
        }
        float intentTop = r.bottom - px(2.15f);
        line(c, r.left + px(0.4f), intentTop, r.right - px(0.4f), intentTop, LIGHT, px(0.05f));
        textCenter(c, "意图", cx, intentTop + px(0.58f), px(0.5f), GRAY, sans);
        String it = engine.intentText(e);
        textCenter(c, fit(it, px(0.67f), sansBold, r.width() - px(0.4f)), cx, intentTop + px(1.35f), px(0.67f),
                e.intent != null && e.intent.phase == 2 ? INK : INK_2, sansBold);
        if (targetable && targetingCard != null) {
            hit(r, () -> playTargeted(e));
        } else {
            hit(r, () -> toast = e.def.name + "：" + (e.def.lore == null ? "" : e.def.lore));
        }
    }

    private void drawPlayerPanel(Canvas c, float top, float height) {
        RectF r = new RectF(pad, top, w - pad, top + height);
        fillRound(c, r, PAPER_2, px(0.18f));
        strokeRound(c, r, LIGHT, px(0.06f), px(0.18f));
        float hpTop = r.top + px(0.45f);
        float hpW = r.width() * 0.47f;
        text(c, "生命", r.left + px(0.55f), hpTop + px(0.85f), px(0.64f), INK, sansBold, Paint.Align.LEFT);
        textRight(c, engine.c.hp + " / " + engine.c.maxHp, r.left + hpW, hpTop + px(0.85f), px(0.68f), INK, sansBold);
        RectF bar = new RectF(r.left + px(0.55f), hpTop + px(1.05f), r.left + hpW, hpTop + px(1.43f));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PAPER_3);
        c.drawRect(bar, paint);
        RectF fill = new RectF(bar.left, bar.top, bar.left + bar.width() * Math.max(0f, engine.c.hp / (float) engine.c.maxHp), bar.bottom);
        paint.setColor(INK);
        c.drawRect(fill, paint);
        float rx = r.left + r.width() * 0.55f;
        text(c, "能量 " + engine.c.energy + " / " + engine.c.maxEnergy, rx, hpTop + px(0.85f), px(0.7f), INK, sansBold, Paint.Align.LEFT);
        text(c, "格挡 " + engine.c.block, rx + px(5.5f), hpTop + px(0.85f), px(0.7f), INK_2, sans, Paint.Align.LEFT);
        int expected = Math.max(0, engine.c.incomingDamage() - engine.c.block);
        textRight(c, "预计受 " + expected, r.right - px(0.55f), hpTop + px(0.85f), px(0.62f),
                expected > 0 ? INK : GRAY, sansBold);
        // status chips row
        float cx = r.left + px(0.55f);
        float cy = r.top + px(1.95f);
        int count = 0;
        int total = 0;
        for (Map.Entry<Status, Integer> en : engine.c.playerStatuses.entrySet()) {
            if (en.getValue() != 0) total++;
        }
        for (Map.Entry<Status, Integer> en : engine.c.playerStatuses.entrySet()) {
            if (en.getValue() == 0) continue;
            String label = en.getKey().label + " " + en.getValue();
            float tw = textWidth(label, px(0.62f), sansBold) + px(0.7f);
            if (cx + tw > r.right - px(0.6f)) {
                int rest = total - count;
                if (rest > 0) {
                    text(c, "+" + rest, cx, cy + px(0.82f), px(0.62f), GRAY, sansBold, Paint.Align.LEFT);
                }
                break;
            }
            RectF chip = new RectF(cx, cy, cx + tw, cy + px(1.08f));
            fillRound(c, chip, en.getKey().turnsDown ? PAPER_3 : WHITE, px(0.15f));
            strokeRound(c, chip, LIGHT, px(0.05f), px(0.15f));
            textCenter(c, label, chip.centerX(), chip.centerY() + px(0.25f), px(0.62f), INK_2, sansBold);
            cx += tw + px(0.4f);
            count++;
        }
        if (count == 0) text(c, "无状态", r.left + px(0.55f), cy + px(0.85f), px(0.58f), GRAY, sans, Paint.Align.LEFT);
        // potion beads（最多画 4 颗，多出来的用 +N 表示）
        int potionCount = engine.c.run.potions.size();
        int beads = Math.min(potionCount, 4);
        for (int i = 0; i < beads; i++) {
            PotionDef d = PotionLibrary.get(engine.c.run.potions.get(i).id);
            if (d == null) continue;
            float bx = r.right - px(0.8f) - i * px(1.35f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(px(0.06f));
            paint.setColor(INK_2);
            c.drawCircle(bx, r.bottom - px(0.85f), px(0.5f), paint);
            textCenter(c, d.glyph, bx, r.bottom - px(0.63f), px(0.5f), INK, serifBold);
            RectF bead = new RectF(bx - px(0.7f), r.bottom - px(1.55f), bx + px(0.7f), r.bottom - px(0.15f));
            hitPad(bead, () -> { if (!autoRunning) { modal = Modal.POTIONS; invalidate(); } });
        }
        if (potionCount > beads) {
            textRight(c, "+" + (potionCount - beads), r.right - px(0.55f) - beads * px(1.35f),
                    r.bottom - px(0.7f), px(0.5f), GRAY, sansBold);
        }
    }

    private void drawCombatLog(Canvas c, float top, float height) {
        List<String> log = engine.c.log;
        float y = top + px(0.35f);
        if (!hintOn || hintLine.length() == 0) {
            if (log.size() > 0) text(c, fit("· " + log.get(log.size() - 1), px(0.57f), sans, w - pad * 2), pad, y + px(0.8f), px(0.57f), INK_2, sans, Paint.Align.LEFT);
            if (log.size() > 1) text(c, fit("· " + log.get(log.size() - 2), px(0.57f), sans, w - pad * 2), pad, y + px(1.4f), px(0.54f), GRAY, sans, Paint.Align.LEFT);
        } else {
            text(c, fit("军师：" + hintLine, px(0.58f), sansBold, w - pad * 2), pad, y + px(0.82f), px(0.58f), INK, sansBold, Paint.Align.LEFT);
            if (log.size() > 0) text(c, fit("· " + log.get(log.size() - 1), px(0.53f), sans, w - pad * 2), pad, y + px(1.38f), px(0.53f), GRAY, sans, Paint.Align.LEFT);
        }
    }

    /**
     * 手牌单页显示：两列铺满，最多 10 张（手牌上限）一次全看见，没有翻页。
     * 行高按实际张数自适应并留一点余量，保证 40u 高的最小值屏幕上 5 行也放得下。
     */
    private void drawHand(Canvas c, float top, float footerTop) {
        CombatState s = engine.c;
        int n = s.hand.size();
        float headerH = px(1.0f);
        text(c, "手牌 " + n + " / " + CombatState.HAND_LIMIT + " · 点牌出牌", pad, top + px(0.65f),
                px(0.62f), INK, sansBold, Paint.Align.LEFT);
        float gridTop = top + headerH;
        if (n == 0) {
            textCenter(c, "手牌已空，结束回合", w / 2f, gridTop + px(2.2f), px(0.7f), GRAY, serif);
            return;
        }
        float gap = px(0.28f);
        float available = footerTop - gridTop - px(0.25f);
        // 行高固定按「装满 5 行（手牌上限 10 张）」来算：牌数变化时格子不会忽大忽小。
        float rowH = (available - gap * 4f) / 5f;
        rowH = Math.max(px(1.7f), Math.min(px(5.6f), rowH));
        float gapX = px(0.4f);
        float colW = (w - pad * 2 - gapX) / 2f;
        for (int i = 0; i < n; i++) {
            int col = i % 2, row = i / 2;
            RectF r = new RectF(pad + col * (colW + gapX), gridTop + row * (rowH + gap),
                    pad + col * (colW + gapX) + colW, gridTop + row * (rowH + gap) + rowH);
            CardInstance card = s.hand.get(i);
            boolean rec = card.uid == uidOf(suggestedUid);
            drawHandRow(c, r, card, engine.canPlay(card), rec);
        }
    }

    private long uidOf(String text) {
        try { return Long.parseLong(text); } catch (Exception e) { return -1; }
    }

    private void drawHandRow(Canvas c, RectF r, CardInstance card, boolean playable, boolean recommended) {
        boolean selected = targetingCard == card;
        fillRound(c, r, selected || recommended ? WHITE : PAPER, px(0.15f));
        strokeRound(c, r, selected || recommended ? INK : (playable ? INK_2 : LIGHT),
                selected ? px(0.16f) : (recommended ? px(0.12f) : px(0.06f)), px(0.15f));
        float costD = Math.min(r.height() * 0.46f, px(1.55f));
        float cx = r.left + px(0.35f) + costD / 2f;
        float cy = r.top + px(0.35f) + costD / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(playable ? INK : LIGHT);
        c.drawCircle(cx, cy, costD / 2f, paint);
        textCenter(c, card.def.xCost ? "X" : "" + engine.currentCost(card), cx, cy + px(0.28f), px(0.72f), PAPER, sansBold);
        float tx = cx + costD / 2f + px(0.35f);
        text(c, fit(card.name(), px(0.7f), sansBold, r.right - tx - px(0.4f)), tx, r.top + px(0.8f),
                px(0.7f), playable ? INK : GRAY, sansBold, Paint.Align.LEFT);
        text(c, card.def.type.label + (card.upgraded ? " · 升级" : ""), tx, r.top + px(1.35f),
                px(0.56f), playable ? GRAY : GRAY, sans, Paint.Align.LEFT);
        float descTop = r.top + px(1.5f);
        // 行高富余时多给一行说明，避免格子高、字只有两行显得空。
        int descLines = (int) Math.max(2f, Math.min(4f, (r.height() - px(1.55f)) / px(0.8f)));
        wrap(c, card.description(), tx, descTop, r.right - tx - px(0.4f), px(0.56f),
                playable ? INK_2 : GRAY, px(0.8f), descLines);
        if (recommended) {
            RectF seal = new RectF(r.right - px(1.25f), r.top + px(0.18f), r.right - px(0.25f), r.top + px(1.05f));
            fillRound(c, seal, INK, px(0.1f));
            textCenter(c, "荐", seal.centerX(), seal.centerY() + px(0.22f), px(0.52f), PAPER, serifBold);
        }
        // 点牌即出牌，不再弹出大卡片详情。需要选目标时只进入一次目标选择。
        hit(r, () -> onHandCard(card));
    }

    private void onHandCard(CardInstance card) {
        if (engine == null || card == null || modal != Modal.NONE) return;
        if (autoRunning) {
            toast = "托管进行中，点「停」可以接手。";
            invalidate();
            return;
        }
        if (!engine.c.playerTurn || engine.c.over) {
            toast = "现在不是你的回合。";
            invalidate();
            return;
        }
        if (!engine.canPlay(card)) {
            if (card.def.type == CardType.STATUS || card.def.type == CardType.CURSE) {
                toast = "「" + card.name() + "」无法打出。";
            } else if (!Cheats.infiniteEnergy && engine.currentCost(card) > engine.c.energy) {
                toast = "费用不足：需要 " + engine.currentCost(card) + "，当前 " + engine.c.energy + "。";
            } else {
                toast = "「" + card.name() + "」现在不能打出。";
            }
            invalidate();
            return;
        }
        targetingCard = null;
        if (card.def.target == Target.ENEMY && engine.c.living().size() > 1) {
            targetingCard = card;
            toast = "点选敌人，打出「" + card.name() + "」。";
            invalidate();
            return;
        }
        if (engine.c.living().isEmpty()) {
            toast = "没有可攻击的目标。";
            invalidate();
            return;
        }
        engine.play(card, engine.c.firstLiving());
        afterCombatAction();
    }

    private void drawCombatFooter(Canvas c, float top) {
        line(c, pad, top - px(0.2f), w - pad, top - px(0.2f), LIGHT, px(0.05f));
        text(c, "抽 " + engine.c.drawPile.size(), pad, top + px(0.9f), px(0.56f), GRAY, sans, Paint.Align.LEFT);
        text(c, "弃 " + engine.c.discardPile.size(), pad + px(3.3f), top + px(0.9f), px(0.56f), GRAY, sans, Paint.Align.LEFT);
        text(c, "耗 " + engine.c.exhaustPile.size(), pad + px(6.6f), top + px(0.9f), px(0.56f), GRAY, sans, Paint.Align.LEFT);
        float buttonY = top + px(0.15f);
        float bh = px(1.9f);
        float endW = px(6.6f);
        RectF end = new RectF(w - pad - endW, buttonY, w - pad, buttonY + bh);
        button(c, end, "结束回合", true, engine.c.playerTurn && !engine.c.over && !autoRunning,
                () -> { targetingCard = null; engine.endTurn(); afterCombatAction(); });
        float smallW = px(3.3f);
        RectF potion = new RectF(end.left - px(0.35f) - smallW, buttonY, end.left - px(0.35f), buttonY + bh);
        button(c, potion, "药水 " + engine.c.run.potions.size(), false, !autoRunning,
                () -> { modal = Modal.POTIONS; invalidate(); });
        RectF hint = new RectF(potion.left - px(0.35f) - smallW, buttonY, potion.left - px(0.35f), buttonY + bh);
        if (targetingCard != null) {
            button(c, hint, "取消", false, true, () -> { targetingCard = null; invalidate(); });
        } else {
            button(c, hint, "提示", false, hintOn && !autoRunning, () -> {
                refreshHint();
                if (hintLine.length() > 0) toast = hintLine;
                invalidate();
            });
        }
    }

    private void saveIfSafe() {
        if (run == null || run.dead || run.won) return;
        boolean safe = screen == Screen.MAP || screen == Screen.RELICS
                || (screen == Screen.DECK && deckBack == Screen.MAP)
                || (screen == Screen.HELP && helpBack == Screen.MAP)
                || (screen == Screen.SETTINGS && settingsBack == Screen.MAP);
        if (safe) {
            SaveStore.save(context, run);
            saveExists = true;
        }
    }

    private void playTargeted(EnemyState e) {
        if (targetingCard == null || engine == null) return;
        CardInstance card = targetingCard;
        targetingCard = null;
        engine.play(card, e);
        afterCombatAction();
    }

    private void afterCombatAction() {
        if (engine == null) { invalidate(); return; }
        CombatState s = engine.c;
        if (s.over) {
            if (s.victory) beginReward();
            else gameOver();
            return;
        }
        refreshHint();
        invalidate();
    }

    /**
     * 托管：军师一张一张地打，每张之间留一小段时间，血条、能量、日志的变化都看得见；
     * 再点一次「停」可以随时接手。没有值得出的牌时才结束回合。
     */
    private void autoplayTurn() {
        if (engine == null || engine.c.over) return;
        if (autoRunning) {
            stopAuto("托管已停手，交给你。");
            return;
        }
        if (!engine.c.playerTurn) {
            toast = "等敌人行动完再托管。";
            invalidate();
            return;
        }
        autoRunning = true;
        autoPlayed = 0;
        autoSteps = 0;
        targetingCard = null;
        toast = "托管中：逐张出牌，点「停」随时接手。";
        removeCallbacks(autoTick);
        postDelayed(autoTick, AUTO_STEP_MS);
        invalidate();
    }

    private void stopAuto(String message) {
        autoRunning = false;
        removeCallbacks(autoTick);
        if (message != null) toast = message;
        invalidate();
    }

    /** 托管的一步：喝药 / 出最多一张牌，然后安排下一步。 */
    private void autoStep() {
        if (!autoRunning) return;
        if (engine == null) { stopAuto(null); return; }
        CombatState s = engine.c;
        if (s.over || !s.playerTurn) { stopAuto(null); return; }
        if (autoSteps++ > 40) { autoEndTurn(); return; }

        int potion = TacticalAI.bestPotionIndex(s);
        if (potion >= 0 && potion < s.run.potions.size()) {
            PotionDef d = PotionLibrary.get(s.run.potions.get(potion).id);
            engine.usePotion(potion);
            toast = "托管：喝下" + (d == null ? "药水" : "「" + d.name + "」") + "。";
            if (s.over) { finishAuto(); return; }
            invalidate();
            postDelayed(autoTick, AUTO_STEP_MS);
            return;
        }

        TacticalAI.Suggestion sug = null;
        try { sug = TacticalAI.suggest(s); } catch (Throwable ignored) { }
        boolean worthwhile = sug != null && sug.card != null
                && (sug.score > 0 || sug.card.def.xCost);
        if (!worthwhile) { autoEndTurn(); return; }
        String label = sug.card.name();
        engine.play(sug.card, sug.target);
        autoPlayed++;
        toast = "托管：" + label + (sug.target != null && s.living().size() > 1 ? " → " + sug.target.def.name : "");
        if (s.over) { finishAuto(); return; }
        invalidate();
        postDelayed(autoTick, AUTO_STEP_MS);
    }

    /** 托管收尾：结束回合（敌人行动同步结算）。 */
    private void autoEndTurn() {
        CombatState s = engine.c;
        if (!s.over && s.playerTurn) engine.endTurn();
        autoRunning = false;
        removeCallbacks(autoTick);
        if (s.over) { finishAuto(); return; }
        toast = autoPlayed == 0 ? "托管：没有值得打的牌，回合已结束。"
                : "托管：打出 " + autoPlayed + " 张牌，回合结束。";
        afterCombatAction();
    }

    /** 战斗刚结束：先在战斗界面定格一会儿，看清最后一击，再进奖励或结算。 */
    private void finishAuto() {
        autoRunning = false;
        removeCallbacks(autoTick);
        if (engine == null) { invalidate(); return; }
        final CombatState snap = engine.c;
        toast = snap.victory ? "战斗胜利。" : "墨迹干涸了……";
        invalidate();
        postDelayed(() -> {
            if (engine == null || engine.c != snap) return;
            afterCombatAction();
        }, AUTO_VICTORY_MS);
    }

    private void refreshHint() {
        suggestedUid = "";
        hintLine = "";
        if (!hintOn || engine == null || engine.c.over) return;
        try {
            TacticalAI.Suggestion s = TacticalAI.suggest(engine.c);
            if (s != null && s.card != null) {
                suggestedUid = Long.toString(s.card.uid);
                hintLine = s.card.name() + "：" + (s.reason == null ? "" : s.reason);
            }
        } catch (Throwable ignored) { }
    }

    // =====================================================================
    // reward
    // =====================================================================
    private void beginReward() {
        if (engine == null || run == null) return;
        CombatState s = engine.c;
        // 硬性检查：只要还有敌人站着，就绝不进奖励界面（把状态退回可继续操作）。
        // 作弊版的「一键胜利」是有意为之，不拦。
        if (!Cheats.autoWin && !s.living().isEmpty()) {
            s.over = false;
            s.victory = false;
            s.playerTurn = true;
            autoRunning = false;
            removeCallbacks(autoTick);
            toast = "结算已撤销：敌人仍在场。";
            invalidate();
            return;
        }
        run.hp = Math.max(1, s.hp);
        run.maxHp = s.maxHp;
        if (s.boss) run.elitesKilled++;
        rewardGold = s.goldReward;
        run.gainGold(rewardGold);
        rewardRelicId = null;
        rewardPotionId = null;
        bonusRewardText = "";
        if (s.elite || s.boss) {
            RelicDef relic = RelicLibrary.random(run.rng, run.relics);
            if (relic != null) {
                run.addRelic(relic.id);
                rewardRelicId = relic.id;
            }
        }
        int potionChance = s.boss ? 100 : s.elite ? 65 : 38;
        if (run.rng.nextInt(100) < potionChance) {
            PotionDef p = PotionLibrary.random(run.rng);
            if (run.addPotion(p.id)) rewardPotionId = p.id;
        }
        int extra = 0;
        for (String rid : run.relics) {
            RelicDef rd = RelicLibrary.get(rid);
            if (rd != null) extra += rd.eliteCardBonus;
        }
        int count = 3 + extra;
        if (extra > 0) bonusRewardText = "遗物效果：额外展示 " + extra + " 张牌。";
        rewardChoices.clear();
        for (CardDef def : CardLibrary.rewardChoices(run.rng, count, run.characterId)) {
            rewardChoices.add(new CardInstance(def));
        }
        rewardTaken = false;
        rewardTakenName = "";
        run.score = run.floor * 15 + run.kills * 6 + run.maxHp / 2 + run.deck.size() * 2;
        screen = Screen.REWARD;
        modal = Modal.NONE;
        invalidate();
    }

    private void takeRewardCard(int index) {
        if (rewardTaken || index < 0 || index >= rewardChoices.size()) return;
        CardInstance src = rewardChoices.get(index);
        boolean upgrade = autoUpgrade(src.def);
        CardInstance add = new CardInstance(src.def, upgrade);
        run.deck.add(add);
        rewardTaken = true;
        rewardTakenName = add.name();
        toast = "获得「" + add.name() + "」" + (upgrade ? "（已自动升级）" : "") + "。";
        SaveStore.save(context, run);
        saveExists = true;
        modal = Modal.NONE;
        invalidate();
    }

    private boolean autoUpgrade(CardDef def) {
        if (run == null) return false;
        if (run.hasRelic("molten_egg") && def.type == CardType.ATTACK) return true;
        if (run.hasRelic("toxic_egg") && def.type == CardType.SKILL) return true;
        if (run.hasRelic("frozen_egg") && def.type == CardType.POWER) return true;
        return false;
    }

    /** 事件选项标价：只认标题里的「N 金币」，奖励文案不会被当成成本。 */
    private static int goldCostOf(String label) {
        if (label == null) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*金币").matcher(label);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) { return 0; }
        }
        return 0;
    }

    private void drawReward(Canvas c) {
        if (run == null) return;
        float y = pad;
        text(c, engine != null && engine.c.boss ? "首领倒下" : "战斗胜利", pad, y + px(1.5f), px(1.5f), INK, serifBold, Paint.Align.LEFT);
        textRight(c, "第 " + Math.max(1, run.floor) + " 层", w - pad, y + px(1.45f), px(0.68f), GRAY, sans);
        y += px(2.2f);
        StringBuilder gift = new StringBuilder("+" + rewardGold + " 金币");
        if (rewardRelicId != null) {
            RelicDef rd = RelicLibrary.get(rewardRelicId);
            if (rd != null) gift.append("   ·   遗物「").append(rd.name).append("」").append(rd.glyph);
        }
        if (rewardPotionId != null) {
            PotionDef pd = PotionLibrary.get(rewardPotionId);
            if (pd != null) gift.append("   ·   药水「").append(pd.name).append("」");
        }
        y = wrap(c, gift.toString(), pad, y, w - pad * 2, px(0.7f), INK_2, px(1.0f), 2) + px(0.55f);
        if (bonusRewardText.length() > 0) {
            text(c, bonusRewardText, pad, y, px(0.58f), GRAY, serif, Paint.Align.LEFT);
            y += px(0.9f);
        }
        text(c, rewardTaken ? "已选择：" + rewardTakenName : "选择一张牌加入牌组（也可以跳过）", pad, y, px(0.65f), INK, sansBold, Paint.Align.LEFT);
        y += px(0.75f);
        float rowH = Math.min(px(4.3f), (h - y - px(3.9f)) / Math.max(1, rewardChoices.size()) - px(0.4f));
        rowH = Math.max(px(2.5f), rowH);
        for (int i = 0; i < rewardChoices.size(); i++) {
            CardInstance card = rewardChoices.get(i);
            RectF r = new RectF(pad, y + i * (rowH + px(0.4f)), w - pad, y + i * (rowH + px(0.4f)) + rowH);
            boolean enabled = !rewardTaken;
            String badge = rewardTaken && rewardTakenName.equals(card.name()) ? "已取" : "查看";
            drawCardRow(c, r, card, false, badge,
                    enabled, () -> { detailCard = card; detailMode = DetailMode.REWARD; modal = Modal.CARD; invalidate(); });
            // Draw card rows above are also clickable through the detail modal only; add a direct take strip.
            RectF take = new RectF(w - pad - px(3.2f), r.top + px(0.45f), w - pad, r.bottom - px(0.45f));
            if (!rewardTaken) {
                final int idx = i;
                fillRound(c, take, INK, px(0.14f));
                textCenter(c, "拿取", take.centerX(), take.centerY() + px(0.27f), px(0.62f), PAPER, sansBold);
                hit(take, () -> takeRewardCard(idx));
            }
        }
        float by = h - px(2.9f);
        RectF skip = new RectF(pad, by, pad + px(4.2f), by + px(1.9f));
        if (!rewardTaken) button(c, skip, "跳过", false, true, () -> { rewardTaken = true; rewardTakenName = "跳过"; toast = "已跳过卡牌奖励。"; invalidate(); });
        RectF cont = new RectF(w - pad - px(6.2f), by, w - pad, by + px(1.9f));
        button(c, cont, engine != null && engine.c.boss ? "继续深入" : "回到地图", true, true, this::continueAfterReward);
    }

    private void continueAfterReward() {
        if (run == null) return;
        boolean boss = engine != null && engine.c.boss;
        if (boss) {
            if (run.act >= MapGenerator.ACTS - 1) { finishVictory(); return; }
            run.nextAct();
            engine = null;
            screen = Screen.MAP;
            // 幕间喘息：击败首领后回复三成生命，让长跑不至于卡在血线上
            int breathe = (int) Math.ceil(run.maxHp * 0.30);
            int before = run.hp;
            run.heal(breathe);
            SaveStore.save(context, run);
            saveExists = true;
            toast = "踏入" + MapGenerator.actName(run.act) + "，回复 " + (run.hp - before) + " 生命。";
            invalidate();
        } else {
            engine = null;
            screen = Screen.MAP;
            SaveStore.save(context, run);
            saveExists = true;
            invalidate();
        }
    }

    // =====================================================================
    // shop
    // =====================================================================
    private void drawShop(Canvas c) {
        if (run == null || shop == null) return;
        float y = pad;
        text(c, "墨 贩", pad, y + px(1.45f), px(1.4f), INK, serifBold, Paint.Align.LEFT);
        text(c, "有些东西比生命更便宜。", pad, y + px(2.15f), px(0.6f), GRAY, serif, Paint.Align.LEFT);
        textRight(c, "金币 " + run.gold, w - pad, y + px(1.45f), px(0.8f), INK, sansBold);
        y += px(2.75f);
        String[] tabs = {"卡牌", "遗物", "药水", "服务"};
        float tw = (w - pad * 2 - px(0.4f) * 3) / 4f;
        for (int i = 0; i < tabs.length; i++) {
            RectF r = new RectF(pad + i * (tw + px(0.4f)), y, pad + i * (tw + px(0.4f)) + tw, y + px(1.65f));
            final int idx = i;
            button(c, r, tabs[i], shopTab == i, true, () -> { shopTab = idx; invalidate(); });
        }
        y += px(2.2f);
        float bottom = h - px(3.0f);
        if (shopTab == 0) {
            float rowH = Math.min(px(3.1f), (bottom - y - px(0.35f) * 4) / 5f);
            for (int i = 0; i < shop.cards.size(); i++) {
                ShopService.CardOffer offer = shop.cards.get(i);
                RectF r = new RectF(pad, y + i * (rowH + px(0.35f)), w - pad, y + i * (rowH + px(0.35f)) + rowH);
                drawShopCard(c, r, offer);
            }
        } else if (shopTab == 1) {
            float rowH = px(4.1f);
            for (int i = 0; i < shop.relics.size(); i++) {
                ShopService.RelicOffer offer = shop.relics.get(i);
                RectF r = new RectF(pad, y + i * (rowH + px(0.45f)), w - pad, y + i * (rowH + px(0.45f)) + rowH);
                fillRound(c, r, PAPER_2, px(0.16f));
                strokeRound(c, r, LIGHT, px(0.06f), px(0.16f));
                text(c, offer.relic.glyph, pad + px(0.75f), r.top + px(1.45f), px(1.25f), INK, serifBold, Paint.Align.LEFT);
                text(c, offer.relic.name, pad + px(2.05f), r.top + px(1.05f), px(0.78f), INK, sansBold, Paint.Align.LEFT);
                wrap(c, offer.relic.description, pad + px(2.05f), r.top + px(1.25f), r.width() - px(6.6f), px(0.6f), GRAY, px(0.9f), 2);
                RectF buy = new RectF(w - pad - px(3.7f), r.top + px(0.65f), w - pad - px(0.45f), r.bottom - px(0.65f));
                if (offer.sold) { fillRound(c, buy, LIGHT, px(0.15f)); textCenter(c, "已售", buy.centerX(), buy.centerY() + px(0.25f), px(0.58f), PAPER, sansBold); }
                else {
                    button(c, buy, offer.price + " 金", true, run.gold >= offer.price, () -> buyShopRelic(offer));
                }
            }
        } else if (shopTab == 2) {
            float rowH = px(3.6f);
            for (int i = 0; i < shop.potions.size(); i++) {
                ShopService.PotionOffer offer = shop.potions.get(i);
                RectF r = new RectF(pad, y + i * (rowH + px(0.45f)), w - pad, y + i * (rowH + px(0.45f)) + rowH);
                fillRound(c, r, PAPER_2, px(0.16f));
                strokeRound(c, r, LIGHT, px(0.06f), px(0.16f));
                text(c, offer.potion.glyph, pad + px(0.75f), r.top + px(1.25f), px(1.15f), INK, serifBold, Paint.Align.LEFT);
                text(c, offer.potion.name, pad + px(2.05f), r.top + px(0.95f), px(0.75f), INK, sansBold, Paint.Align.LEFT);
                wrap(c, offer.potion.description, pad + px(2.05f), r.top + px(1.15f), r.width() - px(6.4f), px(0.6f), GRAY, px(0.9f), 2);
                RectF buy = new RectF(w - pad - px(3.7f), r.top + px(0.55f), w - pad - px(0.45f), r.bottom - px(0.55f));
                if (offer.sold) { fillRound(c, buy, LIGHT, px(0.15f)); textCenter(c, "已售", buy.centerX(), buy.centerY() + px(0.25f), px(0.58f), PAPER, sansBold); }
                else button(c, buy, offer.price + " 金", true, run.gold >= offer.price, () -> buyShopPotion(offer));
            }
        } else {
            text(c, "撕页服务", pad, y + px(0.9f), px(0.85f), INK, serifBold, Paint.Align.LEFT);
            wrap(c, "从牌组中永久移除一张牌。每间商店只提供一次。", pad, y + px(1.3f), w - pad * 2, px(0.68f), GRAY, px(1.0f), 2);
            RectF rm = new RectF(pad, y + px(2.6f), w - pad, y + px(4.5f));
            boolean can = !shop.removalUsed && run.gold >= shop.removalPrice;
            button(c, rm, shop.removalUsed ? "本次已使用" : "选择一张牌移除 · " + shop.removalPrice + " 金", false, can, () -> {
                if (!can) return;
                openDeck(DeckMode.REMOVE, Screen.SHOP, "选择要移除的牌");
            });
        }
        RectF leave = new RectF(pad, h - px(2.75f), w - pad, h - px(1.05f));
        button(c, leave, "离开商店", false, true, () -> { screen = Screen.MAP; saveIfSafe(); invalidate(); });
    }

    private void drawShopCard(Canvas c, RectF r, ShopService.CardOffer offer) {
        CardDef d = offer.card;
        fillRound(c, r, PAPER, px(0.15f));
        strokeRound(c, r, offer.sold ? LIGHT : INK_2, px(0.06f), px(0.15f));
        float costD = px(1.35f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(offer.sold ? LIGHT : INK);
        c.drawCircle(r.left + px(0.55f) + costD / 2f, r.top + px(0.55f) + costD / 2f, costD / 2f, paint);
        textCenter(c, d.xCost ? "X" : "" + d.cost, r.left + px(0.55f) + costD / 2f,
                r.top + px(0.55f) + costD / 2f + px(0.25f), px(0.6f), PAPER, sansBold);
        text(c, fit(d.name, px(0.72f), sansBold, r.width() - px(8.0f)), r.left + px(2.2f), r.top + px(0.85f),
                px(0.72f), offer.sold ? GRAY : INK, sansBold, Paint.Align.LEFT);
        text(c, d.type.label + " · " + d.rarity.label, r.left + px(2.2f), r.top + px(1.4f),
                px(0.5f), GRAY, sans, Paint.Align.LEFT);
        wrap(c, d.description, r.left + px(2.2f), r.top + px(1.5f), r.width() - px(8.2f), px(0.56f),
                offer.sold ? LIGHT : INK_2, px(0.78f), 2);
        RectF buy = new RectF(w - pad - px(4.3f), r.top + px(0.5f), w - pad - px(0.4f), r.bottom - px(0.5f));
        if (offer.sold) {
            fillRound(c, buy, LIGHT, px(0.14f));
            textCenter(c, "已售", buy.centerX(), buy.centerY() + px(0.25f), px(0.58f), PAPER, sansBold);
        } else {
            button(c, buy, offer.price + " 金", true, run.gold >= offer.price, () -> buyShopCard(offer));
        }
    }

    private void buyShopCard(ShopService.CardOffer offer) {
        if (run == null || offer.sold || run.gold < offer.price) return;
        run.gold -= offer.price;
        offer.sold = true;
        CardInstance c = new CardInstance(offer.card, autoUpgrade(offer.card));
        run.deck.add(c);
        toast = "买下「" + c.name() + "」。";
        invalidate();
    }

    private void buyShopRelic(ShopService.RelicOffer offer) {
        if (run == null || offer.sold || run.gold < offer.price) return;
        run.gold -= offer.price;
        offer.sold = true;
        run.addRelic(offer.relic.id);
        toast = "买下遗物「" + offer.relic.name + "」。";
        invalidate();
    }

    private void buyShopPotion(ShopService.PotionOffer offer) {
        if (run == null || offer.sold || run.gold < offer.price) return;
        if (!run.addPotion(offer.potion.id)) { toast = "药水袋已满。"; invalidate(); return; }
        run.gold -= offer.price;
        offer.sold = true;
        toast = "买下药水「" + offer.potion.name + "」。";
        invalidate();
    }

    // =====================================================================
    // rest
    // =====================================================================
    private void drawRest(Canvas c) {
        if (run == null) return;
        float y = h * 0.12f;
        textCenter(c, restUsed ? "余 烬" : "营 火", w / 2f, y, px(1.8f), INK, serifBold);
        textCenter(c, "在下一段路之前，让墨迹干一干。", w / 2f, y + px(1.05f), px(0.65f), GRAY, serif);
        drawCampfire(c, w / 2f, y + px(5.6f), Math.min(w * 0.20f, px(5.2f)), restUsed);
        if (restUsed) {
            wrap(c, restResult, pad * 1.5f, y + px(8.6f), w - pad * 3f, px(0.78f), INK_2, px(1.15f), 4);
            RectF back = new RectF(w / 2f - px(4.3f), h - px(4.2f), w / 2f + px(4.3f), h - px(2.5f));
            button(c, back, "继续攀登", true, true, this::finishNode);
        } else {
            float bh = px(2.4f);
            float bw = Math.min(w * 0.66f, px(12f));
            RectF rest = new RectF((w - bw) / 2f, h * 0.58f, (w + bw) / 2f, h * 0.58f + bh);
            int heal = restHealAmount();
            button(c, rest, "休息 · 回复 " + heal + " 生命", true, run.hp < run.maxHp, () -> {
                int hh = restHealAmount();
                run.hp = Math.min(run.maxHp, run.hp + hh);
                restUsed = true;
                restResult = "你在火边合眼片刻。回复 " + hh + " 生命，现在是 " + run.hp + " / " + run.maxHp + "。";
                invalidate();
            });
            RectF smith = new RectF((w - bw) / 2f, h * 0.58f + bh + px(0.7f), (w + bw) / 2f, h * 0.58f + bh * 2 + px(0.7f));
            button(c, smith, "锻造 · 升级一张牌", false, hasUpgradable(), () -> {
                if (!hasUpgradable()) { toast = "没有可升级的牌。"; invalidate(); return; }
                openDeck(DeckMode.UPGRADE, Screen.REST, "选择要升级的牌");
            });
            textCenter(c, "只能选择一项。", w / 2f, h * 0.58f + bh * 2 + px(1.55f), px(0.58f), GRAY, sans);
            RectF leave = new RectF(w / 2f - px(3.4f), h - px(3.9f), w / 2f + px(3.4f), h - px(2.2f));
            button(c, leave, "不休息，直接离开", false, true, this::finishNode);
        }
        int bonus = restHealAmount() - (int) Math.ceil(run.maxHp * 0.40f);
        if (bonus > 0) {
            textCenter(c, "遗物让休息额外回复 " + bonus + " 生命。", w / 2f, h - px(1.2f), px(0.55f), GRAY, serif);
        }
    }

    private int restHealAmount() {
        int bonus = 0;
        for (String id : run.relics) {
            RelicDef d = RelicLibrary.get(id);
            if (d != null) bonus += d.restHeal;
        }
        return (int) Math.ceil(run.maxHp * 0.40f) + bonus;
    }

    private boolean hasUpgradable() {
        if (run == null) return false;
        for (CardInstance c : run.deck) if (!c.upgraded) return true;
        return false;
    }

    /**
     * 营火图标：石圈 + 交叉柴薪 + 三层墨焰（外焰实、内焰挖空、心焰再实），
     * 顶部几点火星。休息过后只剩余烬与两缕烟。全部静态，无动画。
     */
    private void drawCampfire(Canvas c, float cx, float cy, float size, boolean embers) {
        float baseY = cy + size * 0.40f;

        // 地面
        line(c, cx - size * 0.66f, baseY + px(0.05f), cx + size * 0.66f, baseY + px(0.05f), LIGHT, px(0.06f));

        // 三块围石
        float[] sx = {-0.44f, 0f, 0.44f};
        float[] sw = {0.30f, 0.36f, 0.28f};
        float[] sh = {0.15f, 0.19f, 0.14f};
        for (int i = 0; i < 3; i++) {
            RectF st = new RectF(cx + size * sx[i] - size * sw[i] / 2f, baseY - size * sh[i],
                    cx + size * sx[i] + size * sw[i] / 2f, baseY);
            fillRound(c, st, PAPER_2, size * 0.05f);
            strokeRound(c, st, INK_2, px(0.055f), size * 0.05f);
        }

        // 交叉柴薪
        line(c, cx - size * 0.34f, baseY - size * 0.01f, cx + size * 0.28f, baseY - size * 0.24f, INK, px(0.095f));
        line(c, cx + size * 0.34f, baseY - size * 0.01f, cx - size * 0.28f, baseY - size * 0.24f, INK, px(0.095f));
        line(c, cx - size * 0.14f, baseY - size * 0.22f, cx + size * 0.16f, baseY - size * 0.30f, INK_2, px(0.06f));

        float fireBottom = baseY - size * 0.16f;
        if (embers) {
            // 余烬：一低矮的炭块堆，加两缕缓烟
            fillRound(c, new RectF(cx - size * 0.17f, fireBottom - size * 0.10f,
                    cx + size * 0.17f, fireBottom + size * 0.02f), INK_2, size * 0.05f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(px(0.05f));
            paint.setColor(GRAY);
            Path smoke = new Path();
            smoke.moveTo(cx - size * 0.03f, fireBottom - size * 0.14f);
            smoke.cubicTo(cx - size * 0.14f, fireBottom - size * 0.40f,
                    cx + size * 0.12f, fireBottom - size * 0.56f,
                    cx + size * 0.02f, fireBottom - size * 0.82f);
            c.drawPath(smoke, paint);
            Path smoke2 = new Path();
            smoke2.moveTo(cx + size * 0.09f, fireBottom - size * 0.14f);
            smoke2.cubicTo(cx + size * 0.22f, fireBottom - size * 0.36f,
                    cx + size * 0.02f, fireBottom - size * 0.52f,
                    cx + size * 0.12f, fireBottom - size * 0.70f);
            c.drawPath(smoke2, paint);
            return;
        }

        // 外焰 → 挖空 → 心焰
        flameShape(c, cx, fireBottom, size * 0.92f, INK);
        flameShape(c, cx, fireBottom - size * 0.02f, size * 0.62f, PAPER);
        flameShape(c, cx, fireBottom - size * 0.02f, size * 0.34f, INK);

        // 火星
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INK_2);
        c.drawCircle(cx - size * 0.31f, fireBottom - size * 0.62f, px(0.06f), paint);
        c.drawCircle(cx + size * 0.25f, fireBottom - size * 0.76f, px(0.05f), paint);
        c.drawCircle(cx + size * 0.04f, fireBottom - size * 0.88f, px(0.04f), paint);
    }

    /** 一片静态墨焰：宽度随高度收，底部收在 bottom。 */
    private void flameShape(Canvas c, float cx, float bottom, float height, int color) {
        if (height <= 0) return;
        float w = height * 0.31f;
        Path p = new Path();
        p.moveTo(cx, bottom - height);
        p.cubicTo(cx + w, bottom - height * 0.66f, cx + w * 0.70f, bottom - height * 0.30f, cx, bottom);
        p.cubicTo(cx - w * 0.70f, bottom - height * 0.30f, cx - w, bottom - height * 0.66f, cx, bottom - height);
        p.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawPath(p, paint);
    }

    // =====================================================================
    // event
    // =====================================================================
    private void drawEvent(Canvas c) {
        if (event == null || run == null) return;
        float y = pad;
        text(c, "事 件", pad, y + px(1.2f), px(0.65f), GRAY, sans, Paint.Align.LEFT);
        textCenter(c, event.title, w / 2f, y + px(2.9f), px(2.0f), INK, serifBold);
        textCenter(c, event.glyph, w / 2f, y + px(4.4f), px(1.2f), INK_2, serif);
        y += px(4.8f);
        y = wrap(c, event.body, pad * 1.3f, y, w - pad * 2.6f, px(0.78f), INK_2, px(1.25f), 8) + px(0.6f);
        if (eventResult != null) {
            RectF box = new RectF(pad, y + px(0.2f), w - pad, y + px(2.8f));
            fillRound(c, box, PAPER_2, px(0.16f));
            strokeRound(c, box, LIGHT, px(0.06f), px(0.16f));
            wrap(c, eventResult, pad + px(0.6f), y + px(0.55f), w - pad * 2 - px(1.2f), px(0.7f), INK, px(1.05f), 3);
            RectF cont = new RectF(w / 2f - px(4.2f), h - px(4.1f), w / 2f + px(4.2f), h - px(2.4f));
            button(c, cont, "继续", true, true, this::finishNode);
        } else {
            float by = Math.max(y + px(0.4f), h * 0.56f);
            float bh = px(2.4f);
            for (int i = 0; i < event.choices.size(); i++) {
                EventLibrary.Choice ch = event.choices.get(i);
                int cost = goldCostOf(ch.label);
                boolean afford = cost <= 0 || run.gold >= cost;
                RectF r = new RectF(pad, by + i * (bh + px(0.45f)), w - pad, by + i * (bh + px(0.45f)) + bh);
                fillRound(c, r, afford ? PAPER : PAPER_2, px(0.16f));
                strokeRound(c, r, afford ? INK_2 : LIGHT, px(0.07f), px(0.16f));
                text(c, fit(ch.label, px(0.72f), sansBold, w - pad * 2 - px(1.2f)), r.left + px(0.6f), r.top + px(0.95f), px(0.72f), afford ? INK : GRAY, sansBold, Paint.Align.LEFT);
                text(c, fit(ch.hint, px(0.6f), sans, w - pad * 2 - px(1.2f)), r.left + px(0.6f), r.top + px(1.72f), px(0.6f), afford ? GRAY : LIGHT, sans, Paint.Align.LEFT);
                if (cost > 0) {
                    textRight(c, run.gold >= cost ? "当前 " + run.gold : "不足 " + run.gold + "/" + cost,
                            r.right - px(0.6f), r.bottom - px(0.45f), px(0.55f),
                            run.gold >= cost ? GRAY : INK_2, sansBold);
                }
                final int idx = i;
                if (afford) {
                    hit(r, () -> {
                        eventResult = EventLibrary.resolve(run, event, idx);
                        if (eventResult == null || eventResult.length() == 0) eventResult = "什么也没有发生。";
                        invalidate();
                    });
                } else {
                    hit(r, () -> toast = "金币不足：需要 " + cost + "，当前 " + run.gold + "。");
                }
            }
        }
    }

    // =====================================================================
    // game over / victory
    // =====================================================================
    private void gameOver() {
        if (run == null) return;
        autoRunning = false;
        removeCallbacks(autoTick);
        run.dead = true;
        run.score = run.floor * 15 + run.kills * 6 + run.maxHp / 2 + run.deck.size() * 2;
        lastRunScore = run.score;
        lastRunSummary = "上一次：第 " + (run.act + 1) + " 幕 · 第 " + Math.max(1, run.floor) + " 层";
        SaveStore.clear(context);
        saveExists = false;
        screen = Screen.GAME_OVER;
        modal = Modal.NONE;
        invalidate();
    }

    private void finishVictory() {
        if (run == null) return;
        run.won = true;
        run.score = 1000 + run.floor * 15 + run.kills * 6 + run.maxHp / 2 + run.deck.size() * 2;
        lastRunScore = run.score;
        lastRunSummary = "上一次：登顶 · " + run.score + " 分";
        SaveStore.clear(context);
        saveExists = false;
        screen = Screen.VICTORY;
        modal = Modal.NONE;
        invalidate();
    }

    private void drawGameOver(Canvas c, boolean win) {
        float top = h * 0.12f;
        textCenter(c, win ? "塔 顶" : "墨 尽", w / 2f, top + px(2.2f), px(3.2f), INK, serifBold);
        textCenter(c, win ? "你翻到了最后一页。塔在你身后合上。" : "墨迹干涸，故事停在这里。",
                w / 2f, top + px(3.4f), px(0.72f), GRAY, serif);
        drawEndArt(c, w / 2f, top + px(7.0f), Math.min(w * 0.50f, px(10f)), win);
        int act = run == null ? 1 : run.act + 1;
        int floor = run == null ? 0 : run.floor;
        int kills = run == null ? 0 : run.kills;
        int deck = run == null ? 0 : run.deck.size();
        int gold = run == null ? 0 : run.gold;
        float sy = top + px(11.8f);
        String[][] stats = {
                {"抵达", "第 " + act + " 幕 · " + Math.max(1, floor) + " 层"},
                {"击倒", kills + " 个敌人"},
                {"牌组", deck + " 张牌"},
                {"金币", gold + ""},
                {"得分", lastRunScore + ""}
        };
        for (int i = 0; i < stats.length; i++) {
            float yy = sy + i * px(1.5f);
            text(c, stats[i][0], pad, yy, px(0.7f), GRAY, sans, Paint.Align.LEFT);
            textRight(c, stats[i][1], w - pad, yy, px(0.78f), INK, sansBold);
            hairline(c, yy + px(0.4f));
        }
        float by = h - px(5.7f);
        RectF again = new RectF(pad, by, w - pad, by + px(2.2f));
        button(c, again, "再次攀登", true, true, this::restartSameCharacter);
        RectF menu = new RectF(pad, by + px(2.7f), w - pad, by + px(4.7f));
        button(c, menu, "回到封面", false, true, () -> { screen = Screen.MENU; engine = null; run = null; invalidate(); });
    }

    private void drawEndArt(Canvas c, float cx, float cy, float size, boolean win) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(px(0.08f));
        paint.setColor(LIGHT);
        c.drawCircle(cx, cy, size * 0.45f, paint);
        if (win) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(INK);
            Path p = new Path();
            p.moveTo(cx, cy - size * 0.38f);
            p.lineTo(cx + size * 0.25f, cy);
            p.lineTo(cx, cy + size * 0.38f);
            p.lineTo(cx - size * 0.25f, cy);
            p.close();
            c.drawPath(p, paint);
            textCenter(c, "顶", cx, cy + px(0.65f), px(1.8f), PAPER, serifBold);
        } else {
            textCenter(c, "墨", cx, cy + px(0.9f), px(3.4f), INK_2, serifBold);
            line(c, cx - size * 0.35f, cy + size * 0.4f, cx + size * 0.35f, cy - size * 0.4f, INK, px(0.12f));
        }
    }

    // =====================================================================
    // card rows, detail modal, potions, confirm, toast
    // =====================================================================
    private void drawCardRow(Canvas c, RectF r, CardInstance card, boolean selected, String badge,
                             boolean enabled, Runnable action) {
        fillRound(c, r, selected ? WHITE : PAPER, px(0.15f));
        strokeRound(c, r, selected ? INK : (enabled ? INK_2 : LIGHT), selected ? px(0.12f) : px(0.06f), px(0.15f));
        float costD = Math.min(r.height() * 0.42f, px(1.5f));
        float cx = r.left + px(0.45f) + costD / 2f;
        float cy = r.top + px(0.4f) + costD / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(enabled ? INK : LIGHT);
        c.drawCircle(cx, cy, costD / 2f, paint);
        textCenter(c, card.def.xCost ? "X" : "" + card.cost(), cx, cy + px(0.27f), px(0.68f), PAPER, sansBold);
        float tx = cx + costD / 2f + px(0.5f);
        float badgeW = badge == null || badge.length() == 0 ? 0 : px(2.6f);
        text(c, fit(card.name(), px(0.78f), sansBold, r.right - tx - px(0.4f) - badgeW),
                tx, r.top + px(0.9f), px(0.78f), enabled ? INK : GRAY, sansBold, Paint.Align.LEFT);
        text(c, card.def.type.label + " · " + card.def.rarity.label + (card.upgraded ? " · 升级" : ""),
                tx, r.top + px(1.5f), px(0.52f), GRAY, sans, Paint.Align.LEFT);
        wrap(c, card.description(), tx, r.top + px(1.65f), r.right - tx - px(0.5f) - badgeW,
                px(0.6f), enabled ? INK_2 : LIGHT, px(0.86f), 2);
        if (badgeW > 0) {
            textRight(c, fit(badge, px(0.56f), sans, badgeW), r.right - px(0.45f), r.top + px(0.95f), px(0.56f), GRAY, sans);
        }
        if (action != null && enabled) hit(r, action);
    }

    private void drawCardModal(Canvas c) {
        if (detailCard == null) { modal = Modal.NONE; return; }
        fullBackdrop(c);
        text(c, "卡 牌", pad, pad + px(1.2f), px(1.3f), INK, serifBold, Paint.Align.LEFT);
        textRight(c, detailCard.def.type.label + " · " + detailCard.def.rarity.label, w - pad,
                pad + px(1.1f), px(0.62f), GRAY, sans);
        float top = pad + px(2.0f);
        float bottom = h - px(4.6f);
        RectF card = new RectF(pad, top, w - pad, bottom);
        fillRound(c, card, WHITE, px(0.22f));
        strokeRound(c, card, INK, px(0.12f), px(0.22f));
        RectF inner = new RectF(card.left + px(0.3f), card.top + px(0.3f), card.right - px(0.3f), card.bottom - px(0.3f));
        strokeRound(c, inner, LIGHT, px(0.05f), px(0.18f));
        float costD = px(2.3f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INK);
        c.drawCircle(card.left + px(1.0f) + costD / 2f, card.top + px(1.0f) + costD / 2f, costD / 2f, paint);
        textCenter(c, detailCard.def.xCost ? "X" : "" + detailCard.cost(),
                card.left + px(1.0f) + costD / 2f, card.top + px(1.0f) + costD / 2f + px(0.45f), px(1.15f), PAPER, sansBold);
        text(c, fit(detailCard.name(), px(1.7f), serifBold, card.width() - px(5.0f)),
                card.left + px(4.1f), card.top + px(2.1f), px(1.7f), INK, serifBold, Paint.Align.LEFT);
        text(c, detailCard.def.type.label + (detailCard.upgraded ? " · 已升级" : ""),
                card.left + px(4.1f), card.top + px(2.75f), px(0.66f), GRAY, sans, Paint.Align.LEFT);
        line(c, card.left + px(0.9f), card.top + px(3.35f), card.right - px(0.9f), card.top + px(3.35f), LIGHT, px(0.06f));
        float descTop = card.top + px(3.95f);
        wrap(c, detailCard.description(), card.left + px(0.95f), descTop, card.width() - px(1.9f),
                px(0.84f), INK_2, px(1.22f), 7);
        if (detailCard.def.flavor != null && detailCard.def.flavor.length() > 0) {
            float fy = card.bottom - px(1.8f);
            line(c, card.left + px(2.2f), fy - px(0.85f), card.right - px(2.2f), fy - px(0.85f), LIGHT, px(0.045f));
            textCenter(c, "「" + detailCard.def.flavor + "」", card.centerX(), fy, px(0.62f), GRAY, serif);
        }
        float by = h - px(3.7f);
        float half = (w - pad * 2 - px(0.7f)) / 2f;
        RectF close = new RectF(pad, by, pad + half, by + px(2.0f));
        button(c, close, "返回", false, true, () -> { modal = Modal.NONE; invalidate(); });
        RectF action = new RectF(pad + half + px(0.7f), by, w - pad, by + px(2.0f));
        if (detailMode == DetailMode.REWARD) {
            boolean can = !rewardTaken;
            button(c, action, rewardTaken ? "已拿取" : "拿取", true, can, this::takeDetailReward);
        } else {
            button(c, action, "关闭", true, true, () -> { modal = Modal.NONE; invalidate(); });
        }
    }

    private void takeDetailReward() {
        int idx = -1;
        for (int i = 0; i < rewardChoices.size(); i++) {
            if (rewardChoices.get(i).uid == detailCard.uid) { idx = i; break; }
        }
        if (idx >= 0) takeRewardCard(idx);
        modal = Modal.NONE;
        screen = Screen.REWARD;
        invalidate();
    }

    private void drawPotionsModal(Canvas c) {
        fullBackdrop(c);
        text(c, "药 水", pad, pad + px(1.3f), px(1.4f), INK, serifBold, Paint.Align.LEFT);
        int slots = run == null ? 0 : run.potionSlots();
        textRight(c, "药水袋 " + (run == null ? 0 : run.potions.size()) + " / " + slots,
                w - pad, pad + px(1.2f), px(0.62f), GRAY, sans);
        float y = pad + px(2.2f);
        if (run == null || run.potions.isEmpty()) {
            textCenter(c, "药水袋是空的。", w / 2f, y + px(3f), px(0.8f), GRAY, serif);
        } else {
            float rowH = Math.min(px(3.9f), (h - y - px(4.2f) - px(0.4f) * (run.potions.size() - 1))
                    / Math.max(1, run.potions.size()));
            rowH = Math.max(px(2.7f), rowH);
            for (int i = 0; i < run.potions.size(); i++) {
                PotionDef d = PotionLibrary.get(run.potions.get(i).id);
                if (d == null) continue;
                float top = y + i * (rowH + px(0.4f));
                RectF r = new RectF(pad, top, w - pad, top + rowH);
                fillRound(c, r, PAPER_2, px(0.18f));
                strokeRound(c, r, LIGHT, px(0.06f), px(0.18f));
                text(c, d.glyph, pad + px(0.85f), r.top + px(1.5f), px(1.35f), INK, serifBold, Paint.Align.LEFT);
                text(c, fit(d.name, px(0.8f), sansBold, r.width() - px(7.6f)), pad + px(2.35f),
                        r.top + px(1.05f), px(0.8f), INK, sansBold, Paint.Align.LEFT);
                wrap(c, d.description, pad + px(2.35f), r.top + px(1.25f), r.width() - px(7.6f),
                        px(0.62f), GRAY, px(0.94f), 2);
                boolean can = engine != null && engine.c.playerTurn && !engine.c.over;
                RectF use = new RectF(w - pad - px(3.3f), r.top + px(0.55f), w - pad - px(0.45f),
                        r.bottom - px(0.55f));
                final int idx = i;
                button(c, use, "使用", true, can, () -> usePotionByIndex(idx));
                RectF drop = new RectF(w - pad - px(6.6f), r.top + px(0.55f), w - pad - px(3.6f),
                        r.bottom - px(0.55f));
                if (!can) {
                    button(c, drop, "丢弃", false, true, () -> { discardPotion(idx); });
                }
            }
        }
        RectF close = new RectF(w / 2f - px(4f), h - px(3.8f), w / 2f + px(4f), h - px(2.1f));
        button(c, close, "返回", false, true, () -> { modal = Modal.NONE; invalidate(); });
    }

    private void discardPotion(int index) {
        if (run == null || index < 0 || index >= run.potions.size()) return;
        PotionDef d = PotionLibrary.get(run.potions.remove(index).id);
        toast = d == null ? "药水已丢弃。" : "倒掉了「" + d.name + "」。";
        modal = Modal.NONE;
        saveIfSafe();
        invalidate();
    }

    private void usePotionByIndex(int index) {
        if (engine == null || run == null || index < 0 || index >= run.potions.size()) return;
        int before = run.potions.size();
        engine.usePotion(index);
        modal = Modal.NONE;
        if (run.potions.size() == before) { toast = "现在无法使用药水。"; invalidate(); return; }
        afterCombatAction();
    }

    private void drawConfirm(Canvas c) {
        fullBackdrop(c);
        float boxW = Math.min(w * 0.78f, px(14f));
        RectF box = new RectF((w - boxW) / 2f, h * 0.31f, (w + boxW) / 2f, h * 0.62f);
        fillRound(c, box, PAPER, px(0.2f));
        strokeRound(c, box, INK, px(0.1f), px(0.2f));
        wrap(c, confirmText, box.left + px(1f), box.top + px(1.0f), box.width() - px(2f), px(0.8f), INK, px(1.15f), 4);
        float by = box.bottom - px(2.8f);
        float half = (box.width() - px(2.0f)) / 2f;
        button(c, new RectF(box.left + px(0.7f), by, box.left + px(0.7f) + half, by + px(1.8f)),
                "取消", false, true, () -> { modal = Modal.NONE; invalidate(); });
        button(c, new RectF(box.right - px(0.7f) - half, by, box.right - px(0.7f), by + px(1.8f)),
                "确认", true, true, this::confirmYes);
    }

    private void confirmYes() {
        modal = Modal.NONE;
        if (confirmAction == 1) {
            SaveStore.clear(context);
            saveExists = false;
            run = null;
            engine = null;
            screen = Screen.MENU;
            toast = "存档已清除。";
        } else if (confirmAction == 2 || confirmAction == 3) {
            SaveStore.clear(context);
            saveExists = false;
            run = null;
            engine = null;
            screen = Screen.CHARACTER;
        }
        invalidate();
    }

    private void drawToast(Canvas c) {
        if (toast == null || toast.length() == 0) return;
        boolean changed = toastShown == null ? true : !toastShown.equals(toast);
        if (changed) {
            toastShown = toast;
            removeCallbacks(toastClear);
            postDelayed(toastClear, 2600);
        }
        float maxW = w - pad * 3f;
        paint.setTypeface(sansBold);
        paint.setTextSize(px(0.65f));
        float tw = Math.min(paint.measureText(toast), maxW);
        float th = px(1.65f);
        float left = (w - tw - px(1.3f)) / 2f;
        float top = pad + px(1.72f);
        RectF r = new RectF(left, top, left + tw + px(1.3f), top + th);
        fillRound(c, r, INK, px(0.16f));
        textCenter(c, fit(toast, px(0.65f), sansBold, maxW), r.centerX(), r.centerY() + px(0.31f), px(0.65f), PAPER, sansBold);
    }

    // =====================================================================
    // cheat / lifecycle / input
    // =====================================================================
    private void drawCheat(Canvas c) {
        if (!BuildConfig.CHEAT) { screen = Screen.SETTINGS; return; }
        header(c, "作弊间", "关闭", () -> { screen = cheatBack; invalidate(); });
        text(c, "此版本的所有改动只影响本机存档。", pad, pad + px(2.3f), px(0.58f), GRAY, sans, Paint.Align.LEFT);
        float y = pad + px(3.0f);
        String[][] rows = {
                {"无限能量", "出牌不消耗能量。"},
                {"无敌", "敌人无法造成生命伤害。"},
                {"一击必杀", "你的攻击造成 9999 伤害。"},
                {"卡牌免费", "所有卡牌费用变为 0。"}
        };
        boolean[] values = {Cheats.infiniteEnergy, Cheats.invincible, Cheats.oneHit, Cheats.freeCards};
        for (int i = 0; i < rows.length; i++) {
            RectF r = new RectF(pad, y + i * px(3.0f), w - pad, y + i * px(3.0f) + px(2.55f));
            fillRound(c, r, vals(values[i]) ? PAPER_2 : PAPER, px(0.15f));
            strokeRound(c, r, LIGHT, px(0.055f), px(0.15f));
            text(c, rows[i][0], r.left + px(0.55f), r.top + px(0.95f), px(0.75f), INK, sansBold, Paint.Align.LEFT);
            text(c, rows[i][1], r.left + px(0.55f), r.top + px(1.55f), px(0.55f), GRAY, sans, Paint.Align.LEFT);
            RectF tg = new RectF(r.right - px(3.2f), r.top + px(0.65f), r.right - px(0.45f), r.bottom - px(0.65f));
            final int idx = i;
            button(c, tg, values[i] ? "已开" : "已关", values[i], true, () -> {
                switch (idx) {
                    case 0: Cheats.infiniteEnergy = !Cheats.infiniteEnergy; break;
                    case 1: Cheats.invincible = !Cheats.invincible; break;
                    case 2: Cheats.oneHit = !Cheats.oneHit; break;
                    case 3: Cheats.freeCards = !Cheats.freeCards; break;
                }
                invalidate();
            });
        }
        y += px(12.5f);
        float bh = px(2.0f);
        float bw = (w - pad * 2 - px(0.5f)) / 2f;
        String[] labels = {"金币 +9999", "回满生命", "随机遗物", "随机稀有牌", "直接胜利", "重置开关"};
        for (int i = 0; i < labels.length; i++) {
            float col = i % 2;
            float row = i / 2;
            RectF r = new RectF(pad + col * (bw + px(0.5f)), y + row * (bh + px(0.45f)),
                    pad + col * (bw + px(0.5f)) + bw, y + row * (bh + px(0.45f)) + bh);
            final int idx = i;
            boolean enabled = true;
            if (i == 4) enabled = engine != null && !engine.c.over;
            button(c, r, labels[i], false, enabled, () -> cheatAction(idx));
        }
        textCenter(c, "作弊不会上传，也不会影响标准版。", w / 2f, h - px(1.0f), px(0.55f), GRAY, serif);
    }

    private boolean vals(boolean b) { return b; }

    private void cheatAction(int i) {
        if (run == null) return;
        if (i == 0) run.gold += 9999;
        else if (i == 1) {
            run.hp = run.maxHp;
            if (engine != null) engine.c.hp = engine.c.maxHp;
        } else if (i == 2) {
            RelicDef r = RelicLibrary.random(run.rng, run.relics);
            if (r != null) { run.addRelic(r.id); toast = "获得遗物「" + r.name + "」。"; }
        } else if (i == 3) {
            CardDef c = rareCard();
            run.deck.add(new CardInstance(c, autoUpgrade(c)));
            toast = "获得稀有牌「" + c.name + "」。";
        } else if (i == 4) {
            if (engine != null && !engine.c.over) {
                Cheats.autoWin = true;
                screen = Screen.COMBAT;
                engine.checkAutoWin();
                Cheats.autoWin = false;
                modal = Modal.NONE;
                afterCombatAction();
                return;
            }
        } else {
            Cheats.infiniteEnergy = false; Cheats.invincible = false; Cheats.oneHit = false; Cheats.freeCards = false;
        }
        invalidate();
    }

    private CardDef rareCard() {
        for (int i = 0; i < 400; i++) {
            CardDef c = CardLibrary.randomReward(run.rng);
            if (c.rarity == com.inkspire.rogue.model.Rarity.RARE) return c;
        }
        return CardLibrary.get("bludgeon");
    }

    private void newRun() {
        openCharacterSelect();
    }

    private void askNewRun() {
        openCharacterSelect();
    }

    private void restartSameCharacter() {
        String id = run == null ? CharacterLibrary.defaultId() : run.characterId;
        startCharacter(id);
    }

    private void continueRun() {
        RunState loaded = SaveStore.load(context);
        if (loaded == null) {
            toast = "存档读取失败。";
            saveExists = false;
            invalidate();
            return;
        }
        run = loaded;
        engine = null;
        screen = Screen.MAP;
        modal = Modal.NONE;
        targetingCard = null;
        toast = "继续上次的攀登。";
        invalidate();
    }

    private void drawToastAgain(Canvas c) { /* retained for binary compatibility of older builds */ }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (e.getAction() == MotionEvent.ACTION_UP) {
            float x = e.getX(), y = e.getY();
            Hit chosen = null;
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (!h.backdrop && h.r.contains(x, y)) { chosen = h; break; }
            }
            if (chosen == null) {
                float bestDist = Float.MAX_VALUE;
                for (int i = hits.size() - 1; i >= 0; i--) {
                    Hit h = hits.get(i);
                    if (h.backdrop || h.pad == null || !h.pad.contains(x, y)) continue;
                    float d = distanceTo(h.r, x, y);
                    if (d < bestDist) { bestDist = d; chosen = h; }
                }
            }
            if (chosen != null) {
                toast = null;
                performClick();
                try {
                    chosen.action.run();
                } catch (Throwable error) {
                    fatalError = error;
                }
                invalidate();
                return true;
            }
            for (int i = hits.size() - 1; i >= 0; i--) {
                if (hits.get(i).backdrop) { toast = null; invalidate(); return true; }
            }
        }
        return true;
    }

    @Override
    public boolean performClick() { super.performClick(); return true; }

    public boolean handleBack() {
        if (modal != Modal.NONE) { modal = Modal.NONE; invalidate(); return true; }
        if (screen == Screen.CHARACTER) { screen = Screen.MENU; invalidate(); return true; }
        if (screen == Screen.DECK) { screen = deckBack; invalidate(); return true; }
        if (screen == Screen.RELICS) { screen = Screen.MAP; invalidate(); return true; }
        if (screen == Screen.HELP) { screen = helpBack; invalidate(); return true; }
        if (screen == Screen.SETTINGS) { screen = settingsBack; invalidate(); return true; }
        if (screen == Screen.CHEAT) { screen = cheatBack; invalidate(); return true; }
        if (screen == Screen.COMBAT || screen == Screen.REWARD || screen == Screen.SHOP
                || screen == Screen.REST || screen == Screen.EVENT || screen == Screen.TREASURE) {
            toast = "先完成这里。";
            invalidate();
            return true;
        }
        if (screen == Screen.MAP) {
            saveIfSafe();
            screen = Screen.MENU;
            invalidate();
            return true;
        }
        if (screen == Screen.GAME_OVER || screen == Screen.VICTORY) {
            screen = Screen.MENU; run = null; invalidate(); return true;
        }
        return false;
    }
}
