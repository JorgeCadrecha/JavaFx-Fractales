package sample;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import points2d.Vec2df;
import sample.utils.IOUtils;
import sample.utils.MessageUtils;

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

    private FractalMath.FractalMethod mode = FractalMath.FractalMethod.NAIVE;

    private ColorBuilder.WayToRender paintingMode = ColorBuilder.WayToRender.SINE;

    private int iterations = 64;

    private boolean isQKeyHeld = false;

    private boolean isAKeyHeld = false;

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

        CustomTimer t = new CustomTimer();
        t.setUpdater(this::update);
        t.setRenderer(this::render);
        lblFps.textProperty().bind(t.getTextFps());
        t.start();

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
        for ( FractalMath.FractalMethod method : FractalMath.FractalMethod.values() ) {
            comboBoxPerformance.getItems().add(method.name().toLowerCase());
        }
        comboBoxPerformance.setValue(mode.name().toLowerCase());
        comboBoxPerformance.setOnAction(event ->
                mode = FractalMath.FractalMethod.values()[comboBoxPerformance.getSelectionModel().getSelectedIndex()]);

        for ( ColorBuilder.WayToRender way : ColorBuilder.WayToRender.values() ) {
            comboBoxRendering.getItems().add(way.name().toLowerCase());
        }
        comboBoxRendering.setValue(paintingMode.name().toLowerCase());
        comboBoxRendering.setOnAction(event ->
                paintingMode = ColorBuilder.WayToRender.values()[comboBoxRendering.getSelectionModel().getSelectedIndex()]);
    }

    private float screenToWorld(float magnitude, float offset, float scale) {
        return (magnitude / scale) + offset;
    }

    private void screenToWorld(Vec2df in, Vec2df out, Vec2df offset, float scale) {
        out.setX(screenToWorld(in.getX(), offset.getX(), scale));
        out.setY(screenToWorld(in.getY(), offset.getY(), scale));
    }

    public void update() {
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
            case NAIVE:
                FractalMath.createFractalBasic(
                        pixelsTopLeft,
                        pixelsBottomRight,
                        fractalTopLeft,
                        fractalBottomRight,
                        iterations,
                        fractal,
                        (int)img.getWidth()
                );
                break;
            case POOL_THREAD:
                FractalMath.setScheduleService();
                FractalMath.createFractalThreads(
                        pixelsTopLeft,
                        pixelsBottomRight,
                        fractalTopLeft,
                        fractalBottomRight,
                        iterations,
                        fractal,
                        (int)img.getWidth()
                );
                break;
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        stringDuration.set(String.format("time taken:\n%.6fs", (duration / 1000000000.0f)));
    }

    public void render() {
        for ( int i = 0; i < pixels.length; i++ ) {
            pixels[i] = ColorBuilder.buildColor(fractal[i], paintingMode);
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
            IOUtils.saveImage(directory, name, img);
        } else {
            MessageUtils.showError("Directorio y nombre nulos", "Introduce donde y como se va a guardar la imagen.");
        }
    }

}
