package sample;

import javafx.animation.AnimationTimer;
import javafx.beans.property.ReadOnlyStringWrapper;

public class CustomTimer extends AnimationTimer {

    interface Update {
        void update();
    }

    interface Render {
        void render();
    }

    protected ReadOnlyStringWrapper textFps = new ReadOnlyStringWrapper(this,
            "fpsText", "Frame count: 0 Average frame interval: N/A");

    protected Update updater;

    private Render renderer;

    protected long firstTime = 0;

    protected long lastTime = 0;

    protected long accumulatedTime = 0;

    protected int frames = 0;

    @Override
    public void handle(long now) {
        if ( lastTime > 0 ) {
            long elapsedTime = now - lastTime;
            accumulatedTime += elapsedTime;
            updater.update();
        } else {
            firstTime = now;
        }
        lastTime = now;

        if ( accumulatedTime >= 1000000000L ) {
            accumulatedTime -= 1000000000L;
            textFps.set(String.format("FPS: %,d", frames));
            frames = 0;
        }
        renderer.render();
        frames++;
    }

    public ReadOnlyStringWrapper getTextFps() {
        return textFps;
    }

    public void setUpdater(Update updater) {
        this.updater = updater;
    }

    public void setRenderer(Render renderer) {
        this.renderer = renderer;
    }

}
