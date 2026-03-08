package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.RoundedRectTexture;
import dev.vfyjxf.cloudlib.ui.widget.ButtonWidget;
import dev.vfyjxf.cloudlib.ui.widget.ProgressBarWidget;
import dev.vfyjxf.cloudlib.ui.widget.TextWidget;
import dev.vfyjxf.taffy.style.TaffyDimension;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;

/**
 * PingPong game logic and UI construction as a reusable middle layer.
 * Screen is just a wrapper; this class can be hosted by both
 * {@link PingPongScreen} and test scenes.
 */
public class PingPongGame {

    // ====== Game State (public for test access) ======
    public enum State {READY, RUNNING, PAUSED}

    public State gameState = State.READY;
    public int leftScore = 0;
    public int rightScore = 0;
    public int round = 1;

    // Ball position/velocity as fraction [0,1] of field dimensions
    public float ballX = 0.5f, ballY = 0.5f;
    public float ballVX = 0.004f, ballVY = 0.003f;

    // Paddle Y position as fraction [0,1]
    public float leftPaddleY = 0.35f;
    public float rightPaddleY = 0.40f;
    public static final float PADDLE_SPEED = 0.02f;
    public static final float PADDLE_HEIGHT_FRAC = 0.2f;

    // Key held state
    public boolean wHeld, sHeld, upHeld, downHeld;

    // Speed setting: 1-10
    public int speed = 5;

    // ====== Widget References ======
    private WidgetGroup<Widget> rootWidget;
    private TextWidget p1ScoreText;
    private TextWidget p2ScoreText;
    private TextWidget statusText;
    private TextWidget roundText;
    private TextWidget speedValueText;
    private ProgressBarWidget speedBarWidget;
    private WidgetGroup<Widget> ballWidget;
    private WidgetGroup<Widget> leftPaddleWidget;
    private WidgetGroup<Widget> rightPaddleWidget;

