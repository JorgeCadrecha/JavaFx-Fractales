package sample;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import points2d.Vec2df;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This class contains the static methods
 * what do the math for the fractal
 */
public class FractalMath {

    enum FractalMethod {
        NAIVE,
        POOL_THREAD
    }

    private static ScheduledService<Boolean> scheduledService;

    private static ThreadPoolExecutor executor;

    private static double[] squareComplexNumber(double real, double complex) {
        double real2 = real * real - complex * complex;
        double comp2 = 2 * real * complex;
        return new double[] { real2, comp2 };
    }

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
            for ( int i = 0; i < 1; i++ ) {
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

    public static int naiveCal(float x, float y, double xScale, double yScale, float fTop, float fLeft, int iterations) {
        double realC = x * xScale + fTop;
        double compC = y * yScale + fLeft;
        return mathCal(realC, compC, iterations);
    }

    public static void createFractalBasic(
            Vec2df pixelsTopLeft,
            Vec2df pixelsBottomRight,
            Vec2df fractalTopLeft,
            Vec2df fractalBottomRight,
            int iterations,
            int[] fractal,
            int imgWidth
    ) {
        double xScale = (fractalBottomRight.getX() - fractalTopLeft.getX()) / (double)(pixelsBottomRight.getX() - pixelsTopLeft.getX());
        double yScale = (fractalBottomRight.getY() - fractalTopLeft.getY()) / (double)(pixelsBottomRight.getY() - pixelsTopLeft.getY());

        for ( float y = pixelsTopLeft.getY(); y < pixelsBottomRight.getY(); y++ ) {
            for ( float x = pixelsTopLeft.getX(); x < pixelsBottomRight.getX(); x++ ) {
                fractal[(int)y * imgWidth + (int)x] = FractalMath.naiveCal(x, y, xScale, yScale, fractalTopLeft.getX(), fractalTopLeft.getY(), iterations);
            }
        }
    }

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

    public static void createFractalThreads(
            Vec2df pixelsTopLeft,
            Vec2df pixelsBottomRight,
            Vec2df fractalTopLeft,
            Vec2df fractalBottomRight,
            int iterations,
            int[] fractal,
            int imgWidth
    ) {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int numThreads = executor.getMaximumPoolSize() - 1;
        int sectionWidth = (int) ((pixelsBottomRight.getX() - pixelsTopLeft.getX()) / numThreads);
        float fractalWidth = (fractalBottomRight.getX() - fractalTopLeft.getX()) / (float)numThreads;
        for (int i = 0; i < numThreads; i++ ) {
            Vec2df pTopLeft, pBottomRight, fTopLeft, fBottomRight;
            pTopLeft = new Vec2df(pixelsTopLeft.getX() + sectionWidth * (i), pixelsTopLeft.getY());
            pBottomRight = new Vec2df(pixelsTopLeft.getX() + sectionWidth * (i + 1), pixelsBottomRight.getY());
            fTopLeft = new Vec2df(fractalTopLeft.getX(), fractalTopLeft.getY()); // fractalTopLeft.getX() + fractalWidth * (i)
            fBottomRight = new Vec2df(fractalTopLeft.getX() + fractalWidth, fractalBottomRight.getY()); // fractalTopLeft.getX() + fractalWidth * (i + 1)
            executor.execute(() -> {
                createFractalBasic(pTopLeft, pBottomRight, fTopLeft, fBottomRight, iterations, fractal, imgWidth);
            });
        }
        executor.shutdown();
        scheduledService.restart();
    }

}
