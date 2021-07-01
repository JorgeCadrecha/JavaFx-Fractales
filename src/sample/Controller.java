package sample;

import javafx.animation.AnimationTimer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import points2d.Vec2df;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private final int INCREASE_ITERATIONS = 16;

    @FXML
    private BorderPane borderPane;

    @FXML
    private Button btnLessIterations;

    @FXML
    private Button btnAddIterations;

    @FXML
    private ImageView imageView;

    @FXML
    private Label lblFps;

    @FXML
    private Label lblTime;

    @FXML
    private Label lblIterations;

    private WritableImage img;

    private ReadOnlyStringWrapper stringIterations = new ReadOnlyStringWrapper(this, "iterations", "0");

    private ReadOnlyStringWrapper stringDuration = new ReadOnlyStringWrapper(this, "duration", "0");

    private int[] pixels;

    private int[] fractal;

    private Vec2df mousePos;

    private Vec2df offset = new Vec2df(-4.0f, -2.0f);

    private Vec2df startPan = new Vec2df();

    private Vec2df mouseWorldBeforeZoom = new Vec2df();

    private Vec2df mouseWorldAfterZoom = new Vec2df();

    private float scale = 120.0f;

    private int mode = 0;

    private int iterations = 64;

    private boolean isQKeyHeld = false;

    private boolean isAKeyHeld = false;

    private AnimationTimer timer = new AnimationTimer() {

        protected ReadOnlyStringWrapper textFps = new ReadOnlyStringWrapper(this,
                "fpsText", "Frame count: 0 Average frame interval: N/A");

        protected long firstTime = 0;

        protected long lastTime = 0;

        protected long accumulatedTime = 0;

        protected int frames = 0;

        @Override
        public void handle(long now) {
            if ( lastTime > 0 ) {
                long elapsedTime = now - lastTime;
                accumulatedTime += elapsedTime;
                update(elapsedTime / 1000000000.0f);
            } else {
                firstTime = now;
            }
            lastTime = now;

            if ( accumulatedTime >= 1000000000L ) {
                accumulatedTime -= 1000000000L;
                textFps.set(String.format("FPS: %,d", frames));
                frames = 0;
            }
            render();
            frames++;

            lblFps.textProperty().bind(textFps);
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        img = new WritableImage((int)imageView.getFitWidth(), (int)imageView.getFitHeight());
        imageView.setImage(img);

        int size = (int) (img.getWidth() * img.getHeight());
        pixels = new int[size];
        fractal = new int[size];

        mousePos = new Vec2df();

        setBorderPaneEvents();
        setImageViewEvents();

        btnAddIterations.setOnAction(event -> iterations += INCREASE_ITERATIONS);
        btnLessIterations.setOnAction(event -> iterations -= INCREASE_ITERATIONS);

        lblIterations.textProperty().bind(stringIterations);
        lblTime.textProperty().bind(stringDuration);

        timer.start();
    }

    private void setBorderPaneEvents() {
        borderPane.setOnKeyPressed((key)->{
            if ( key.getCode() == KeyCode.Q && !isQKeyHeld ) {
                isQKeyHeld = true;
            }
            if ( key.getCode() == KeyCode.A && !isAKeyHeld ) {
                isAKeyHeld = true;
            }
        });

        borderPane.setOnKeyReleased((key)->{
            if ( key.getCode() == KeyCode.Q && isQKeyHeld ) {
                isQKeyHeld = false;
            }
            if ( key.getCode() == KeyCode.A && isAKeyHeld ) {
                isAKeyHeld = false;
            }
        });
    }

    private void setImageViewEvents() {
        imageView.setOnMouseMoved(event -> {
            mousePos.setX((float)event.getX());
            mousePos.setY((float)event.getY());
        });

        imageView.setOnMousePressed(event -> {
            startPan.setX((float)event.getX());
            startPan.setY((float)event.getY());
        });

        imageView.setOnMouseDragged(event -> {
            offset.addToX(- ( (float)event.getX() - startPan.getX() ) / scale );
            offset.addToY(- ( (float)event.getY() - startPan.getY() ) / scale );
            startPan.setX((float)event.getX());
            startPan.setY((float)event.getY());
        });

        imageView.setOnMouseReleased(event -> {
            offset.addToX((float)event.getX() - startPan.getX());
            offset.addToY((float)event.getY() - startPan.getY());
        });

        imageView.setOnScroll(event -> {
            screenToWorld(new Vec2df((float)event.getX(), (float)event.getY()), mouseWorldBeforeZoom, offset, scale);

            double deltaY = event.getDeltaY();
            if ( deltaY < 0 ) {
                scale *= 0.95;
            }
            if ( deltaY > 0 ) {
                scale *= 1.05;
            }

            screenToWorld(new Vec2df((float)event.getX(), (float)event.getY()), mouseWorldAfterZoom, offset, scale);

            offset.addToX(mouseWorldBeforeZoom.getX() - mouseWorldAfterZoom.getX());
            offset.addToY(mouseWorldBeforeZoom.getY() - mouseWorldAfterZoom.getY());
        });
    }

    private float screenToWorld(float magnitude, float offset, float scale) {
        return (magnitude / scale) + offset;
    }

    private Vec2df screenToWorld(Vec2df in, Vec2df out, Vec2df offset, float scale) {
        out.setX(screenToWorld(in.getX(), offset.getX(), scale));
        out.setY(screenToWorld(in.getY(), offset.getY(), scale));
        return out;
    }

    public void update(float elapsedTime) {
        if ( isQKeyHeld ) {
            screenToWorld(mousePos, mouseWorldBeforeZoom, offset, scale);
            scale *= 1.05f;
            screenToWorld(mousePos, mouseWorldAfterZoom, offset, scale);
            offset.addToX(mouseWorldBeforeZoom.getX() - mouseWorldAfterZoom.getX());
            offset.addToY(mouseWorldBeforeZoom.getY() - mouseWorldAfterZoom.getY());
        }

        if ( isAKeyHeld ) {
            screenToWorld(mousePos, mouseWorldBeforeZoom, offset, scale);
            scale *= 0.95f;
            screenToWorld(mousePos, mouseWorldAfterZoom, offset, scale);
            offset.addToX(mouseWorldBeforeZoom.getX() - mouseWorldAfterZoom.getX());
            offset.addToY(mouseWorldBeforeZoom.getY() - mouseWorldAfterZoom.getY());
        }

        Vec2df pixelsTopLeft = new Vec2df(0.0f, 0.0f);
        Vec2df pixelsBottomRight = new Vec2df((float)img.getWidth(), (float)img.getHeight());
        Vec2df fractalTopLeft = new Vec2df(-2.0f, -1.0f);
        Vec2df fractalBottomRight = new Vec2df(1.0f, 1.0f);

        screenToWorld(pixelsTopLeft, fractalTopLeft, offset, scale);
        screenToWorld(pixelsBottomRight, fractalBottomRight, offset, scale);

        long startTime = System.nanoTime();

        switch ( mode ) {
            case 0:
                createFractalBasic(pixelsTopLeft, pixelsBottomRight, fractalTopLeft, fractalBottomRight, iterations);
                break;
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        stringIterations.set("iterations: " + iterations);
        stringDuration.set(String.format("time taken:\n%.6fs", (duration / 1000000000.0f)));
    }

    private void createFractalBasic(Vec2df pixelsTopLeft, Vec2df pixelsBottomRight, Vec2df fractalTopLeft, Vec2df fractalBottomRight, int iterations) {
        double xScale = (fractalBottomRight.getX() - fractalTopLeft.getX()) / (double)(pixelsBottomRight.getX() - pixelsTopLeft.getX());
        double yScale = (fractalBottomRight.getY() - fractalTopLeft.getY()) / (double)(pixelsBottomRight.getY() - pixelsTopLeft.getY());

        for ( float y = pixelsTopLeft.getY(); y < pixelsBottomRight.getY(); y++ ) {
            for ( float x = pixelsTopLeft.getX(); x < pixelsBottomRight.getX(); x++ ) {

                double realC = x * xScale + fractalTopLeft.getX();
                double compC = y * yScale + fractalTopLeft.getY();
                double realZ = 0.0;
                double compZ = 0.0;

                double realZ2;
                double compZ2;

                double mod = 0;

                int n = 0;
                while ( mod < 4.0 && n < iterations ) {

                    // Operación: z = z * z + c
                    realZ2 = realZ * realZ - compZ * compZ;
                    compZ2 = 2 * realZ * compZ;

                    realZ = realZ2;
                    compZ = compZ2;

                    realZ += realC;
                    compZ += compC;

                    n++;
                    mod = (realZ * realZ + compZ * compZ);
                }

                int pos = (int)y * (int)img.getWidth() + (int)x;
                try {
                    fractal[pos] = n;
                } catch ( ArrayIndexOutOfBoundsException e ) {
                    System.out.println("pos: " + pos + " n: " + n);
                }
            }
        }
    }

    private int buildColor(int fractalValue) {
        float n = (float) fractalValue;
        float a = 0.1f;
        //double r = 0.5f * Math.sin(a * n) + 0.5f;          // 0.5f * sin(a * n) + 0.5f
        //double g = 0.5f * Math.sin(a * n + 2.094f) + 0.5f; // 0.5f * sin(a * n + 2.094f) + 0.5f
        //double b = 0.5f * Math.sin(a * n + 4.188f) + 0.5f; // 0.5f * sin(a * n + 4.188f) + 0.5f
        int res = 3;
        double r = 0.5f * (a * n) % res + 0.5f;
        double g = 0.5f * (a * n + 2.094f) % res + 0.5f;
        double b = 0.5f * (a * n + 4.188f) % res + 0.5f;
        return 0xff << 24 | (int)(255 * r) << 16 | (int)(255 * g) << 8 | (int)(255 * b);
    }

    public void render() {
        for ( int y = 1; y < (int)img.getHeight(); y++ ) {
            for ( int x = 0; x < (int)img.getWidth(); x++ ) {
                pixels[y * (int)(img.getWidth()) + x] = buildColor(fractal[y * (int)(img.getWidth()) + x]);
            }
        }

        img.getPixelWriter().setPixels(
                0, 0,
                (int)img.getWidth(), (int)img.getHeight(),
                PixelFormat.getIntArgbInstance(),
                pixels,
                0, (int)img.getWidth());
    }

}