    /**
     * Builds the complete PingPong UI into the given root group.
     * Registers tick, key, and button event handlers on the widget tree.
     */
    public void buildUI(WidgetGroup<Widget> root) {
        rootWidget = root;
        root.useStyle(UIStyle.of(
                sizeFull(),
                background(new ColorTexture(0xFF0A0A1A)),
                flexColumn()
        ));

        // ====== Header Bar ======
        WidgetGroup<Widget> header = new WidgetGroup<>();
        header.setKey("header");
        header.useStyle(UIStyle.of(
                heightOf(40), widthOf(TaffyDimension.percent(1)),
                background(new ColorTexture(0xFF151530)),
                flexRow(), padding(0, 20), alignItemsCenter()
        ));
        addText(header, "Player 1", 0xFF58A6FF, "p1-label");
        p1ScoreText = addText(header, "0", 0xFFFFFFFF, "p1-score");
        header.addWidget(spacer());
        addTitle(header);
        header.addWidget(spacer());
        p2ScoreText = addText(header, "0", 0xFFFFFFFF, "p2-score");
        addText(header, "Player 2", 0xFFFF6B6B, "p2-label");
        root.addWidget(header);

        // ====== Game Field ======
        WidgetGroup<Widget> field = new WidgetGroup<>();
        field.setKey("field");
        field.useStyle(UIStyle.of(
                widthOf(TaffyDimension.percent(1)), flexGrow(1),
                background(new ColorTexture(0xFF0D0D22))
        ));
        field.setTickable(true);

        // Center dashed line
        for (int i = 0; i < 20; i++) {
            WidgetGroup<Widget> dash = new WidgetGroup<>();
            dash.useStyle(UIStyle.of(
                    sizeOf(3, 18),
                    background(new ColorTexture(0xFF333355)),
                    positionAbsolute(),
                    insetLeftPercent(0.5f), insetTop(i * 22)
            ));
            field.addWidget(dash);
        }

        // Left paddle
        leftPaddleWidget = new WidgetGroup<>();
        leftPaddleWidget.setKey("left-paddle");
        leftPaddleWidget.useStyle(UIStyle.of(
                sizeOf(12, 80),
                background(new ColorTexture(0xFF58A6FF)),
                positionAbsolute(), insetLeftPercent(0.02f), insetTopPercent(leftPaddleY)
        ));
        field.addWidget(leftPaddleWidget);

        // Right paddle
        rightPaddleWidget = new WidgetGroup<>();
        rightPaddleWidget.setKey("right-paddle");
        rightPaddleWidget.useStyle(UIStyle.of(
                sizeOf(12, 80),
                background(new ColorTexture(0xFFFF6B6B)),
                positionAbsolute(), insetRightPercent(0.02f), insetTopPercent(rightPaddleY)
        ));
        field.addWidget(rightPaddleWidget);

        // Ball
        ballWidget = new WidgetGroup<>();
        ballWidget.setKey("ball");
        ballWidget.useStyle(UIStyle.of(
                sizeOf(14, 14),
                background(RoundedRectTexture.of(0xFF00FF88, 7)),
                positionAbsolute(), insetLeftPercent(ballX), insetTopPercent(ballY)
        ));
        field.addWidget(ballWidget);

        // Top & bottom walls
        WidgetGroup<Widget> topWall = new WidgetGroup<>();
        topWall.setKey("top-wall");
        topWall.useStyle(UIStyle.of(
                widthOf(TaffyDimension.percent(1)), heightOf(3),
                background(new ColorTexture(0xFF333355)),
                positionAbsolute(), insetLeft(0), insetTop(0)
        ));
        field.addWidget(topWall);

        WidgetGroup<Widget> bottomWall = new WidgetGroup<>();
        bottomWall.setKey("bottom-wall");
        bottomWall.useStyle(UIStyle.of(
                widthOf(TaffyDimension.percent(1)), heightOf(3),
                background(new ColorTexture(0xFF333355)),
                positionAbsolute(), insetLeft(0), insetBottom(0)
        ));
        field.addWidget(bottomWall);

        field.onTick(this::gameTick);
        root.onKeyPressed((input, ctx) -> handleKeyDown(input));
        root.onKeyReleased((input, ctx) -> handleKeyUp(input));
        root.addWidget(field);

        // ====== Control Bar ======
        WidgetGroup<Widget> controlBar = new WidgetGroup<>();
        controlBar.setKey("control-bar");
        controlBar.useStyle(UIStyle.of(
                heightOf(40), widthOf(TaffyDimension.percent(1)),
                background(new ColorTexture(0xFF151530)),
                flexRow(), padding(8), alignItemsCenter(), columnGap(12)
        ));
        controlBar.addWidget(spacer());

        ButtonWidget startBtn = ButtonWidget.of("Start", this::onStart);
        startBtn.setKey("start-btn");
        startBtn.useStyle(UIStyle.of(sizeOf(80, 24)));
        startBtn.setColors(0xFF238636, 0xFF2EA043, 0xFF196C2E);
        startBtn.setTextColor(0xFFFFFFFF);
        controlBar.addWidget(startBtn);

        ButtonWidget pauseBtn = ButtonWidget.of("Pause", this::onPause);
        pauseBtn.setKey("pause-btn");
        pauseBtn.useStyle(UIStyle.of(sizeOf(80, 24)));
        pauseBtn.setColors(0xFFD29922, 0xFFE3B341, 0xFFBB8009);
        pauseBtn.setTextColor(0xFFFFFFFF);
        controlBar.addWidget(pauseBtn);

        ButtonWidget resetBtn = ButtonWidget.of("Reset", this::onReset);
        resetBtn.setKey("reset-btn");
        resetBtn.useStyle(UIStyle.of(sizeOf(80, 24)));
        resetBtn.setColors(0xFFDA3633, 0xFFFF7B72, 0xFF8B1A1A);
        resetBtn.setTextColor(0xFFFFFFFF);
        controlBar.addWidget(resetBtn);

        controlBar.addWidget(spacer());
        root.addWidget(controlBar);

        // ====== Settings Bar ======
        WidgetGroup<Widget> settingsBar = new WidgetGroup<>();
        settingsBar.setKey("settings-bar");
        settingsBar.useStyle(UIStyle.of(
                heightOf(40), widthOf(TaffyDimension.percent(1)),
                background(new ColorTexture(0xFF101025)),
                flexRow(), padding(8, 20), alignItemsCenter(), columnGap(12)
        ));

        addText(settingsBar, "Speed:", 0xFF8B949E, "speed-label");

        speedBarWidget = ProgressBarWidget.create();
        speedBarWidget.setKey("speed-bar");
        speedBarWidget.setProgress(speed / 10.0);
        speedBarWidget.setBackgroundTexture(new ColorTexture(0xFF30363D));
        speedBarWidget.setFillTexture(new ColorTexture(0xFF58A6FF));
        speedBarWidget.useStyle(UIStyle.of(sizeOf(120, 8)));
        settingsBar.addWidget(speedBarWidget);

        speedValueText = addText(settingsBar, speed + "/10", 0xFF8B949E, "speed-value");
        settingsBar.addWidget(spacer());

        ButtonWidget speedDown = ButtonWidget.of("-", this::onSpeedDown);
        speedDown.setKey("speed-down");
        speedDown.useStyle(UIStyle.of(sizeOf(24, 24)));
        speedDown.setColors(0xFF30363D, 0xFF484F58, 0xFF21262D);
        speedDown.setTextColor(0xFFFFFFFF);
        settingsBar.addWidget(speedDown);

        ButtonWidget speedUp = ButtonWidget.of("+", this::onSpeedUp);
        speedUp.setKey("speed-up");
        speedUp.useStyle(UIStyle.of(sizeOf(24, 24)));
        speedUp.setColors(0xFF30363D, 0xFF484F58, 0xFF21262D);
        speedUp.setTextColor(0xFFFFFFFF);
        settingsBar.addWidget(speedUp);
        root.addWidget(settingsBar);

        // ====== Status Bar ======
        WidgetGroup<Widget> statusBar = new WidgetGroup<>();
        statusBar.setKey("status-bar");
        statusBar.useStyle(UIStyle.of(
                heightOf(40), widthOf(TaffyDimension.percent(1)),
                background(new ColorTexture(0xFF151530)),
                flexRow(), padding(8, 20), alignItemsCenter(), columnGap(8)
        ));
        statusText = addText(statusBar, "READY", 0xFF00FF88, "status-text");
        statusBar.addWidget(spacer());
        roundText = addText(statusBar, "Round: 1", 0xFF8B949E, "round-text");
        statusBar.addWidget(spacer());
        addText(statusBar, "W/S \u2191\u2193 to move", 0xFF484F58, "hint-text");
        root.addWidget(statusBar);
    }

