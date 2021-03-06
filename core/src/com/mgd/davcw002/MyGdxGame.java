package com.mgd.davcw002;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MyGdxGame extends ApplicationAdapter implements InputProcessor {
    private final int MAXHEALTH = 350;
    private final int HEALTHBONUS = 175;
    private final int HEALTHPENALTY = 100;
    private final int MAX_FUEL = 350;
    private final int FUEL_BONUS = 150;
    private final int FUEL_USE = 50;
    private final int HEALTH_TIMER = 150;
    private final int REWARD_TIMER = 100;
    private final int SCORE_TIMER = 1000;
    private enum GameState {
        START, GAMEOVER, PLAYING
    }
    private GameState gameState = GameState.START;

    private boolean addReward = false;
    private int score = 0;
    private TextButton startButton;
    private SpriteBatch batch;
    private Player player;
    private List<Enemy> enemies;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Vector2 touchStart;
    private Vector2 dragPoint;
    private Vector2 touchEnd;
    private boolean vectorDisplayed = true;
    boolean drawLine = false;
    private OrthographicCamera camera;
    private World world;
    private Vector2 forceToApply;
    private List<Entity> allEntities;
    private List<Entity> entitiesToDestroy;
    private List<Reward> rewards;
    private float startRange = 300;
    private boolean debugMode = false;
    private float leftEdge = 0;
    private float rightEdge = 0;
    private float topEdge = 0;
    private float bottomEdge = 0;
    private int health = MAXHEALTH;
    private int currentFuel = MAX_FUEL;
    private Timer timer = new Timer();
    private FreeTypeFontGenerator fontGenerator;
    private FreeTypeFontGenerator.FreeTypeFontParameter fontParamter;
    private BitmapFont uiFont;
    private BitmapFont buttonFont;
    private TextureRegion textureRegion;
    private Texture spaceTexture;

    @Override
    public void create() {
        fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("Sumana-Bold.ttf"));
        fontParamter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        fontParamter.flip = true;
        fontParamter.size = 32;
        FreeTypeFontGenerator.FreeTypeFontParameter buttonFontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        buttonFontParameter.flip = true;
        buttonFontParameter.size = 80;
        uiFont = fontGenerator.generateFont(fontParamter);
        buttonFont = fontGenerator.generateFont(buttonFontParameter);
        allEntities = new ArrayList<Entity>();
        entitiesToDestroy = new ArrayList<Entity>();
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        leftEdge = 0;
        rightEdge = Gdx.graphics.getWidth() - 10;
        topEdge = 0;
        bottomEdge = Gdx.graphics.getHeight() - 10;
        spaceTexture = new Texture(Gdx.files.internal("sprites/space_texture1.png"));
        spaceTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        textureRegion = new TextureRegion(spaceTexture);

        world = new World(new Vector2(0, 0), true);
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Body bodyA = contact.getFixtureA().getBody();
                Body bodyB = contact.getFixtureB().getBody();

                if (bodyB.equals(player.getBody())) {
                    checkCollisionType(bodyA);
                } else if (bodyA.equals(player.getBody())) {
                    checkCollisionType(bodyB);
                }
            }

            @Override
            public void endContact(Contact contact) {

            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {

            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {

            }
        });
        prepareGame();

        batch = new SpriteBatch();
        //font = new BitmapFont(Gdx.files.internal("font.font"), Gdx.files.internal("font.png"), true);
        font = new BitmapFont(true);
        uiFont.setColor(Color.WHITE);
        Gdx.input.setInputProcessor(this);
        shapeRenderer = new ShapeRenderer();
        UserInterfaceAssets uiAssets = new UserInterfaceAssets(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle(
            uiAssets.startButtonUp,
            uiAssets.startButtonDown,
            uiAssets.startButtonChecked,
            buttonFont
        );

        startButton = new TextButton("Play", new TextButton.TextButtonStyle(buttonStyle));
        startButton.setPosition(Gdx.graphics.getWidth()/2 - startButton.getWidth()/2, Gdx.graphics.getHeight()/2 - startButton.getHeight()/2);
    }

    private void increaseScore() {
        if (isPlaying()) {
            score++;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    increaseScore();
                }
            }, SCORE_TIMER);
        }
    }

    private void checkCollisionType(Body body) {
        int rewardToRemove = -9;
        for (Entity entity : allEntities) {
            if (entity.getBody().equals(body)) {
                if (entity instanceof Reward) {
                    rewardToRemove = rewardCollision((Reward) entity);
                }
                if (entity instanceof Enemy) {
                    enemyCollision((Enemy) entity);
                }
            }
        }
        if (rewardToRemove != -9) {
            Reward toRemove = rewards.remove(rewardToRemove);
            allEntities.remove(toRemove);
            entitiesToDestroy.add(toRemove);
        }
    }

    private int rewardCollision(Reward reward) {
        if (reward.getType() == Reward.TYPES.HEALTH) {
            if (health > 0) {
                health = health + HEALTHBONUS;
                if (health > MAXHEALTH) {
                    int scoreBonus = (health - MAXHEALTH)/2;
                    health = MAXHEALTH;
                    score += scoreBonus;
                }
            }
        } else if (reward.getType() == Reward.TYPES.FUEL) {
            currentFuel = currentFuel + FUEL_BONUS;
            if (currentFuel > MAX_FUEL) {
                int scoreBonus = (currentFuel - MAX_FUEL)/2;
                currentFuel = MAX_FUEL;
                score += scoreBonus;
            }
        }

        int indexToRemove = rewards.indexOf(reward);
        return indexToRemove;
    }

    private void enemyCollision(Enemy enemy) {
        health = health - HEALTHPENALTY;
        if (health < 0) {
            health = 0;
        }
    }

    private void reduceHealth() {
        if (isPlaying()) {
            health = health - 1;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    reduceHealth();
                }
            }, HEALTH_TIMER);
        }
    }

    private void initialiseEnemies() {
        enemies = new ArrayList<Enemy>();
        int numberEnemies = 10;
        Random random = new Random();

        for (int i = 0; i < numberEnemies; i++) {
            Enemy currentEnemy = new Enemy();
            currentEnemy.setBaseSprite(new Sprite(new Texture("sprites/enemy1.png")));
            findSpawnPoint(currentEnemy);
            enemies.add(currentEnemy);
            allEntities.add(currentEnemy);
        }
    }


    private void findSpawnPoint(Entity entity) {
        Random random = new Random();
        float positionX = 0;
        float positionY = 0;
        float leftBoundary = player.getCentreX() - startRange;
        float rightBoundary = player.getCentreX() + startRange;
        float topBoundary = player.getCentreY() - startRange;
        float bottomBoundary = player.getCentreY() + startRange;
        boolean validLocationFound = false;

        while (!validLocationFound) {
            boolean left = random.nextBoolean();
            if (left) {
                positionX = (float) (40 + (Math.random() * leftBoundary));
            } else {
                positionX = (float) (-40 + rightBoundary + (Math.random() * (Gdx.graphics.getWidth() - rightBoundary)));
            }

            boolean top = random.nextBoolean();
            if (top) {
                positionY = (float) (40 + Math.random() * topBoundary);
            } else {
                positionY = (float) (-40 + bottomBoundary + (Math.random() * (Gdx.graphics.getHeight() - bottomBoundary)));
            }

            if (entity instanceof Reward) {
                Reward rEntity = (Reward) entity;
                rEntity.setSize(60, 100);
            }
            else {
                entity.setSize(100, 100);
            }

            entity.setLocation(positionX, positionY);

            validLocationFound = true;
            for (Entity otherEntity : allEntities) {
                Rectangle r1 = entity.getBaseSprite().getBoundingRectangle();
                Rectangle r2 = otherEntity.getBaseSprite().getBoundingRectangle();
                if (r1.overlaps(r2)) {
                    validLocationFound = false;
                }
            }
        }

        entity.setBody(world, positionX, positionY);
        entity.initialiseFixture(8);
        entity.addForce(new Vector2(positionX, positionY).scl(100));
    }

    private void initialiseRewards() {
        Random random = new Random();
        rewards = new ArrayList<Reward>();
        int numberRewards = 8;
        for (int i = 0; i < numberRewards; i++) {
            Reward.TYPES rewardType;
            String spriteLocation;
            if (i % 3 == 0) {
                rewardType = Reward.TYPES.FUEL;
                spriteLocation = "sprites/fuel.png";
            } else {
                rewardType = Reward.TYPES.HEALTH;
                spriteLocation = "sprites/health.png";
            }
            Reward currentReward = new Reward(rewardType);
            Sprite baseSprite = new Sprite(new Texture(spriteLocation));
            baseSprite.flip(false, true);
            currentReward.setBaseSprite(baseSprite);
            findSpawnPoint(currentReward);
            rewards.add(currentReward);
            allEntities.add(currentReward);
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                readyForReward();
            }
        }, REWARD_TIMER);
    }

    private void readyForReward() {
        addReward = true;
    }

    private void addReward() {
        if (isPlaying()) {
            Random random = new Random();
            Reward.TYPES rewardType;
            String spriteLocation;
            if (random.nextBoolean()) {
                rewardType = Reward.TYPES.FUEL;
                spriteLocation = "sprites/fuel.png";
            } else {
                rewardType = Reward.TYPES.HEALTH;
                spriteLocation = "sprites/health.png";
            }
            Reward currentReward = new Reward(rewardType);
            Sprite baseSprite = new Sprite(new Texture(spriteLocation));
            baseSprite.setFlip(false, true);
            currentReward.setBaseSprite(baseSprite);
            findSpawnPoint(currentReward);
            rewards.add(currentReward);
            allEntities.add(currentReward);

            addReward = false;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    readyForReward();
                }
            }, REWARD_TIMER);
        }
    }


    @Override
    public void render() {
        executeDraw();
        processLogic();
    }

    private void executeDraw() {
        camera.setToOrtho(true, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        player.render(batch);
        for (Enemy enemy : enemies) {
            enemy.render(batch);
        }
        for (Reward reward : rewards) {
            reward.render(batch);
        }
        if (drawLine) {
            drawToPoint();
        }
        drawUI();

        float leftBoundary = player.getCentreX() - startRange;
        float rightBoundary = player.getCentreX() + startRange;
        float topBoundary = player.getCentreY() - startRange;
        float bottomBoundary = player.getCentreY() + startRange;

        batch.begin();
        batch.draw(spaceTexture,
            0, 0,
            spaceTexture.getWidth(), spaceTexture.getHeight(),
            textureRegion.getRegionX(), textureRegion.getRegionY(),
            textureRegion.getRegionWidth(), textureRegion.getRegionHeight()
        );
        batch.end();

        if (debugMode) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.BLUE);
            shapeRenderer.line(leftBoundary, 0, 0, leftBoundary, Gdx.graphics.getHeight(), 0);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.line(rightBoundary, 0, 0, rightBoundary, Gdx.graphics.getHeight(), 0);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.line(0, topBoundary, 0, Gdx.graphics.getWidth(), topBoundary, 0);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.line(0, bottomBoundary, 0, Gdx.graphics.getWidth(), bottomBoundary, 0);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.box(0, Gdx.graphics.getHeight() / 2, 0, 10, 10, 0);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.BLUE);
            shapeRenderer.box(Gdx.graphics.getWidth() - 10, Gdx.graphics.getHeight() / 2, 0, 10, 10, 0);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.GREEN);
            shapeRenderer.box(Gdx.graphics.getWidth() / 2, 0, 0, 10, 10, 0);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.box(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() - 10, 0, 10, 10, 0);
            shapeRenderer.end();
        }

        if (debugMode) {
            for (Entity e : allEntities) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(Color.CYAN);
                Rectangle r = e.getBaseSprite().getBoundingRectangle();
                shapeRenderer.box(r.x, r.y, 0, r.width, r.height, 0);
                shapeRenderer.end();
            }
        }

    }

    public void drawUI() {
        // draw health
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.box(leftEdge + 50, topEdge + 50, 0, health + 50, 50, 0);
        shapeRenderer.end();

        // draw fuel
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.box(rightEdge - 50 - MAX_FUEL, topEdge + 50, 0, currentFuel, 50, 0);
        shapeRenderer.end();

        // draw score
        String scoreStr = "health: " + health;
        float textWidth = Helpers.getTextWidth(uiFont, scoreStr);
        float drawPoint = Gdx.graphics.getWidth() / 2 - textWidth/2;
        batch.begin();

        uiFont.draw(batch, scoreStr, drawPoint, 20);
        batch.end();

        if (isStart()) {
            // draw game over message
            String[] messages = new String[1];
            messages[0] = "Touch Screen to Start Game";

            int newLine = 1;
            for (String message : messages) {
                textWidth = Helpers.getTextWidth(uiFont, message);
                drawPoint = Gdx.graphics.getWidth()/2 - textWidth/2;
                batch.begin();
                uiFont.draw(batch, message, drawPoint, (Helpers.getTextHeight(uiFont, message) * 3) * newLine);
                batch.end();
                newLine ++;
            }
            batch.begin();
            //startButton.draw(batch, 1.0f);
            batch.end();
        }

        if (isGameOver()) {
            // draw game over message
            String[] messages = new String[3];
            messages[0] = "Game Over";
            messages[1] = "Touch Screen to Restart";
            messages[2] = "health: " + health;

            int newLine = 1;
            for (String message : messages) {
                textWidth = Helpers.getTextWidth(uiFont, message);
                drawPoint = Gdx.graphics.getWidth()/2 - textWidth/2;
                batch.begin();
                uiFont.draw(batch, message, drawPoint, (Helpers.getTextHeight(uiFont, message) * 3) * newLine);
                batch.end();
                newLine ++;
            }
            batch.begin();
            //startButton.draw(batch, 1.0f);
            batch.end();
        }
    }

    private void processLogic() {
        if (isPlaying()) {
            if (health > 0) {
                if (addReward) {
                    addReward();
                }
                if (forceToApply != null) {
                    if (currentFuel >= FUEL_USE) {
                        player.getBody().applyForceToCenter(forceToApply, true);
                        forceToApply = null;
                        currentFuel = currentFuel - FUEL_USE;
                        if (currentFuel < 0) {
                            currentFuel = 0;
                        }
                    }
                }

                destroyQueuedEntities();
                world.step(Gdx.graphics.getDeltaTime(), 6, 2);
                player.updateLocation();
                for (Enemy enemy : enemies) {
                    enemy.updateLocation();
                }
                for (Reward reward : rewards) {
                    reward.updateLocation();
                }
            } else {
                entitiesToDestroy.addAll(allEntities);
                allEntities.clear();
                destroyQueuedEntities();
                gameState = GameState.GAMEOVER;
            }
        }
    }

    private void drawToPoint() {
        Color color;
        if (currentFuel > 0) {
            color = Color.CYAN;
        } else {
            color = Color.RED;
        }
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.line(touchStart.x, touchStart.y, 0, dragPoint.x, dragPoint.y, 0, color, color);
        shapeRenderer.end();

    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (isPlaying()) {
            if (touchStart == null) {
                touchStart = new Vector2(screenX, screenY);
                dragPoint = new Vector2(screenX, screenY);
            }
            drawLine = (touchStart != null && dragPoint != null);
            touchEnd = null;
        }

        return false;
    }

    private void log(String message) {
        Gdx.app.log("debug", message);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (isPlaying()) {
            touchEnd = new Vector2(screenX, screenY);
            if (touchStart != null) {
                forceToApply = touchStart.sub(touchEnd).scl(30);
            }

            touchStart = null;
            dragPoint = null;
            drawLine = false;
        } else if (isGameOver()) {
            prepareGame();
            startGame();
        } else if (isStart()) {
            startGame();
        }

        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        dragPoint = new Vector2(screenX, screenY);
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    private void destroyQueuedEntities() {
        while (!entitiesToDestroy.isEmpty()) {
            world.destroyBody(entitiesToDestroy.remove(0).getBody());
        }
    }

    public void startGame() {
        gameState = GameState.PLAYING;
        reduceHealth();
        increaseScore();
    }

    public void prepareGame() {
        currentFuel = MAX_FUEL;
        health = MAXHEALTH;
        score = 0;
        allEntities = new ArrayList<Entity>();
        entitiesToDestroy = new ArrayList<Entity>();
        player = new Player();
        Sprite playerBaseSprite = new Sprite(new Texture("sprites/player1.png"));
        playerBaseSprite.flip(false, true);
        player.setBaseSprite(playerBaseSprite);
        player.setBody(world, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
        player.setLocation(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
        player.setSize(80, 80);
        player.initialiseFixture(5);

        allEntities.add(player);
        initialiseEnemies();
        initialiseRewards();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }

    private boolean isGameOver() {
        return (gameState.equals(GameState.GAMEOVER));
    }

    private boolean isPlaying() {
        return (gameState.equals(GameState.PLAYING));
    }

    private boolean isStart() {
        return (gameState.equals(GameState.START));
    }
}
