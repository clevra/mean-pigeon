package com.zaal.pigeon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Iterator;

/**
 * Created by Arash on 2014-07-01.
 */
public class GameScreen implements Screen {
    final int ScreenWidth = 800;
    final int ScreenHeight = 480;
    final MeanPigeon game;

    Texture dropImage;
    Texture bucketImage;
    Texture poopImage;
    OrthographicCamera camera;
    Rectangle bird;
    Rectangle poop;
    Array<Rectangle> raindrops;
    long lastDropTime;
    long lastPoopUpdate;
    int dropsGathered;

    private static final int    FRAME_COLS = 5;     // #1
    private static final int    FRAME_ROWS = 3;     // #2

    Animation walkAnimation;      // #3
    Texture             walkSheet;      // #4
    TextureRegion[]         walkFrames;     // #5
    TextureRegion           currentFrame;       // #7

    float stateTime;                    // #8
    Boolean pooped = false;
    public GameScreen(final MeanPigeon gam) {
        this.game = gam;

        // load the images for the droplet and the bird, 64x64 pixels each
        dropImage = new Texture(Gdx.files.internal("droplet.png"));
        walkSheet = new Texture(Gdx.files.internal("sprite1.png")); // #9
        poopImage = new Texture(Gdx.files.internal("poop.png"));
        TextureRegion[][] tmp = TextureRegion.split(walkSheet, walkSheet.getWidth()/FRAME_COLS, walkSheet.getHeight()/FRAME_ROWS);              // #10
        walkFrames = new TextureRegion[(FRAME_COLS * FRAME_ROWS)-1];
        int index = 0;
        for (int i = 0; i < FRAME_ROWS; i++) {
            for (int j = 0; j < FRAME_COLS; j++) {
                if (i*j != (FRAME_COLS-1) * (FRAME_ROWS-1))

                walkFrames[index++] = tmp[i][j];
            }
        }
        walkAnimation = new Animation(0.050f, walkFrames);      // #11
        stateTime = 0f;                         // #13

        // create the camera and the SpriteBatch
        camera = new OrthographicCamera();
        camera.setToOrtho(false, ScreenWidth, ScreenHeight);

        // create a Rectangle to logically represent the bird
        poop = new Rectangle();
        bird = new Rectangle();
        bird.y = ScreenHeight  - 64 ; // center the bird horizontally
        bird.x = ScreenWidth / 3; // bottom left corner of the bird is 20 pixels above
        // the bottom screen edge
        bird.width = 64;
        bird.height = 64;

        // create the raindrops array and spawn the first raindrop
        raindrops = new Array<Rectangle>();
        spawnRaindrop();

    }

    private Rectangle spawnRaindrop() {
        Rectangle raindrop = new Rectangle();
        raindrop.x = ScreenWidth;
        raindrop.y = MathUtils.random(ScreenHeight/2, ScreenHeight - 96);
        raindrop.width = 64;
        raindrop.height = 64;
        raindrops.add(raindrop);
        lastDropTime = TimeUtils.nanoTime();
        return raindrop;
    }

    private void spawnPoop(){
        poop.x = bird.x;
        poop.y = bird.y + 32;
        poop.width = 10;
        poop.height = 10;
        pooped = true;
        lastPoopUpdate = TimeUtils.nanoTime();
    }
    @Override
    public void render(float delta) {
        // clear the screen with a dark blue color. The
        // arguments to glClearColor are the red, green
        // blue and alpha component in the range [0,1]
        // of the color to be used to clear the screen.
        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // tell the camera to update its matrices.
        camera.update();

        // tell the SpriteBatch to render in the
        // coordinate system specified by the camera.
        game.batch.setProjectionMatrix(camera.combined);

        // begin a new batch and draw the bird and
        // all drops
        game.batch.begin();
        stateTime += Gdx.graphics.getDeltaTime();           // #15
        currentFrame = walkAnimation.getKeyFrame(stateTime, true);  // #16
        game.batch.draw(currentFrame, bird.x, bird.y);             // #17
        game.font.draw(game.batch, "Drops Collected: " + dropsGathered, 0, 480);
//        game.batch.draw(bucketImage, bird.x, bird.y);
        for (Rectangle raindrop : raindrops) {
            game.batch.draw(dropImage, raindrop.x, raindrop.y);

        }
        if (pooped)
            game.batch.draw(poopImage, poop.x, poop.y);
        game.batch.end();

        // process user input
        if (Gdx.input.isTouched()) {
            Vector3 touchPos = new Vector3();
            touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            bird.y = touchPos.y - 64 / 2;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
            bird.y -= 200 * Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.UP))
            bird.y += 200 * Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && !pooped)
            spawnPoop();

        // make sure the bird stays within the screen bounds
        if (bird.y < 0)
            bird.y = 0;
        if (bird.y > ScreenHeight - 64)
            bird.y = ScreenHeight - 64;

        // check if we need to create a new raindrop
        if (TimeUtils.nanoTime() - lastDropTime > 2000000000) {
            spawnRaindrop();

        }
        if (TimeUtils.nanoTime() - lastPoopUpdate > 50000000) {
            poop.y -= 10;
            poop.x -= 5;
            lastPoopUpdate = TimeUtils.nanoTime();
            if (poop.x + 30 < 0 && poop.y + 30 < 0)
                pooped = false;
        }
        // move the raindrops, remove any that are beneath the bottom edge of
        // the screen or that hit the bird. In the later case we increase the
        // value our drops counter and add a sound effect.
        Iterator<Rectangle> iter = raindrops.iterator();
        while (iter.hasNext()) {
            Rectangle raindrop = iter.next();
            raindrop.x -= 200 * Gdx.graphics.getDeltaTime();
            if (raindrop.x + 64 < 0)
                iter.remove();
            if (raindrop.overlaps(bird)) {
                dropsGathered++;

                iter.remove();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {

    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        dropImage.dispose();
        bucketImage.dispose();

    }

}