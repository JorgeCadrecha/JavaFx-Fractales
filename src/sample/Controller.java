package sample;

import javafx.animation.AnimationTimer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import points2d.Vec2df;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private final int INCREASE_ITERATIONS = 16;

    @FXML
    private BorderPane borderPane;

    @FXML
    private Spinner<Integer> spinnerIterations;

    @FXML
    private ComboBox<String> comboBoxRendering;

    @FXML
    private ComboBox<String> comboBoxPerformance;

    @FXML
    private Button btnLessIterations;

    @FXML
    private Button btnAddIterations;

    @FXML
    private Button btnSave;

    @FXML
    private Label lblFps;

    @FXML
    private Label lblTime;

    @FXML
    private TextField txtFieldSaveDirectory;

    @FXML
    private TextField txtFieldSaveName;

    @FXML
    private ImageView imageView;

    private WritableImage img;

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

    private int paintingMode = 0;

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

        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 65536, iterations);
        spinnerIterations.setValueFactory(valueFactory);

        spinnerIterations.valueProperty().addListener((observable, oldValue, newValue) -> iterations = newValue);

        setBorderPaneEvents();
        setComboBoxesEvents();
        setImageViewEvents();

        btnAddIterations.setOnAction(event -> {
            iterations += INCREASE_ITERATIONS;
            spinnerIterations.getValueFactory().setValue(iterations);
        });
        btnLessIterations.setOnAction(event -> {
            iterations -= INCREASE_ITERATIONS;
            spinnerIterations.getValueFactory().setValue(iterations);
        });
        btnSave.setOnAction(event -> saveImage());

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

    private void setComboBoxesEvents() {
        comboBoxPerformance.getItems().add("Naive method");
        comboBoxPerformance.setOnAction(event -> mode = comboBoxPerformance.getSelectionModel().getSelectedIndex());
        comboBoxPerformance.setValue("Naive method");

        comboBoxRendering.getItems().add("sinus");
        comboBoxRendering.getItems().add("cosine");
        comboBoxRendering.getItems().add("division rest");
        comboBoxRendering.getItems().add("sinus plus cosine");
        comboBoxRendering.getItems().add("cosine approach");
        comboBoxRendering.setOnAction(event -> paintingMode = comboBoxRendering.getSelectionModel().getSelectedIndex());
        comboBoxRendering.setValue("sinus");
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

    private int buildColorSinAndCos(int fractalValue) {
        float n = (float) fractalValue;
        float a = 0.1f;
        double r = 0.5f * (Math.sin(a * n) + Math.cos(a * n)) + 0.5f;
        double g = 0.5f * (Math.sin(a * n + 2.094f) + Math.cos(a * n + 2.094f)) + 0.5f;
        double b = 0.5f * (Math.sin(a * n + 4.188f) + Math.cos(a * n + 4.188f)) + 0.5f;
        return 0xff << 24 | (int)(255 * r) << 16 | (int)(255 * g) << 8 | (int)(255 * b);
    }

    private int buildColorRes(int fractalValue) {
        float n = (float) fractalValue;
        float a = 0.1f;
        int res = 3;
        double r = 0.5f * (a * n) % res + 0.5f;
        double g = 0.5f * (a * n + 2.094f) % res + 0.5f;
        double b = 0.5f * (a * n + 4.188f) % res + 0.5f;
        return 0xff << 24 | (int)(255 * r) << 16 | (int)(255 * g) << 8 | (int)(255 * b);
    }

    private int buildColorCosineApproach(int fractalValue) {
        float q = fractalValue * 0.1f;

        //make 3 phase-shifted triangle waves
        double r = Math.abs((q % 2) -1);
        double g = Math.abs(((q + 0.66f) % 2) -1);
        double b = Math.abs(((q + 1.33f) % 2) -1);

        //use cubic beizer curve to approximate the (cos+1)/2 function
        r = r * r * ( 3 -2 * r );
        g = g * g * ( 3 -2 * g );
        b = b * b * ( 3 -2 * b );

        //combine into a color
        return 0xff << 24 | (int)(255 * r) << 16 | (int)(255 * g) << 8 | (int)(255 * b);
    }

    private int buildColorSineApproach(int fractalValue) {
        float q = fractalValue * 0.1f + ((float)Math.PI / 2.0f);

        //make 3 phase-shifted triangle waves
        double r = Math.abs((q % 2) -1);
        double g = Math.abs(((q + 0.66f) % 2) -1);
        double b = Math.abs(((q + 1.33f) % 2) -1);

        //use cubic beizer curve to approximate the (cos+1)/2 function
        r = r * r * ( 3 -2 * r );
        g = g * g * ( 3 -2 * g );
        b = b * b * ( 3 -2 * b );

        //combine into a color
        return 0xff << 24 | (int)(255 * r) << 16 | (int)(255 * g) << 8 | (int)(255 * b);
    }

    private int buildColor(int fractalValue, int way) {
        switch ( way ) {
            case 0: default: // Original del usuario @Eriksonn (Thank you @Eriksonn - Wonderful Magic Fractal Oddball Man)
                return buildColorSineApproach(fractalValue);
            case 1: // La función anterior utiliza el seno, podemos hacer lo mismo con otra función trigonometrica
                return buildColorCosineApproach(fractalValue);
            case 2: // Otra función periódica con un menor consumo de recursos es la operación de la resta de la división
                return buildColorRes(fractalValue);
            case 3: // La suma del seno y del coseno también es periodica
                return buildColorSinAndCos(fractalValue);
        }
    }

    public void render() {
        for ( int y = 1; y < (int)img.getHeight(); y++ ) {
            for ( int x = 0; x < (int)img.getWidth(); x++ ) {
                pixels[y * (int)(img.getWidth()) + x] = buildColor(fractal[y * (int)(img.getWidth()) + x], paintingMode);
            }
        }

        img.getPixelWriter().setPixels(
                0, 0,
                (int)img.getWidth(), (int)img.getHeight(),
                PixelFormat.getIntArgbInstance(),
                pixels,
                0, (int)img.getWidth());
    }

    private void saveImage() {
        String directory = txtFieldSaveDirectory.getText();
        String name = txtFieldSaveName.getText();

        if (!directory.equals("") && !name.equals("")) {
            String fileName = directory + "\\" + name + ".png";
            File file = new File(fileName);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(img, null), "PNG", file);
            } catch ( IOException e ) {
                MessageUtils.showError("Ha ocurrido algún error al intentar guardar la imagen", "Error: " + e.getMessage());
            }
            MessageUtils.showMessage("Imagen guardada", "Se ha guardado la imagen en el directorio: " + fileName);
        } else {
            MessageUtils.showError("Directorio y nombre nulos", "Introduce donde y como se va a guardar la imagen.");
        }
    }

}
