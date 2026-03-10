import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class CatFish extends JPanel implements ActionListener, KeyListener {

    // ── Window ────────────────────────────────────────────────────────────────
    static final int W = 480, H = 640;

    // ── Cat ───────────────────────────────────────────────────────────────────
    double catX = W / 2.0;
    static final int CAT_Y = H - 100;
    static final int CAT_W = 64;
    static final int CAT_SPEED = 5;
    boolean moveLeft, moveRight;
    float catBlink = 0, blinkTimer = 0;
    float tailAnim = 0;
    float catchAnim = 0; // flash on catch
    boolean facingLeft = false;

    // ── Fish ──────────────────────────────────────────────────────────────────
    ArrayList<Fish> fishes  = new ArrayList<>();
    ArrayList<Fish> bombs   = new ArrayList<>(); // bad items (boots/trash)
    int frameCount = 0;
    int fishInterval = 90; // frames between fish spawns (speeds up)

    // ── Bubbles (background) ─────────────────────────────────────────────────
    float[][] bubbles = new float[18][4]; // x, y, r, speed

    // ── Particles ────────────────────────────────────────────────────────────
    ArrayList<float[]> particles = new ArrayList<>(); // x,y,vx,vy,life,r,g,b,a

    // ── Score / Lives ─────────────────────────────────────────────────────────
    int score = 0, bestScore = 0, lives = 3;
    ArrayList<float[]> scorePopups = new ArrayList<>(); // x,y,val,life

    // ── State ─────────────────────────────────────────────────────────────────
    enum State { MENU, PLAYING, DEAD }
    State state = State.MENU;

    // ── Timer / RNG ───────────────────────────────────────────────────────────
    Timer timer;
    Random rng = new Random();

    // ── Animation ─────────────────────────────────────────────────────────────
    float menuFloat = 0;
    float bgWave   = 0;

    // ── Pastel Palette ────────────────────────────────────────────────────────
    Color BG_SKY    = new Color(255, 240, 220);   // warm peach sky
    Color BG_MID    = new Color(200, 235, 255);   // soft blue horizon
    Color WATER_TOP = new Color(140, 210, 230);   // mint water
    Color WATER_BOT = new Color(80,  170, 200);   // deeper teal
    Color WATER_GL  = new Color(255, 255, 255, 60);
    Color GROUND_C  = new Color(210, 175, 130);   // sandy bank
    Color GRASS_C   = new Color(150, 210, 120);   // soft green
    Color CAT_BODY  = new Color(255, 200, 160);   // peach cat
    Color CAT_STRIPE= new Color(230, 160, 110);   // stripe
    Color CAT_BELLY = new Color(255, 230, 210);   // belly
    Color CAT_EYE   = new Color(80,  160,  80);   // green eyes
    Color CAT_NOSE  = new Color(255, 140, 160);   // pink nose
    Color CAT_INNER = new Color(255, 180, 190);   // ear inner
    Color FISH_1    = new Color(255, 160, 100);   // orange fish
    Color FISH_2    = new Color(160, 220, 255);   // blue fish
    Color FISH_3    = new Color(255, 210, 100);   // gold fish
    Color BOMB_C    = new Color(160, 130, 100);   // boot brown
    Color PANEL_BG  = new Color(255, 245, 235, 210);
    Color PANEL_BD  = new Color(220, 180, 140);
    Color TEXT_DARK = new Color(100,  70,  50);
    Color TEXT_PINK = new Color(240, 100, 130);
    Color TEXT_MINT = new Color( 80, 180, 160);
    Color STAR_C    = new Color(255, 220,  80);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    Font scoreFont, titleFont, subFont, tinyFont;

    // ── Fish inner class ──────────────────────────────────────────────────────
    class Fish {
        double x, y, speed;
        int type;        // 0=orange,1=blue,2=gold  (for bombs: 0=boot)
        boolean isBomb;
        float wobble = 0;
        boolean flipX;

        Fish(double x, double speed, int type, boolean isBomb) {
            this.x = x; this.y = -30; this.speed = speed;
            this.type = type; this.isBomb = isBomb;
            this.flipX = rng.nextBoolean();
        }

        void update() { y += speed; wobble += 0.18f; }

        boolean offScreen() { return y > H + 40; }

        boolean hitsCat() {
            return Math.abs(x - catX) < CAT_W / 2.0 + 18
                && Math.abs(y - CAT_Y) < 28;
        }

        Color color() {
            if (isBomb) return BOMB_C;
            return type == 0 ? FISH_1 : type == 1 ? FISH_2 : FISH_3;
        }

        int points() { return type == 0 ? 1 : type == 1 ? 2 : 3; }
    }

    // ── Constructor ───────────────────────────────────────────────────────────
    public CatFish() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (state == State.MENU) { startGame(); }
                else if (state == State.DEAD) { resetGame(); }
            }
        });

        for (float[] b : bubbles) resetBubble(b, true);

        scoreFont = new Font("Monospaced", Font.BOLD, 46);
        titleFont = new Font("Monospaced", Font.BOLD, 36);
        subFont   = new Font("Monospaced", Font.BOLD, 15);
        tinyFont  = new Font("Monospaced", Font.PLAIN, 13);

        timer = new Timer(16, this);
        timer.start();
    }

    void resetBubble(float[] b, boolean anywhere) {
        b[0] = rng.nextFloat() * W;
        b[1] = anywhere ? rng.nextFloat() * H : H + 20;
        b[2] = 4 + rng.nextFloat() * 10;
        b[3] = 0.4f + rng.nextFloat() * 0.6f;
    }

    void startGame() {
        state = State.PLAYING;
        frameCount = 0;
    }

    void resetGame() {
        catX = W / 2.0;
        fishes.clear(); bombs.clear(); particles.clear(); scorePopups.clear();
        score = 0; lives = 3; frameCount = 0; fishInterval = 90;
        catchAnim = 0;
        state = State.PLAYING;
    }

    // ── Game Loop ─────────────────────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        bgWave   += 0.03f;
        menuFloat+= 0.04f;
        tailAnim += 0.07f;
        blinkTimer += 0.02f;
        catBlink = (float)(0.5 + 0.5 * Math.sin(blinkTimer));
        if (catchAnim > 0) catchAnim -= 0.06f;

        for (float[] b : bubbles) {
            b[1] -= b[3];
            if (b[1] < -20) resetBubble(b, false);
        }
        updateParticles();
        updateScorePopups();

        if (state == State.PLAYING) updateGame();
        repaint();
    }

    void updateGame() {
        frameCount++;

        // Move cat
        if (moveLeft)  { catX -= CAT_SPEED; facingLeft = true; }
        if (moveRight) { catX += CAT_SPEED; facingLeft = false; }
        catX = Math.max(CAT_W / 2.0, Math.min(W - CAT_W / 2.0, catX));

        // Spawn fish & bombs
        if (frameCount % fishInterval == 0) spawnFish();
        if (frameCount % 270 == 135)        spawnBomb();

        // Speed up over time
        if (frameCount % 600 == 0 && fishInterval > 45) fishInterval -= 5;

        // Update fish
        for (int i = fishes.size()-1; i >= 0; i--) {
            Fish f = fishes.get(i);
            f.update();
            if (f.hitsCat()) {
                spawnCatchParticles((float)f.x, (float)f.y, f.color());
                scorePopups.add(new float[]{(float)f.x, (float)f.y - 20, f.points(), 1f});
                score += f.points();
                if (score > bestScore) bestScore = score;
                catchAnim = 1f;
                fishes.remove(i);
            } else if (f.offScreen()) {
                fishes.remove(i);
            }
        }

        // Update bombs
        for (int i = bombs.size()-1; i >= 0; i--) {
            Fish b = bombs.get(i);
            b.update();
            if (b.hitsCat()) {
                spawnBombParticles((float)b.x, (float)b.y);
                lives--;
                catchAnim = -1f; // negative = hurt flash
                bombs.remove(i);
                if (lives <= 0) { state = State.DEAD; return; }
            } else if (b.offScreen()) {
                bombs.remove(i);
            }
        }
    }

    void spawnFish() {
        double x = 30 + rng.nextDouble() * (W - 60);
        double speed = 2.0 + rng.nextDouble() * 2.5 + (90 - fishInterval) * 0.04;
        int type = rng.nextInt(10) < 5 ? 0 : rng.nextInt(10) < 7 ? 1 : 2;
        fishes.add(new Fish(x, speed, type, false));
    }

    void spawnBomb() {
        double x = 30 + rng.nextDouble() * (W - 60);
        double speed = 1.8 + rng.nextDouble() * 1.5;
        bombs.add(new Fish(x, speed, 0, true));
    }

    // ── Particles ─────────────────────────────────────────────────────────────
    void spawnCatchParticles(float x, float y, Color c) {
        for (int i = 0; i < 14; i++) {
            double ang = Math.random() * Math.PI * 2;
            float sp = (float)(Math.random() * 4 + 1.5);
            particles.add(new float[]{x, y,
                (float)(Math.cos(ang)*sp), (float)(Math.sin(ang)*sp),
                1f, c.getRed(), c.getGreen(), c.getBlue()});
        }
        // sparkles
        for (int i = 0; i < 6; i++) {
            double ang = Math.random() * Math.PI * 2;
            float sp = (float)(Math.random() * 3 + 2);
            particles.add(new float[]{x, y,
                (float)(Math.cos(ang)*sp), (float)(Math.sin(ang)*sp),
                1f, 255, 240, 100});
        }
    }

    void spawnBombParticles(float x, float y) {
        for (int i = 0; i < 16; i++) {
            double ang = Math.random() * Math.PI * 2;
            float sp = (float)(Math.random() * 5 + 1);
            particles.add(new float[]{x, y,
                (float)(Math.cos(ang)*sp), (float)(Math.sin(ang)*sp),
                1f, 180, 100, 60});
        }
    }

    void updateParticles() {
        for (int i = particles.size()-1; i >= 0; i--) {
            float[] p = particles.get(i);
            p[0] += p[2]; p[1] += p[3]; p[3] += 0.1f; p[4] -= 0.04f;
            if (p[4] <= 0) particles.remove(i);
        }
    }

    void updateScorePopups() {
        for (int i = scorePopups.size()-1; i >= 0; i--) {
            float[] p = scorePopups.get(i);
            p[1] -= 1.2f; p[3] -= 0.03f;
            if (p[3] <= 0) scorePopups.remove(i);
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g);
        drawBubbles(g);
        drawWater(g);
        drawGround(g);

        // hurt flash
        if (catchAnim < -0.1f) {
            g.setColor(new Color(255, 80, 80, (int)(Math.abs(catchAnim) * 80)));
            g.fillRect(0, 0, W, H);
        }
        // catch flash
        if (catchAnim > 0.1f) {
            g.setColor(new Color(255, 255, 200, (int)(catchAnim * 50)));
            g.fillRect(0, 0, W, H);
        }

        drawParticles(g);
        drawFishes(g);
        drawCat(g);
        drawHUD(g);
        drawScorePopups(g);

        if (state == State.MENU) drawMenu(g);
        if (state == State.DEAD) drawGameOver(g);
    }

    void drawBackground(Graphics2D g) {
        // Sky gradient
        GradientPaint sky = new GradientPaint(0, 0, BG_SKY, 0, H * 0.55f, BG_MID);
        g.setPaint(sky);
        g.fillRect(0, 0, W, H);

        // Soft clouds
        g.setColor(new Color(255, 255, 255, 140));
        drawCloud(g, 60, 60, 1.0f);
        drawCloud(g, 280, 45, 0.8f);
        drawCloud(g, 390, 80, 0.6f);

        // Sun
        g.setColor(new Color(255, 230, 130, 180));
        g.fillOval(W - 90, 30, 55, 55);
        g.setColor(new Color(255, 245, 180, 80));
        g.fillOval(W - 98, 22, 71, 71);
    }

    void drawCloud(Graphics2D g, int x, int y, float s) {
        int w = (int)(70 * s), h = (int)(30 * s);
        g.fillOval(x, y, w, h);
        g.fillOval(x + (int)(10*s), y - (int)(14*s), (int)(40*s), (int)(34*s));
        g.fillOval(x + (int)(30*s), y - (int)(8*s),  (int)(35*s), (int)(28*s));
    }

    void drawBubbles(Graphics2D g) {
        for (float[] b : bubbles) {
            float alpha = 0.15f + 0.1f * (float)Math.sin(b[1] * 0.05f);
            g.setColor(new Color(180, 230, 255, (int)(alpha * 255)));
            g.setStroke(new BasicStroke(1.2f));
            g.drawOval((int)b[0], (int)b[1], (int)b[2], (int)b[2]);
        }
    }

    void drawWater(Graphics2D g) {
        int waterY = H - 180;
        // Water body
        GradientPaint wp = new GradientPaint(0, waterY, WATER_TOP, 0, H, WATER_BOT);
        g.setPaint(wp);
        g.fillRect(0, waterY, W, H - waterY);

        // Wave lines
        g.setStroke(new BasicStroke(1.5f));
        for (int row = 0; row < 5; row++) {
            float wy = waterY + 20 + row * 20;
            g.setColor(new Color(255, 255, 255, 40 - row * 6));
            Path2D wave = new Path2D.Float();
            wave.moveTo(0, wy);
            for (int x = 0; x <= W; x += 4) {
                float dy = (float)(6 * Math.sin((x * 0.03f) + bgWave + row * 0.7f));
                wave.lineTo(x, wy + dy);
            }
            g.draw(wave);
        }

        // Water shimmer
        g.setColor(WATER_GL);
        for (int i = 0; i < 8; i++) {
            float sx = (i * 60 + frameCount * 0.8f) % W;
            float sy = waterY + 8 + i * 12;
            g.fillOval((int)sx, (int)sy, 30, 4);
        }
    }

    void drawGround(Graphics2D g) {
        int gy = H - 80;
        // Sandy bank
        GradientPaint gp = new GradientPaint(0, gy, GROUND_C, 0, H, new Color(180, 140, 90));
        g.setPaint(gp);
        g.fillRect(0, gy, W, H - gy);
        // Grass strip
        g.setColor(GRASS_C);
        g.fillRect(0, gy - 8, W, 16);
        // Grass tufts
        g.setColor(new Color(120, 190, 90));
        g.setStroke(new BasicStroke(2f));
        for (int x = 10; x < W; x += 22) {
            g.drawLine(x, gy - 8, x - 3, gy - 18);
            g.drawLine(x + 4, gy - 8, x + 5, gy - 20);
            g.drawLine(x + 8, gy - 8, x + 7, gy - 16);
        }
        // Pebbles
        g.setColor(new Color(200, 170, 130));
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < 12; i++) {
            int px = 20 + i * 40, py = gy + 20;
            g.fillOval(px, py, 12, 7);
        }
    }

    void drawFishes(Graphics2D g) {
        for (Fish f : fishes)  drawFish(g, f);
        for (Fish b : bombs)   drawBoot(g, b);
    }

    void drawFish(Graphics2D g, Fish f) {
        int fx = (int)f.x, fy = (int)f.y;
        float wb = (float)(Math.sin(f.wobble) * 4);

        Graphics2D fg = (Graphics2D) g.create();
        fg.translate(fx, fy + wb);
        if (f.flipX) fg.scale(-1, 1);

        Color c = f.color();
        Color dark = c.darker();

        // Tail
        int[] tx = {-10, -22, -22};
        int[] ty = {0, -9, 9};
        fg.setColor(dark);
        fg.fillPolygon(tx, ty, 3);

        // Body
        fg.setColor(c);
        fg.fillOval(-10, -11, 28, 22);

        // Belly
        fg.setColor(new Color(255, 255, 255, 120));
        fg.fillOval(-4, -5, 16, 10);

        // Scales hint
        fg.setColor(new Color(0,0,0,25));
        fg.setStroke(new BasicStroke(1f));
        for (int i = 0; i < 3; i++)
            fg.drawArc(-2 + i*6, -6, 8, 8, 0, 180);

        // Eye
        fg.setColor(Color.WHITE);
        fg.fillOval(10, -6, 8, 8);
        fg.setColor(new Color(40, 40, 80));
        fg.fillOval(12, -4, 5, 5);
        fg.setColor(Color.WHITE);
        fg.fillOval(15, -4, 2, 2);

        // Fin
        fg.setColor(dark);
        int[] fnx = {0, -4, 8};
        int[] fny = {-11, -20, -18};
        fg.fillPolygon(fnx, fny, 3);

        fg.dispose();

        // Stars for gold fish
        if (f.type == 2) {
            g.setColor(new Color(255, 220, 60, 160));
            g.setFont(tinyFont);
            g.drawString("★", fx + 6, fy - 18 + (int)wb);
        }
    }

    void drawBoot(Graphics2D g, Fish b) {
        int bx = (int)b.x, by = (int)b.y;
        float wb = (float)(Math.sin(b.wobble) * 3);

        Graphics2D bg2 = (Graphics2D) g.create();
        bg2.translate(bx, by + wb);

        // Boot shape (simple)
        bg2.setColor(BOMB_C);
        bg2.fillRoundRect(-12, -20, 24, 26, 8, 8);  // shaft
        bg2.fillRoundRect(-12,  2,  30, 14, 6, 6);  // sole
        bg2.setColor(new Color(130, 100, 70));
        bg2.setStroke(new BasicStroke(1.5f));
        bg2.drawRoundRect(-12, -20, 24, 26, 8, 8);
        bg2.drawRoundRect(-12,  2,  30, 14, 6, 6);
        // laces
        bg2.setColor(new Color(220, 200, 170));
        bg2.setStroke(new BasicStroke(1.5f));
        bg2.drawLine(-6, -10, 6, -10);
        bg2.drawLine(-6,  -4, 6,  -4);

        // Warning X
        bg2.setColor(new Color(255, 80, 80, 200));
        bg2.setFont(new Font("Monospaced", Font.BOLD, 11));
        bg2.drawString("✕", -5, -22);

        bg2.dispose();
    }

    void drawCat(Graphics2D g) {
        int cx = (int)catX, cy = CAT_Y;
        Graphics2D cg = (Graphics2D) g.create();
        cg.translate(cx, cy);
        if (facingLeft) cg.scale(-1, 1);

        // Shadow
        cg.setColor(new Color(0, 0, 0, 40));
        cg.fillOval(-28, 24, 56, 16);

        // Tail (wagging)
        float tw = (float)(Math.sin(tailAnim) * 30);
        cg.setColor(CAT_STRIPE);
        cg.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D tail = new Path2D.Float();
        tail.moveTo(-18, 10);
        tail.curveTo(-35, 0, -40 + tw, -20, -30 + tw, -35);
        cg.draw(tail);

        // Body
        cg.setColor(CAT_BODY);
        cg.fillOval(-22, -10, 44, 40);
        // Belly
        cg.setColor(CAT_BELLY);
        cg.fillOval(-12, -2, 26, 28);
        // Stripes on body
        cg.setColor(CAT_STRIPE);
        cg.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        cg.drawArc(-8, 5, 14, 14, 20, 130);
        cg.drawArc( 2, 5, 14, 14, 20, 130);

        // Head
        cg.setColor(CAT_BODY);
        cg.fillOval(-20, -44, 40, 38);

        // Ears
        cg.setColor(CAT_BODY);
        int[] elx = {-18, -26, -8};
        int[] ely = {-40, -62, -55};
        cg.fillPolygon(elx, ely, 3);
        int[] erx = {18, 26, 8};
        int[] ery = {-40, -62, -55};
        cg.fillPolygon(erx, ery, 3);
        // Ear inner
        cg.setColor(CAT_INNER);
        int[] ilx = {-16, -22, -10};
        int[] ily = {-42, -57, -52};
        cg.fillPolygon(ilx, ily, 3);
        int[] irx = {16, 22, 10};
        int[] iry = {-42, -57, -52};
        cg.fillPolygon(irx, iry, 3);

        // Stripes on head
        cg.setColor(CAT_STRIPE);
        cg.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        cg.drawLine(-8, -44, -10, -52);
        cg.drawLine( 0, -44,   0, -53);
        cg.drawLine( 8, -44,  10, -52);

        // Eyes (blink = thin)
        boolean blink = blinkTimer % (float)(Math.PI * 4) > (float)(Math.PI * 3.7f);
        if (blink) {
            cg.setColor(CAT_EYE);
            cg.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            cg.drawLine(-11, -30, -5, -30);
            cg.drawLine(  5, -30, 11, -30);
        } else {
            cg.setColor(Color.WHITE);
            cg.fillOval(-14, -36, 12, 12);
            cg.fillOval(  2, -36, 12, 12);
            cg.setColor(CAT_EYE);
            cg.fillOval(-12, -34,  8,  8);
            cg.fillOval(  4, -34,  8,  8);
            cg.setColor(Color.WHITE);
            cg.fillOval(-8, -32, 3, 3);
            cg.fillOval( 8, -32, 3, 3);
        }

        // Nose
        cg.setColor(CAT_NOSE);
        int[] nx = {-3, 3, 0};
        int[] ny = {-24, -24, -20};
        cg.fillPolygon(nx, ny, 3);

        // Mouth (happy when catching)
        cg.setColor(new Color(150, 80, 60));
        cg.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (catchAnim > 0.3f) {
            // happy open mouth
            cg.drawArc(-8, -22, 16, 10, 180, 180);
        } else {
            cg.drawLine(0, -20, -5, -16);
            cg.drawLine(0, -20,  5, -16);
        }

        // Whiskers
        cg.setColor(new Color(180, 140, 110, 180));
        cg.setStroke(new BasicStroke(1f));
        cg.drawLine(-4, -23, -26, -20);
        cg.drawLine(-4, -22, -25, -25);
        cg.drawLine( 4, -23,  26, -20);
        cg.drawLine( 4, -22,  25, -25);

        // Paws
        cg.setColor(CAT_BODY);
        cg.fillOval(-20, 26, 16, 10);
        cg.fillOval(  4, 26, 16, 10);
        cg.setColor(CAT_NOSE);
        for (int i = 0; i < 3; i++) {
            cg.fillOval(-18 + i*5, 30, 3, 3);
            cg.fillOval(  6 + i*5, 30, 3, 3);
        }

        cg.dispose();
    }

    void drawParticles(Graphics2D g) {
        for (float[] p : particles) {
            int alpha = (int)(p[4] * 220);
            if (alpha <= 0) continue;
            g.setColor(new Color((int)p[5], (int)p[6], (int)p[7], alpha));
            int sz = (int)(p[4] * 8 + 2);
            g.fillOval((int)p[0]-sz/2, (int)p[1]-sz/2, sz, sz);
        }
    }

    void drawHUD(Graphics2D g) {
        if (state != State.PLAYING) return;

        // Score
        g.setFont(scoreFont);
        g.setColor(new Color(0,0,0,60));
        drawCenteredString(g, String.valueOf(score), W/2+2, 57);
        g.setColor(TEXT_DARK);
        drawCenteredString(g, String.valueOf(score), W/2, 55);

        // Lives (cat faces)
        for (int i = 0; i < 3; i++) {
            int lx = 16 + i * 34, ly = 20;
            if (i < lives) {
                drawMiniCat(g, lx, ly, 1f);
            } else {
                drawMiniCat(g, lx, ly, 0.3f);
            }
        }

        // Fish type legend (top right)
        g.setFont(tinyFont);
        g.setColor(new Color(100,70,50,180));
        g.drawString("★=3pt  ~=2pt  o=1pt", W - 160, 20);
    }

    void drawMiniCat(Graphics2D g, int x, int y, float alpha) {
        Color c = new Color(255, 180, 130, (int)(alpha * 255));
        g.setColor(c);
        g.fillOval(x, y, 18, 14);
        // ears
        int[] ex1 = {x+1, x-2, x+6};
        int[] ey1 = {y+2, y-6, y-4};
        g.fillPolygon(ex1, ey1, 3);
        int[] ex2 = {x+17, x+20, x+12};
        int[] ey2 = {y+2, y-6, y-4};
        g.fillPolygon(ex2, ey2, 3);
        g.setColor(new Color(60,120,60,(int)(alpha*255)));
        g.fillOval(x+3, y+3, 4, 4);
        g.fillOval(x+11, y+3, 4, 4);
    }

    void drawScorePopups(Graphics2D g) {
        g.setFont(subFont);
        for (float[] p : scorePopups) {
            int alpha = (int)(p[3] * 255);
            if (alpha <= 0) continue;
            String txt = "+" + (int)p[2];
            g.setColor(new Color(240, 160, 50, alpha));
            g.setFont(new Font("Monospaced", Font.BOLD, 18));
            drawCenteredString(g, txt, (int)p[0], (int)p[1]);
        }
    }

    void drawMenu(Graphics2D g) {
        float bob = (float)(Math.sin(menuFloat) * 5);

        g.setColor(PANEL_BG);
        g.fillRoundRect(55, 160, W-110, 280, 30, 30);
        g.setColor(PANEL_BD);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(55, 160, W-110, 280, 30, 30);

        // Decorative fish icons
        g.setFont(new Font("Monospaced", Font.BOLD, 22));
        g.setColor(FISH_1);
        g.drawString("><>", 80, 195 + (int)bob);
        g.setColor(FISH_3);
        g.drawString("<><", W-115, 195 + (int)bob);

        g.setFont(titleFont);
        g.setColor(TEXT_PINK);
        drawCenteredString(g, "CAT & FISH", W/2, 225 + (int)bob);

        g.setColor(PANEL_BD);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(90, 238, W-90, 238);

        g.setFont(subFont);
        g.setColor(TEXT_DARK);
        drawCenteredString(g, "Catch fish, dodge boots!", W/2, 268);
        g.setColor(TEXT_MINT);
        drawCenteredString(g, "orange = 1pt", W/2, 295);
        drawCenteredString(g, "blue   = 2pt", W/2, 314);
        drawCenteredString(g, "gold   = 3pt", W/2, 333);
        g.setColor(new Color(220,100,80));
        drawCenteredString(g, "boot   = lose life!", W/2, 352);

        g.setFont(subFont);
        float pulse = (float)(0.6 + 0.4 * Math.sin(menuFloat * 2.5f));
        g.setColor(new Color(200, 100, 130, (int)(pulse * 255)));
        drawCenteredString(g, "[ SPACE or CLICK to start ]", W/2, 400);

        if (bestScore > 0) {
            g.setFont(tinyFont);
            g.setColor(new Color(180, 130, 80));
            drawCenteredString(g, "best: " + bestScore, W/2, 425);
        }
    }

    void drawGameOver(Graphics2D g) {
        g.setColor(PANEL_BG);
        g.fillRoundRect(55, 175, W-110, 280, 30, 30);
        g.setColor(TEXT_PINK);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(55, 175, W-110, 280, 30, 30);

        g.setFont(titleFont);
        g.setColor(TEXT_PINK);
        drawCenteredString(g, "GAYMAN", W/2, 230);

        g.setFont(subFont);
        g.setColor(new Color(160, 100, 70));
        drawCenteredString(g, "The cat is full...", W/2, 258);

        g.setColor(PANEL_BD);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(90, 272, W-90, 272);

        g.setFont(scoreFont);
        g.setColor(TEXT_DARK);
        drawCenteredString(g, String.valueOf(score), W/2, 330);
        g.setFont(subFont);
        g.setColor(new Color(160, 120, 80));
        drawCenteredString(g, "fish caught", W/2, 352);

        if (score >= bestScore && score > 0) {
            g.setColor(STAR_C);
            drawCenteredString(g, "★  NEW RECORD!  ★", W/2, 380);
        } else {
            g.setColor(new Color(160, 130, 90));
            drawCenteredString(g, "best: " + bestScore, W/2, 380);
        }

        float pulse = (float)(0.6 + 0.4 * Math.sin(menuFloat * 2.5f));
        g.setColor(new Color(200, 100, 130, (int)(pulse * 255)));
        g.setFont(subFont);
        drawCenteredString(g, "[ SPACE or CLICK to retry ]", W/2, 420);
    }

    void drawCenteredString(Graphics2D g, String s, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s)/2, y);
    }

    // ── Key Events ────────────────────────────────────────────────────────────
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_A) moveLeft  = true;
        if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) moveRight = true;
        if ((k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER)) {
            if (state == State.MENU)  startGame();
            else if (state == State.DEAD) resetGame();
        }
    }
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_A) moveLeft  = false;
        if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) moveRight = false;
    }
    public void keyTyped(KeyEvent e) {}

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("🐱 Cat & Fish");
            CatFish game = new CatFish();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
