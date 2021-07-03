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
import points2d.Vec2dd;
import sample.utils.IOUtils;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The controller of the javafx application
 */
public class Controller implements Initializable {

    /**
     * The number of iterations to increment or decrease
     */
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
    private Slider sliderColor;

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
    private Label lblAddedColor;

    @FXML
    public Label lblZoom;

    @FXML
    public Label lblFractalSection;

    @FXML
    private TextField txtFieldSaveDirectory;

    @FXML
    private TextField txtFieldSaveName;

    @FXML
    private ImageView imageView;

    private WritableImage img;

    private ReadOnlyStringWrapper stringDuration = new ReadOnlyStringWrapper(this, "duration", "0");

    private ReadOnlyStringWrapper stringAddedColor = new ReadOnlyStringWrapper(this, "AddedColor", "0");

    private ReadOnlyStringWrapper stringZoom = new ReadOnlyStringWrapper(this, "Zoom", "Zoom:");

    private ReadOnlyStringWrapper stringSection = new ReadOnlyStringWrapper(this, "Section", "Section: ");

    private int[] pixels;

    private int[] fractal;

    private Vec2dd mousePos;

    private Vec2dd offset = new Vec2dd(-4.0, -2.0);

    private Vec2dd startPan = new Vec2dd();

    private Vec2dd mouseWorldBeforeZoom = new Vec2dd();

    private Vec2dd mouseWorldAfterZoom = new Vec2dd();

    private double scale = 120.0f;

    private FractalMath.FractalMethod mode = FractalMath.FractalMethod.NAIVE;

    private ColorBuilder.WayToRender paintingMode = ColorBuilder.WayToRender.SINE;

    private int iterations = 64;

    private float colorAdded;

    private boolean isQKeyHeld = false;

    private boolean isAKeyHeld = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        img = new WritableImage((int)imageView.getFitWidth(), (int)imageView.getFitHeight());
        imageView.setImage(img);

        int size = (int) (img.getWidth() * img.getHeight());
        pixels = new int[size];
        fractal = new int[size];

        mousePos = new Vec2dd();

