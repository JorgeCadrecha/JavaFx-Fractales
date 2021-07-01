package sample;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import points2d.Vec2df;
import sample.utils.IOUtils;
import sample.utils.MessageUtils;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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
    private Label lblStatus;

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

    private ColorBuilder.WayToRender paintingMode = ColorBuilder.WayToRender.SINE;

    private int iterations = 64;

    private boolean isQKeyHeld = false;

    private boolean isAKeyHeld = false;

    private ScheduledService<Boolean> scheduledService;

    private ThreadPoolExecutor executor;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setScheduleService();

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

    private void setScheduleService() {
        scheduledService = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<Boolean>() {
                    @Override
                    protected Boolean call() {
                        Platform.runLater(() -> lblStatus.setText(executor.getCompletedTaskCount() + " of " + executor.getTaskCount() + " tasks finished"));
                        return executor.isTerminated();
                    }
                };
            }
        };

        scheduledService.setDelay(Duration.millis(500));
        scheduledService.setPeriod(Duration.seconds(1));
        scheduledService.setOnSucceeded(e -> {
            if (scheduledService.getValue()) {
                scheduledService.cancel();
            }
        });
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
        comboBoxPerformance.getItems().add("Thread pool");
        comboBoxPerformance.setOnAction(event -> mode = comboBoxPerformance.getSelectionModel().getSelectedIndex());
        comboBoxPerformance.setValue("Naive method");

        for ( ColorBuilder.WayToRender way : ColorBuilder.WayToRender.values() ) {
            comboBoxRendering.getItems().add(way.name());
        }
        comboBoxRendering.setOnAction(event ->
                paintingMode = ColorBuilder.WayToRender.values()[comboBoxRendering.getSelectionModel().getSelectedIndex()]);
        comboBoxRendering.setValue(paintingMode.name());
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
            case 0:
                createFractalBasic(pixelsTopLeft, pixelsBottomRight, fractalTopLeft, fractalBottomRight, iterations);
                break;
            case 1:
                createFractalThreads(pixelsTopLeft, pixelsBottomRight, fractalTopLeft, fractalBottomRight, iterations);
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
                fractal[(int)y * (int)img.getWidth() + (int)x] = FractalMath.naiveCal(x, y, xScale, yScale, fractalTopLeft.getX(), fractalTopLeft.getY(), iterations);
            }
        }
    }

    private void createFractalThreads(Vec2df pixelsTopLeft, Vec2df pixelsBottomRight, Vec2df fractalTopLeft, Vec2df fractalBottomRight, int iterations) {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int numThreads = executor.getCorePoolSize() / 2;
        int sectionWidth = (int) ((pixelsBottomRight.getX() - pixelsTopLeft.getX()) / numThreads);
        float fractalWidth = (fractalBottomRight.getX() - fractalTopLeft.getX()) / (float)numThreads;
        for (int i = 0; i < 4; i++ ) {
            Vec2df pTopLeft, pBottomRight, fTopLeft, fBottomRight;
            pTopLeft = new Vec2df(pixelsTopLeft.getX() + sectionWidth * i, pixelsTopLeft.getY());
            pBottomRight = new Vec2df(pixelsTopLeft.getX() + sectionWidth * (i + 1), pixelsBottomRight.getY());
            fTopLeft = new Vec2df(fractalTopLeft.getX() + fractalWidth * (float) (i), fractalTopLeft.getY());
            fBottomRight = new Vec2df(fractalTopLeft.getX() + fractalWidth * (float) (i + 1), fractalBottomRight.getY());
            executor.execute(()-> createFractalBasic(pTopLeft, pBottomRight, fTopLeft, fBottomRight, iterations));
        }
        executor.shutdown();
        scheduledService.restart();
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
