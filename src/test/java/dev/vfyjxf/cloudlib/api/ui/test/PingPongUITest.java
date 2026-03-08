package dev.vfyjxf.cloudlib.api.ui.test;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PingPong game UI test. Tests real {@link PingPongGame} logic
 * through the widget event system via TestScene.
 */
class PingPongUITest {

    // ==================== Scene Builder ====================

    private record GameScene(TestScene scene, PingPongGame game) {}

    /**
     * Creates a TestScene with the real PingPongGame widget tree and logic.
     * Focuses the game field so key events are routed correctly.
     */
    private GameScene createGame() {
        var scene = TestScene.create(900, 600);
        var game = new PingPongGame();
        game.buildUI(scene.root());
        scene.setup();
        game.requestFocus(scene.scene());
        return new GameScene(scene, game);
    }

    // ==================== Structure Tests ====================

    @Nested
    class StructureTest {

        @Test
        void allPanelsExist() {
            var g = createGame();
            g.scene.assertExists("header");
            g.scene.assertExists("field");
            g.scene.assertExists("control-bar");
            g.scene.assertExists("settings-bar");
            g.scene.assertExists("status-bar");
        }

        @Test
        void headerContainsScoreElements() {
            var g = createGame();
            g.scene.assertExists("p1-label");
            g.scene.assertExists("p1-score");
            g.scene.assertExists("p2-label");
            g.scene.assertExists("p2-score");
            g.scene.assertExists("title");
        }

        @Test
        void fieldContainsGameElements() {
            var g = createGame();
            g.scene.assertExists("left-paddle");
            g.scene.assertExists("right-paddle");
            g.scene.assertExists("ball");
            g.scene.assertExists("top-wall");
            g.scene.assertExists("bottom-wall");
        }

        @Test
        void controlBarHasButtons() {
            var g = createGame();
            g.scene.assertExists("start-btn");
            g.scene.assertExists("pause-btn");
            g.scene.assertExists("reset-btn");
        }

        @Test
        void settingsBarHasControls() {
            var g = createGame();
            g.scene.assertExists("speed-label");
            g.scene.assertExists("speed-bar");
            g.scene.assertExists("speed-value");
            g.scene.assertExists("speed-down");
            g.scene.assertExists("speed-up");
        }

        @Test
        void statusBarHasLabels() {
            var g = createGame();
            g.scene.assertExists("status-text");
            g.scene.assertExists("round-text");
            g.scene.assertExists("hint-text");
        }

        @Test
        void initialScores() {
            var g = createGame();
            g.scene.assertThat("p1-score").hasText("0");
            g.scene.assertThat("p2-score").hasText("0");
        }

        @Test
        void initialGameState() {
            var g = createGame();
            g.scene.assertThat("status-text").hasText("READY");
            g.scene.assertThat("round-text").hasText("Round: 1");
        }

        @Test
        void initialSpeedDisplay() {
            var g = createGame();
            g.scene.assertThat("speed-value").hasText("5/10");
        }

        @Test
        void treeSnapshotMatchesExpected() {
            var g = createGame();
            var snapshot = g.scene.snapshot();
            assertNotNull(snapshot.findByKey("header"));
            assertNotNull(snapshot.findByKey("field"));
            assertNotNull(snapshot.findByKey("control-bar"));
            assertNotNull(snapshot.findByKey("settings-bar"));
            assertNotNull(snapshot.findByKey("status-bar"));
            assertNotNull(snapshot.findByKey("ball"));
            assertNotNull(snapshot.findByKey("left-paddle"));
            assertNotNull(snapshot.findByKey("right-paddle"));
        }
    }

    // ==================== Interaction Tests ====================

    @Nested
    class InteractionTest {

        @Test
        void startButtonStartsGame() {
            var g = createGame();
            g.scene.tap("start-btn");
            g.scene.assertThat("status-text").hasText("PLAYING");
            assertEquals(PingPongGame.State.RUNNING, g.game.gameState);
        }

