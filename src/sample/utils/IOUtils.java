package sample.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * This class contains the methods to save
 * or load something
 */
public class IOUtils {

    public static void saveImage(String directory, String name, WritableImage img) {
        String fileName = directory + "\\" + name + ".png";
        File file = new File(fileName);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "PNG", file);
        } catch ( IOException e ) {
            MessageUtils.showError("Ha ocurrido algún error al intentar guardar la imagen", "Error: " + e.getMessage());
        }
        MessageUtils.showMessage("Imagen guardada", "Se ha guardado la imagen en el directorio: " + fileName);
    }

    public static void saveImage(WritableImage img, String directory, String name) {
        if (!directory.equals("") && !name.equals("")) {
            saveImage(directory, name, img);
        } else {
            MessageUtils.showError("Directorio y nombre nulos", "Introduce donde y como se va a guardar la imagen.");
        }
    }

}