        setBorderPaneEvents();
        setImageViewEvents();
        setComboBoxesEvents();
        setSpinnerEvents();
        setSliderEvents();
        setButtonsEvents();
        setLabelBinds();

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
            mousePos.setX(event.getX());
            mousePos.setY(event.getY());
        });

        imageView.setOnMousePressed(event -> {
            startPan.setX(event.getX());
            startPan.setY(event.getY());
        });

        imageView.setOnMouseDragged(event -> {
            offset.addToX(- (event.getX() - startPan.getX() ) / scale );
            offset.addToY(- (event.getY() - startPan.getY() ) / scale );
            startPan.setX(event.getX());
            startPan.setY(event.getY());
        });

        imageView.setOnMouseReleased(event -> {
            offset.addToX(event.getX() - startPan.getX());
            offset.addToY(event.getY() - startPan.getY());
        });

        imageView.setOnScroll(event -> {
            screenToWorld(new Vec2dd(event.getX(), event.getY()), mouseWorldBeforeZoom, offset, scale);

            double deltaY = event.getDeltaY();
            if ( deltaY < 0 ) {
                scale *= 0.95;
            }
            if ( deltaY > 0 ) {
                scale *= 1.05;
            }

            screenToWorld(new Vec2dd(event.getX(), event.getY()), mouseWorldAfterZoom, offset, scale);

            offset.addToX(mouseWorldBeforeZoom.getX() - mouseWorldAfterZoom.getX());
            offset.addToY(mouseWorldBeforeZoom.getY() - mouseWorldAfterZoom.getY());
        });
    }

    private void setButtonsEvents() {
        btnAddIterations.setOnAction(event -> {
            iterations += INCREASE_ITERATIONS;
            spinnerIterations.getValueFactory().setValue(iterations);
        });
        btnLessIterations.setOnAction(event -> {
            iterations -= INCREASE_ITERATIONS;
            spinnerIterations.getValueFactory().setValue(iterations);
        });
        btnSave.setOnAction(event -> IOUtils.saveImage(img, txtFieldSaveDirectory.getText(), txtFieldSaveName.getText()));
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
        comboBoxRendering.setOnAction(event -> {
            paintingMode = ColorBuilder.WayToRender.values()[comboBoxRendering.getSelectionModel().getSelectedIndex()];
            if ( paintingMode == ColorBuilder.WayToRender.RESIDUAL ) {
                sliderColor.setDisable(true);
            } else {               sliderColor.setDisable(false);
            }
        });
    }

    private void setLabelBinds() {
        lblTime.textProperty().bind(stringDuration);
        lblAddedColor.textProperty().bind(stringAddedColor);
        lblZoom.textProperty().bind(stringZoom);
        lblFractalSection.textProperty().bind(stringSection);
    }

    private void setSpinnerEvents() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 65536, iterations);
        spinnerIterations.setValueFactory(valueFactory);
        spinnerIterations.valueProperty().addListener((observable, oldValue, newValue) -> iterations = newValue);

    }

    private void setSliderEvents() {
        sliderColor.valueProperty().addListener((ov, old_val, new_val) -> {
            colorAdded = (float)Math.PI * new_val.floatValue() / 100.0f;
            stringAddedColor.set(addedColorStringBuilder(colorAdded));
        });
        colorAdded = (float)(sliderColor.getValue());
        stringAddedColor.set(addedColorStringBuilder(colorAdded));
    }

    private double screenToWorld(double magnitude, double offset, double scale) {
        return (magnitude / scale) + offset;
    }

    private void screenToWorld(Vec2dd in, Vec2dd out, Vec2dd offset, double scale) {
        out.setX(screenToWorld(in.getX(), offset.getX(), scale));
        out.setY(screenToWorld(in.getY(), offset.getY(), scale));
    }

    /**
     * Update method, called each frame
     * I does the math
     */
    public void update() {
        if ( isQKeyHeld ) {
            screenToWorld(mousePos, mouseWorldBeforeZoom, offset, scale);
            scale *= 1.05;
            screenToWorld(mousePos, mouseWorldAfterZoom, offset, scale);
            offset.addToX(mouseWorldBeforeZoom.getX() - mouseWorldAfterZoom.getX());
            offset.addToY(mouseWorldBeforeZoom.getY() - mouseWorldAfterZoom.getY());
        }

        if ( isAKeyHeld ) {
            screenToWorld(mousePos, mouseWorldBeforeZoom, offset, scale);
            scale *= 0.95;
            screenToWorld(mousePos, mouseWorldAfterZoom, offset, scale);
            offset.addToX(mouseWorldBeforeZoom.getX() - mouseWorldAfterZoom.getX());
            offset.addToY(mouseWorldBeforeZoom.getY() - mouseWorldAfterZoom.getY());
        }

        Vec2dd pixelsTopLeft = new Vec2dd(0.0f, 0.0f);
        Vec2dd pixelsBottomRight = new Vec2dd(img.getWidth(), img.getHeight());
        Vec2dd fractalTopLeft = new Vec2dd(-2.0f, -1.0f);
        Vec2dd fractalBottomRight = new Vec2dd(1.0f, 1.0f);

        screenToWorld(pixelsTopLeft, fractalTopLeft, offset, scale);
        screenToWorld(pixelsBottomRight, fractalBottomRight, offset, scale);

        long startTime = System.nanoTime();

        FractalMath.buildFractal(
                pixelsTopLeft,
                pixelsBottomRight,
                fractalTopLeft,
                fractalBottomRight,
                iterations,
                fractal,
                (int)img.getWidth(),
                mode
        );

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        stringDuration.set(String.format("time taken:\n%.6fs", (duration / 1000000000.0f)));
        stringZoom.set(String.format("Zoom:\n%f", scale));
        stringSection.set(String.format("Sección:\n%f arriba\n%f izquierda\n%f abajo\n%f derecha",
                fractalTopLeft.getX(), fractalTopLeft.getY(), fractalBottomRight.getX(), fractalBottomRight.getY()));
    }

    /**
     * Render method, goes after the update method
     * and renders the screen
     */
    public void render() {
        for ( int i = 0; i < pixels.length; i++ ) {
            pixels[i] = ColorBuilder.buildColor(fractal[i], colorAdded, paintingMode);
        }

        img.getPixelWriter().setPixels(
                0, 0,
                (int)img.getWidth(), (int)img.getHeight(),
                PixelFormat.getIntArgbInstance(),
                pixels,
                0, (int)img.getWidth());
    }

    private String addedColorStringBuilder(float added) {
        return String.format("%.2f rad || %.2f grados", added, added * 180.0f / (float)Math.PI);
    }

}