        @Test
        void pauseButtonPausesRunningGame() {
            var g = createGame();
            g.scene.tap("start-btn");
            g.scene.tap("pause-btn");
            g.scene.assertThat("status-text").hasText("PAUSED");
            assertEquals(PingPongGame.State.PAUSED, g.game.gameState);
        }

        @Test
        void pauseButtonResumesFromPaused() {
            var g = createGame();
            g.scene.tap("start-btn");
            g.scene.tap("pause-btn");
            g.scene.tap("pause-btn");
            g.scene.assertThat("status-text").hasText("PLAYING");
            assertEquals(PingPongGame.State.RUNNING, g.game.gameState);
        }

        @Test
        void pauseButtonIgnoredWhenReady() {
            var g = createGame();
            g.scene.tap("pause-btn");
            g.scene.assertThat("status-text").hasText("READY");
            assertEquals(PingPongGame.State.READY, g.game.gameState);
        }

        @Test
        void resetButtonResetsEverything() {
            var g = createGame();
            g.scene.tap("start-btn");
            // Run a few ticks to change ball position
            g.scene.tick(10);
            g.scene.tap("reset-btn");

            g.scene.assertThat("status-text").hasText("READY");
            g.scene.assertThat("p1-score").hasText("0");
            g.scene.assertThat("p2-score").hasText("0");
            g.scene.assertThat("round-text").hasText("Round: 1");
            assertEquals(PingPongGame.State.READY, g.game.gameState);
            assertEquals(0.5f, g.game.ballX, 0.001f);
            assertEquals(0.5f, g.game.ballY, 0.001f);
        }

        @Test
        void spaceKeyStartsGame() {
            var g = createGame();
            g.scene.pressKey(GLFW.GLFW_KEY_SPACE);
            g.scene.assertThat("status-text").hasText("PLAYING");
            assertEquals(PingPongGame.State.RUNNING, g.game.gameState);
        }

        @Test
        void spaceKeyTogglesPause() {
            var g = createGame();
            g.scene.pressKey(GLFW.GLFW_KEY_SPACE); // start
            g.scene.pressKey(GLFW.GLFW_KEY_SPACE); // pause
            g.scene.assertThat("status-text").hasText("PAUSED");
            g.scene.pressKey(GLFW.GLFW_KEY_SPACE); // resume
            g.scene.assertThat("status-text").hasText("PLAYING");
        }

        @Test
        void speedUpButton() {
            var g = createGame();
            assertEquals(5, g.game.speed);
            g.scene.tap("speed-up");
            assertEquals(6, g.game.speed);
            g.scene.assertThat("speed-value").hasText("6/10");
        }

        @Test
        void speedDownButton() {
            var g = createGame();
            g.scene.tap("speed-down");
            assertEquals(4, g.game.speed);
            g.scene.assertThat("speed-value").hasText("4/10");
        }

        @Test
        void speedClampsAt10() {
            var g = createGame();
            for (int i = 0; i < 10; i++) g.scene.tap("speed-up");
            assertEquals(10, g.game.speed);
            g.scene.assertThat("speed-value").hasText("10/10");
        }

        @Test
        void speedClampsAt1() {
            var g = createGame();
            for (int i = 0; i < 10; i++) g.scene.tap("speed-down");
            assertEquals(1, g.game.speed);
            g.scene.assertThat("speed-value").hasText("1/10");
        }

        @Test
        void inputSequenceStartPauseReset() {
            var g = createGame();
            InputSequence.begin()
                    .moveTo("start-btn").click()
                    .tick()
                    .moveTo("pause-btn").click()
                    .tick()
                    .moveTo("reset-btn").click()
                    .executeOn(g.scene);
            g.scene.assertThat("status-text").hasText("READY");
        }
    }

    // ==================== Game Logic Tests ====================

    @Nested
    class GameLogicTest {

