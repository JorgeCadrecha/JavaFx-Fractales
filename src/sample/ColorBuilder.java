package sample;

/**
 * This class provides the static method
 * to paint with a beautiful color degraded
 *
 * Thanks to @Eriksonn to help me with the maths
 */
public class ColorBuilder {

    /**
     * The ways of render the colors
     * If you now more, you can add the code
     */
    enum WayToRender {
        SINE,
        COSINE,
        RESIDUAL,
        COSINE_SQUARE,
        SINE_SQUARE
    }

    /**
     * This method combines three integers in a hex color
     * @param r the red color
     * @param g the green color
     * @param b the blue color
     * @return hex color full alpha with the 3 channels specified on parameters
     */
    private static int buildHexColor(int r, int g, int b) {
        return 0xff << 24 | r << 16 | g << 8 | b;
    }

    /**
     * The same as above, but it takes floating point numbers
     */
    private static int buildHexColor(float r, float g, float b) {
        return buildHexColor((int)(255 * r), (int)(255 * g), (int)(255 * b));
    }

    /**
     * This method uses simple mathematical
     * operations to approach the cosine result
     * without doing it (because is high resources needed)
     * It takes the input as radians
     *
     * I assume you know how cosine works: in an angle,
     * out the base of the triangle
     *
     * Thanks to @Eriksson, this code is from him
     *
     * @param val the value of the angle
     * @return the base of the triangle (approach)
     */
    private static float cos(float val) {
        //make 3 phase-shifted triangle waves
        float v = Math.abs((val % 2) -1);
        //use cubic beizer curve to approximate the (cos+1)/2 function
        v = v * v * ( 3 - 2 * v );
        return v;
    }

    /**
     * If you think it, the cosine and the sine
     * do the same but only offset
     * So it's possible do the sin using the
     * cos method
     *
     * cos(x) = sin(x + PI/2)
     * (in degrees, 90 degrees)
     *
     * sin: in angle, out the high of the triangle
     *
     * Thank to @Eriksson to explain me this on
     * the OneLoneCoder discord chat
     *
     * @param val the value of the angle
     * @return the high of the triangle (approach)
     */
    private static float sin(float val) {
        return cos(val + ((float)Math.PI / 2.0f));
    }

    /**
     * Cosine approach
     */
    private static int buildColorCosine(int val, float added) {
        float q = val * 0.1f + added;

        float r = cos(q);
        float g = cos(q + 0.66f);
        float b = cos(q + 1.33f);

        return buildHexColor(r, g, b);
    }

    /**
     * Sine approach
     */
    private static int buildColorSine(int val, float added) {
        float q = val * 0.1f + added;

        float r = sin(q);
        float g = sin(q + 0.66f);
        float b = sin(q + 1.33f);

        return buildHexColor(r, g, b);
    }

    /**
     * Residual
     */
    private static int buildColorRes(int val) {
        float n = (float) val;
        float a = 0.1f;
        int res = 3;
        double r = 0.5f * (a * n) % res + 0.5f;
        double g = 0.5f * (a * n + 2.094f) % res + 0.5f;
        double b = 0.5f * (a * n + 4.188f) % res + 0.5f;
        return 0xff << 24 | (int)(255 * r) << 16 | (int)(255 * g) << 8 | (int)(255 * b);
    }

    /**
     * sine of val^2
     */
    private static int buildColorCosineSquare(int val, float added) {
        return buildColorCosine(val * val, added);
    }

    /**
     * cos of val^2
     */
    private static int buildColorSineSquare(int val, float added) {
        return buildColorSine(val * val, added);
    }

    public static int buildColor(int fractalValue, float added, WayToRender way) {
        switch ( way ) {
            case SINE: default:
                return buildColorSine(fractalValue, added);
            case COSINE:
                return buildColorCosine(fractalValue, added);
            case RESIDUAL:
                return buildColorRes(fractalValue);
            case COSINE_SQUARE:
                return buildColorCosineSquare(fractalValue, added);
            case SINE_SQUARE:
                return buildColorSineSquare(fractalValue, added);
        }
    }

}
