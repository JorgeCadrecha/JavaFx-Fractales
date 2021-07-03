package sample;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import points2d.Vec2dd;
import points2d.Vec2df;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This class contains the static methods
 * what do the math for the fractal
 */
public class FractalMath {

    /**
     * The times z complex number is square
     */
    private static final int N = 1;

    /**
     * The methods to do the fractal
     */
    enum FractalMethod {
        NAIVE,
        POOL_THREAD
    }

    /**
     * Needed service to work with threads. It isn't essential
     * but in the original example what I follow is used
     */
    private static ScheduledService<Boolean> scheduledService;

    /**
     * The pool of threads
     */
    private static ThreadPoolExecutor executor;

    /**
     * This is the math what is needed to calculate
     * the fractal
     *
     * It works with complex numbers, but java doesn't
     * have a complex number object. It is possible to
     * build your own complex number class but for this
     * implementation is even better work with the complex
     * number in binomial form
     *
     * https://stackoverflow.com/questions/2997053/does-java-have-a-class-for-complex-numbers
     *
     * In this post, @Abdul Fatir has implemented his
     * own complex number class in java. If you want use it,
     * go ahead
     */
    public static int mathCal(double realC, double compC, int iterations) {
        double realZ = 0.0;
        double compZ = 0.0;

        double realZ2;
        double compZ2;

        double mod2 = 0;

        int n = 0;
        while ( mod2 < 4.0 && n < iterations ) {

            // Operación: z = z * z + c
            // Por si se quiere hacer z = z^n + c
            for ( int i = 0; i < N; i++ ) {
                realZ2 = realZ * realZ - compZ * compZ;
                compZ2 = 2 * realZ * compZ;

                realZ = realZ2;
                compZ = compZ2;
            }

            realZ += realC;
            compZ += compC;

            n++;
            mod2 = (realZ * realZ + compZ * compZ);
        }

        return n;
    }

    /**
     * It is the same as before but with different inputs
     */
    public static int naiveCal(double x, double y, double xScale, double yScale, double fTop, double fLeft, int iterations) {
        double realC = x * xScale + fTop;
        double compC = y * yScale + fLeft;
        return mathCal(realC, compC, iterations);
    }

    /**
     * This is the method what builds the fractal
     * needs the screen coordinates and the fractal world
     * coordinates to build all the numbers for that section
     *
     * Also, to store the data, is needed the array of the
     * fractal and the image width to set up all the pixels
     */
    public static void createFractalBasic(
            Vec2dd pixelsTopLeft,
            Vec2dd pixelsBottomRight,
            Vec2dd fractalTopLeft,
            Vec2dd fractalBottomRight,
            int iterations,
            int[] fractal,
            int imgWidth
    ) {
        double xScale = (fractalBottomRight.getX() - fractalTopLeft.getX()) / (pixelsBottomRight.getX() - pixelsTopLeft.getX());
        double yScale = (fractalBottomRight.getY() - fractalTopLeft.getY()) / (pixelsBottomRight.getY() - pixelsTopLeft.getY());

        for ( double y = pixelsTopLeft.getY(); y < pixelsBottomRight.getY(); y++ ) {
            for ( double x = pixelsTopLeft.getX(); x < pixelsBottomRight.getX(); x++ ) {
                fractal[(int)y * imgWidth + (int)x] = FractalMath.naiveCal(x, y, xScale, yScale, fractalTopLeft.getX(), fractalTopLeft.getY(), iterations);
            }
        }
    }

    /**
     * Needed method for the service
     */
    public static void setScheduleService() {
        if ( scheduledService == null ) {
            scheduledService = new ScheduledService<Boolean>() {
                @Override
                protected Task<Boolean> createTask() {
                    return new Task<Boolean>() {
                        @Override
                        protected Boolean call() {
                            //Platform.runLater(() -> lblStatus.setText(executor.getCompletedTaskCount() + " of " + executor.getTaskCount() + " tasks finished"));
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
    }

    /**
     * The fastest method I have been able to implement
     * It has some render bugs, but it works pretty well
     */
    public static void createFractalThreads(
            Vec2dd pixelsTopLeft,
            Vec2dd pixelsBottomRight,
            Vec2dd fractalTopLeft,
            Vec2dd fractalBottomRight,
            int iterations,
            int[] fractal,
            int imgWidth
    ) {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int numThreads = executor.getMaximumPoolSize() - 1;
        int sectionWidth = (int) ((pixelsBottomRight.getX() - pixelsTopLeft.getX()) / numThreads);
        double fractalWidth = (fractalBottomRight.getX() - fractalTopLeft.getX()) / (double) numThreads;
        for (int i = 0; i < numThreads; i++ ) {
            Vec2dd pTopLeft = new Vec2dd(pixelsTopLeft.getX() + sectionWidth * (i), pixelsTopLeft.getY());
            Vec2dd pBottomRight = new Vec2dd(pixelsTopLeft.getX() + sectionWidth * (i + 1), pixelsBottomRight.getY());
            Vec2dd fTopLeft = new Vec2dd(fractalTopLeft.getX(), fractalTopLeft.getY()); // fractalTopLeft.getX() + fractalWidth * (i)
            Vec2dd fBottomRight = new Vec2dd(fractalTopLeft.getX() + fractalWidth, fractalBottomRight.getY()); // fractalTopLeft.getX() + fractalWidth * (i + 1)
            executor.execute(() -> createFractalBasic(pTopLeft, pBottomRight, fTopLeft, fBottomRight, iterations, fractal, imgWidth));
        }
        executor.shutdown();
        scheduledService.restart();
    }

    /**
     * To reduce the code, this method contains all the methods
     * to build the fractal
     */
    public static void buildFractal(
            Vec2dd pixelsTopLeft,
            Vec2dd pixelsBottomRight,
            Vec2dd fractalTopLeft,
            Vec2dd fractalBottomRight,
            int iterations,
            int[] fractal,
            int imgWidth,
            FractalMethod method
    ) {
        switch ( method ) {
            case NAIVE:
                FractalMath.createFractalBasic(
                        pixelsTopLeft,
                        pixelsBottomRight,
                        fractalTopLeft,
                        fractalBottomRight,
                        iterations,
                        fractal,
                        imgWidth
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
                        imgWidth
                );
                break;
        }
    }

}