        @Test
        void gameStateTransitions() {
            var g = createGame();

            // READY -> start -> RUNNING
            g.scene.tap("start-btn");
            assertEquals(PingPongGame.State.RUNNING, g.game.gameState);

            // RUNNING -> pause -> PAUSED
            g.scene.tap("pause-btn");
            assertEquals(PingPongGame.State.PAUSED, g.game.gameState);

            // PAUSED -> start -> RUNNING
            g.scene.tap("start-btn");
            assertEquals(PingPongGame.State.RUNNING, g.game.gameState);

            // RUNNING -> reset -> READY
            g.scene.tap("reset-btn");
            assertEquals(PingPongGame.State.READY, g.game.gameState);
        }

        @Test
        void tickDoesNothingWhenReady() {
            var g = createGame();
            float startX = g.game.ballX;
            float startY = g.game.ballY;
            g.scene.tick(10);
            assertEquals(startX, g.game.ballX, 0.001f);
            assertEquals(startY, g.game.ballY, 0.001f);
        }

        @Test
        void tickMovesBallWhenRunning() {
            var g = createGame();
            g.scene.tap("start-btn");
            float startX = g.game.ballX;
            g.scene.tick(5);
            assertNotEquals(startX, g.game.ballX, "Ball should move after ticks");
        }

        @Test
        void tickDoesNotMoveBallWhenPaused() {
            var g = createGame();
            g.scene.tap("start-btn");
            g.scene.tick(5);
            g.scene.tap("pause-btn");
            float pausedX = g.game.ballX;
            float pausedY = g.game.ballY;
            g.scene.tick(10);
            assertEquals(pausedX, g.game.ballX, 0.001f);
            assertEquals(pausedY, g.game.ballY, 0.001f);
        }

        @Test
        void wKeyMovesLeftPaddleUp() {
            var g = createGame();
            g.scene.tap("start-btn");
            float startY = g.game.leftPaddleY;

            InputSequence.begin()
                    .keyDown(GLFW.GLFW_KEY_W)
                    .tick()
                    .keyUp(GLFW.GLFW_KEY_W)
                    .executeOn(g.scene);

            assertTrue(g.game.leftPaddleY < startY,
                    "Left paddle should move up (lower Y)");
        }

        @Test
        void sKeyMovesLeftPaddleDown() {
            var g = createGame();
            g.scene.tap("start-btn");
            float startY = g.game.leftPaddleY;

            InputSequence.begin()
                    .keyDown(GLFW.GLFW_KEY_S)
                    .tick()
                    .keyUp(GLFW.GLFW_KEY_S)
                    .executeOn(g.scene);

            assertTrue(g.game.leftPaddleY > startY,
                    "Left paddle should move down (higher Y)");
        }

        @Test
        void upKeyMovesRightPaddleUp() {
            var g = createGame();
            g.scene.tap("start-btn");
            float startY = g.game.rightPaddleY;

            InputSequence.begin()
                    .keyDown(GLFW.GLFW_KEY_UP)
                    .tick()
                    .keyUp(GLFW.GLFW_KEY_UP)
                    .executeOn(g.scene);

            assertTrue(g.game.rightPaddleY < startY,
                    "Right paddle should move up");
        }

        @Test
        void downKeyMovesRightPaddleDown() {
            var g = createGame();
            g.scene.tap("start-btn");
            float startY = g.game.rightPaddleY;

            InputSequence.begin()
                    .keyDown(GLFW.GLFW_KEY_DOWN)
                    .tick()
                    .keyUp(GLFW.GLFW_KEY_DOWN)
                    .executeOn(g.scene);

            assertTrue(g.game.rightPaddleY > startY,
                    "Right paddle should move down");
        }

        @Test
        void paddleYClampedAtTop() {
            var g = createGame();
            g.scene.tap("start-btn");

            // Hold W for many ticks to push paddle to top
            InputSequence.begin()
                    .keyDown(GLFW.GLFW_KEY_W)
                    .executeOn(g.scene);

            for (int i = 0; i < 100; i++) g.scene.tick();

            assertTrue(g.game.leftPaddleY >= 0.02f,
                    "Paddle should be clamped at minimum Y");
        }