    /**
     * Requests focus on the game field. Call after scene is mounted.
     */
    public void requestFocus(Scene scene) {
        scene.requestFocus(rootWidget);
    }

    // ====== Helpers ======

    private static TextWidget addText(WidgetGroup<Widget> parent, String text, int color, String key) {
        TextWidget tw = TextWidget.of(Component.literal(text));
        tw.setKey(key);
        tw.setColor(color);
        tw.useStyle(UIStyle.of(heightOf(14)));
        parent.addWidget(tw);
        return tw;
    }

    private static void addTitle(WidgetGroup<Widget> parent) {
        TextWidget title = TextWidget.of(Component.literal("PONG"));
        title.setKey("title");
        title.setColor(0xFF00FF88);
        title.useStyle(UIStyle.of(sizeOf(80, 24)));
        parent.addWidget(title);
    }

    private static WidgetGroup<Widget> spacer() {
        WidgetGroup<Widget> s = new WidgetGroup<>();
        s.useStyle(UIStyle.of(flexGrow(1), heightOf(16)));
        return s;
    }

    // ====== Game Tick ======

    private void gameTick() {
        if (gameState != State.RUNNING) return;

        // Move paddles based on held keys
        if (wHeld) leftPaddleY = Math.max(0.02f, leftPaddleY - PADDLE_SPEED);
        if (sHeld) leftPaddleY = Math.min(1f - PADDLE_HEIGHT_FRAC - 0.02f, leftPaddleY + PADDLE_SPEED);
        if (upHeld) rightPaddleY = Math.max(0.02f, rightPaddleY - PADDLE_SPEED);
        if (downHeld) rightPaddleY = Math.min(1f - PADDLE_HEIGHT_FRAC - 0.02f, rightPaddleY + PADDLE_SPEED);

        leftPaddleWidget.useStyle(insetTopPercent(leftPaddleY));
        rightPaddleWidget.useStyle(insetTopPercent(rightPaddleY));

        // Move ball
        float speedMultiplier = speed / 5.0f;
        ballX += ballVX * speedMultiplier;
        ballY += ballVY * speedMultiplier;

        // Ball-wall collision (top/bottom bounce)
        if (ballY <= 0.01f) {
            ballY = 0.01f;
            ballVY = Math.abs(ballVY);
        }
        if (ballY >= 0.96f) {
            ballY = 0.96f;
            ballVY = -Math.abs(ballVY);
        }

        // Ball-paddle collision (left paddle at ~2-5% from left)
        if (ballX <= 0.05f && ballX >= 0.02f) {
            if (ballY + 0.02f >= leftPaddleY && ballY <= leftPaddleY + PADDLE_HEIGHT_FRAC) {
                ballVX = Math.abs(ballVX);
                float hitPos = (ballY - leftPaddleY) / PADDLE_HEIGHT_FRAC;
                ballVY = (hitPos - 0.5f) * 0.008f;
            }
        }

        // Ball-paddle collision (right paddle at ~93-96% from left)
        if (ballX >= 0.93f && ballX <= 0.96f) {
            if (ballY + 0.02f >= rightPaddleY && ballY <= rightPaddleY + PADDLE_HEIGHT_FRAC) {
                ballVX = -Math.abs(ballVX);
                float hitPos = (ballY - rightPaddleY) / PADDLE_HEIGHT_FRAC;
                ballVY = (hitPos - 0.5f) * 0.008f;
            }
        }

        // Scoring
        if (ballX < -0.02f) {
            rightScore++;
            p2ScoreText.setText(String.valueOf(rightScore));
            resetBall();
        } else if (ballX > 1.02f) {
            leftScore++;
            p1ScoreText.setText(String.valueOf(leftScore));
            resetBall();
        }

        ballWidget.useStyle(insetLeftPercent(ballX), insetTopPercent(ballY));
    }