        @Test
        void paddleYClampedAtBottom() {
            var g = createGame();
            g.scene.tap("start-btn");

            InputSequence.begin()
                    .keyDown(GLFW.GLFW_KEY_S)
                    .executeOn(g.scene);

            for (int i = 0; i < 100; i++) g.scene.tick();

            assertTrue(g.game.leftPaddleY <= 1f - PingPongGame.PADDLE_HEIGHT_FRAC - 0.02f,
                    "Paddle should be clamped at maximum Y");
        }

        @Test
        void ballBouncesOffTopWall() {
            var g = createGame();
            // Position ball near top wall moving upward
            g.game.ballY = 0.02f;
            g.game.ballVY = -0.01f;
            g.game.gameState = PingPongGame.State.RUNNING;

            g.scene.tick();

            assertTrue(g.game.ballVY > 0,
                    "Ball should bounce (positive VY) after hitting top wall");
        }

        @Test
        void ballBouncesOffBottomWall() {
            var g = createGame();
            g.game.ballY = 0.95f;
            g.game.ballVY = 0.01f;
            g.game.gameState = PingPongGame.State.RUNNING;

            g.scene.tick();

            assertTrue(g.game.ballVY < 0,
                    "Ball should bounce (negative VY) after hitting bottom wall");
        }

        @Test
        void scoringIncreasesRightScoreWhenBallGoesLeft() {
            var g = createGame();
            g.game.ballX = -0.01f;
            g.game.ballVX = -0.02f;
            g.game.gameState = PingPongGame.State.RUNNING;

            g.scene.tick();

            assertEquals(1, g.game.rightScore);
            g.scene.assertThat("p2-score").hasText("1");
        }

        @Test
        void scoringIncreasesLeftScoreWhenBallGoesRight() {
            var g = createGame();
            g.game.ballX = 1.01f;
            g.game.ballVX = 0.02f;
            g.game.gameState = PingPongGame.State.RUNNING;

            g.scene.tick();

            assertEquals(1, g.game.leftScore);
            g.scene.assertThat("p1-score").hasText("1");
        }

        @Test
        void scoringResetsAndIncrementsRound() {
            var g = createGame();
            g.game.ballX = -0.01f;
            g.game.ballVX = -0.02f;
            g.game.gameState = PingPongGame.State.RUNNING;

            g.scene.tick();

            assertEquals(2, g.game.round);
            g.scene.assertThat("round-text").hasText("Round: 2");
            assertEquals(0.5f, g.game.ballX, 0.001f);
            assertEquals(0.5f, g.game.ballY, 0.001f);
        }

        @Test
        void speedAffectsBallMovement() {
            var g = createGame();
            g.game.gameState = PingPongGame.State.RUNNING;

            // Speed 5 (default)
            float startX = g.game.ballX;
            g.scene.tick();
            float distAtSpeed5 = Math.abs(g.game.ballX - startX);

            // Reset and set speed 10
            g.game.ballX = 0.5f;
            g.game.speed = 10;
            startX = g.game.ballX;
            g.scene.tick();
            float distAtSpeed10 = Math.abs(g.game.ballX - startX);

            assertTrue(distAtSpeed10 > distAtSpeed5,
                    "Ball should move faster at higher speed");
        }
    }

    // ==================== Snapshot Diff Tests ====================

    @Nested
    class DiffTest {

        @Test
        void startGameChangesStatusText() {
            var g = createGame();
            var before = g.scene.snapshot();

            g.scene.tap("start-btn");
            var after = g.scene.snapshot();

            assertEquals("READY", before.findByKey("status-text").label());
            assertEquals("PLAYING", after.findByKey("status-text").label());
        }

        @Test
        void resetAfterScoringRestoresInitialState() {
            var g = createGame();

            // Score a point
            g.game.ballX = -0.01f;
            g.game.ballVX = -0.01f;
            g.game.gameState = PingPongGame.State.RUNNING;
            g.scene.tick();

            // Reset
            g.scene.tap("reset-btn");

            g.scene.assertThat("p1-score").hasText("0");
            g.scene.assertThat("p2-score").hasText("0");
            g.scene.assertThat("round-text").hasText("Round: 1");
            g.scene.assertThat("status-text").hasText("READY");
        }
    }
}