    private void resetBall() {
        ballX = 0.5f;
        ballY = 0.5f;
        ballVX = (round % 2 == 0) ? 0.004f : -0.004f;
        ballVY = ((float) Math.random() - 0.5f) * 0.006f;
        round++;
        roundText.setText("Round: " + round);
    }

    // ====== Button Handlers ======

    void onStart() {
        if (gameState != State.RUNNING) {
            gameState = State.RUNNING;
            statusText.setText("PLAYING");
            statusText.setColor(0xFFFFFF00);
        }
    }

    void onPause() {
        if (gameState == State.RUNNING) {
            gameState = State.PAUSED;
            statusText.setText("PAUSED");
            statusText.setColor(0xFFD29922);
        } else if (gameState == State.PAUSED) {
            gameState = State.RUNNING;
            statusText.setText("PLAYING");
            statusText.setColor(0xFFFFFF00);
        }
    }

    void onReset() {
        gameState = State.READY;
        leftScore = 0;
        rightScore = 0;
        round = 1;
        ballX = 0.5f;
        ballY = 0.5f;
        ballVX = 0.004f;
        ballVY = 0.003f;
        leftPaddleY = 0.35f;
        rightPaddleY = 0.40f;

        p1ScoreText.setText("0");
        p2ScoreText.setText("0");
        roundText.setText("Round: 1");
        statusText.setText("READY");
        statusText.setColor(0xFF00FF88);

        ballWidget.useStyle(insetLeftPercent(ballX), insetTopPercent(ballY));
        leftPaddleWidget.useStyle(insetTopPercent(leftPaddleY));
        rightPaddleWidget.useStyle(insetTopPercent(rightPaddleY));
    }

    void onSpeedUp() {
        if (speed < 10) {
            speed++;
            updateSpeedUI();
        }
    }

    void onSpeedDown() {
        if (speed > 1) {
            speed--;
            updateSpeedUI();
        }
    }

    private void updateSpeedUI() {
        speedBarWidget.setProgress(speed / 10.0);
        speedValueText.setText(speed + "/10");
    }

    // ====== Keyboard Input (via Widget events) ======

    private EventDispatch handleKeyDown(InputContext input) {
        int keyCode = input.key().getValue();
        switch (keyCode) {
            case GLFW.GLFW_KEY_W -> wHeld = true;
            case GLFW.GLFW_KEY_S -> sHeld = true;
            case GLFW.GLFW_KEY_UP -> upHeld = true;
            case GLFW.GLFW_KEY_DOWN -> downHeld = true;
            case GLFW.GLFW_KEY_SPACE -> {
                if (gameState == State.READY) onStart();
                else onPause();
            }
            default -> { return EventDispatch.pass; }
        }
        return EventDispatch.consumed;
    }

    private EventDispatch handleKeyUp(InputContext input) {
        int keyCode = input.key().getValue();
        switch (keyCode) {
            case GLFW.GLFW_KEY_W -> wHeld = false;
            case GLFW.GLFW_KEY_S -> sHeld = false;
            case GLFW.GLFW_KEY_UP -> upHeld = false;
            case GLFW.GLFW_KEY_DOWN -> downHeld = false;
            default -> { return EventDispatch.pass; }
        }
        return EventDispatch.consumed;
    }
}
