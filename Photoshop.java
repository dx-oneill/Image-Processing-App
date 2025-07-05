//worked as pair: Tejay Hall 2204744, Dexter O'Neill 2259884

import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Separator;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class Photoshop extends Application {

    private Image originalImage;
    private ImageView imageView;
    private double[] gammaLUT = new double[256];
    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("CS-256 application");

        // Create an ImageView
        imageView = new ImageView();

        // Load the image from a file
        try {
            originalImage = new Image(new FileInputStream("raytrace.jpg"));
            imageView.setImage(originalImage);
        } catch (FileNotFoundException e) {
            System.out.println(">>>The image could not be located in directory: "+System.getProperty("user.dir")+"<<<");
            System.exit(-1);
        }

        // Create a Slider for gamma correction
        Slider gammaSlider = new Slider(0.1, 3.0, 1.0);

        // Create a Label to display the current gamma value
        Label gammaLabel = new Label("Gamma: " + gammaSlider.getValue());

        // Add a listener to update the image with gamma correction when the slider is changed
        gammaSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double gammaValue = newValue.doubleValue();
            gammaLabel.setText("Gamma: " + gammaValue);
            setGammaLUT(gammaValue);

            // Apply gamma correction to the image and update the ImageView
            Image correctedImage = applyGammaCorrection(originalImage, gammaValue);
            imageView.setImage(correctedImage);
        });

        Slider resizeSlider = new Slider(0.1, 2.0, 1.0);
        Label resizeLabel = new Label("Resize: " + resizeSlider.getValue());

        CheckBox nn = new CheckBox("Nearest neighbour interpolation");
        nn.setSelected(true);

        CheckBox cc = new CheckBox("Laplacian cross-correlation");
        cc.setSelected(false);

        // Add a listener to update the image with resizing when the slider is changed
        resizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double resizeValue = newValue.doubleValue();
            resizeLabel.setText("Resize: " + resizeValue);

            Image resizedImage = resize(originalImage, resizeValue, nn.isSelected());
            imageView.setImage(resizedImage);

        });

        cc.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Image crossCorrelated = crossCorrelation(originalImage);
                imageView.setImage(crossCorrelated);
            } else {
                imageView.setImage(originalImage);
            }

        });

        Separator separator1 = new Separator();
        Separator separator2 = new Separator();
        Separator separator3 = new Separator();
        Separator separator4 = new Separator();

        // Create a VBox to hold the components
        VBox vbox = new VBox(gammaSlider, gammaLabel, separator1, resizeSlider, resizeLabel, separator2, nn, separator3, cc, separator4, imageView);

        // Create a scene with the VBox
        Scene scene = new Scene(vbox, 400, 600);

        // Set the scene to the stage
        primaryStage.setScene(scene);

        // Show the stage
        primaryStage.show();
    }

    /*
     *
     * Gamma correction function
     *
     *
     */
    private void setGammaLUT(double gamma){
        for (int i =0; i<256;i++){
            gammaLUT[i] = Math.pow((double) i/255.0, 1.0/gamma);
        }
    }
    private Image applyGammaCorrection(Image originalImage, double gamma) {
        int newWidth = (int) originalImage.getWidth();
        int newHeight = (int) originalImage.getHeight();

        // Create a new WritableImage
        javafx.scene.image.WritableImage gammaCorrectedImage = new javafx.scene.image.WritableImage(newWidth, newHeight);
        PixelWriter writableImage = gammaCorrectedImage.getPixelWriter();

        Color colour;

        for (int j=0; j<newHeight; j++)
            for (int i=0; i<newWidth; i++) {
                colour=originalImage.getPixelReader().getColor(i, j);
                colour=Color.color(gammaLUT[(int)(colour.getRed()*255.0)], gammaLUT[(int)(colour.getGreen()*255.0)], gammaLUT[(int)(colour.getBlue()*255.0)]);
                writableImage.setColor(i, j, colour);
            }

        return gammaCorrectedImage;
    }


    /*
     *
     * Interpolation functions
     *
     *
     */

    private Image resize(Image originalImage, double resizeScale, boolean nn) {
        int newWidth = (int) ((double) originalImage.getWidth()*resizeScale);
        int newHeight = (int) ((double) originalImage.getHeight()*resizeScale);

        // Create a new WritableImage
        javafx.scene.image.WritableImage resizedImage = new javafx.scene.image.WritableImage(newWidth, newHeight);
        PixelWriter writableImage = resizedImage.getPixelWriter();

        Color colour;
        //check the box is ticked
        if (nn == true) {
            for (int j = 0; j < newHeight; j++) {
                for (int i = 0; i < newWidth; i++) {
                    double x = (double) originalImage.getWidth() * (double) i / (double) newWidth;
                    double y = (double) originalImage.getHeight() * (double) j / (double) newHeight;
                    int ix = (int) x;
                    int iy = (int) y;
                    colour = originalImage.getPixelReader().getColor(ix, iy);
                    writableImage.setColor(i, j, colour);
                }
            }
            //if checkbox is not ticked then
        } else {
            double xRatio = originalImage.getWidth() / (double) newWidth;
            double yRatio = originalImage.getHeight() / (double) newHeight;

            for (int j = 0; j < newHeight; j++) {
                for (int i = 0; i < newWidth; i++) {

                    //map x and y to image coordinates
                    double x = (double) i * xRatio;
                    double y = (double) j * yRatio;
                    //call sample function and use new color for the pixel
                    Color interpolatedColor = sample(x, y);
                    writableImage.setColor(i, j, interpolatedColor);
                }
            }
        }


        return resizedImage;
    }

    private double lerp(double x1, double x2, double ratio) {
        //simple formula using two doubles and a ratio
        return x1 + (x2 - x1) * ratio;
    }

    private double bilinear(double tLeft, double tRight, double bLeft, double bRight, double dx, double dy) {
        //bilinear uses lerp() three times, two horizontally one vertically
        double interpolatedTop = lerp(tLeft, tRight, dx);
        double interpolatedBottom = lerp(bLeft, bRight, dx);
        return lerp(interpolatedTop, interpolatedBottom, dy);
    }
    private Color bilinearCol(Color tLeft, Color bLeft, Color tRight, Color bRight, double dx, double dy) {
        //separates into three color channels and interpolates the colors of the pixels
        double red = bilinear(tLeft.getRed(), tRight.getRed(), bLeft.getRed(), bRight.getRed(), dx, dy);

        double green = bilinear(tLeft.getGreen(), tRight.getGreen(), bLeft.getGreen(), bRight.getGreen(), dx, dy);

        double blue = bilinear(tLeft.getBlue(), tRight.getBlue(), bLeft.getBlue(), bRight.getBlue(), dx, dy);
        //returns a new color using the interpolated R, G, B
        Color bilinearCol = Color.color(red, green, blue);
        return bilinearCol;
    }

    private Color sample(double x, double y) {
        //height and width
        int ix = (int) x;
        int iy = (int) y;
        //stops it going out of bounds
        int ix2 = Math.min(ix + 1, (int) originalImage.getWidth() - 1);
        int iy2 = Math.min(iy + 1, (int) originalImage.getHeight() - 1);
        //reads colors from each corner
        Color tLeft = originalImage.getPixelReader().getColor(ix, iy);
        Color bLeft = originalImage.getPixelReader().getColor(ix, iy2);
        Color tRight = originalImage.getPixelReader().getColor(ix2, iy);
        Color bRight = originalImage.getPixelReader().getColor(ix2, iy2);
        //calculates dx dy
        double dx = x - ix;
        double dy = y - iy;
        Color interpolatedColor = bilinearCol(tLeft, bLeft, tRight, bRight, dx, dy);
        return interpolatedColor;
    }

    /*
     *
     *
     *
     ** cross correlation functions **
     *
     *
     *
     *
     */


    private Image crossCorrelation(Image originalImage) {
        int width = (int) originalImage.getWidth();
        int height = (int) originalImage.getHeight();

        //create laplacian filter
        int[][] laplacianFilter = {
                {-4, -1, 0, -1, -4},
                {-1, 2, 3, 2, -1},
                {0, 3, 4, 3, 0},
                {-1, 2, 3, 2, -1},
                {-4, -1, 0, -1, -4}
        };

        WritableImage resultImage = new WritableImage(width, height);
        PixelWriter writableImage = resultImage.getPixelWriter();

        double[][][] weightedSumArray = new double[height][width][3];
        double redMax = 0;
        double redMin = 1;
        double greenMax = 0;
        double greenMin = 1;
        double blueMax = 0;
        double blueMin = 1;

        //completes weighted sum intermediate image as 3d array
        //compares colours values to find max and min
        for (int y = 2; y < height - 2; y++) {
            for (int x = 2; x < width - 2; x++) {
                double[] weightedSum = weightedSum(x, y, laplacianFilter);
                weightedSumArray[y][x][0] = weightedSum[0];
                weightedSumArray[y][x][1] = weightedSum[1];
                weightedSumArray[y][x][2] = weightedSum[2];
                if (weightedSum[0] > redMax){
                    redMax = weightedSum[0];
                }
                if (weightedSum[0] < redMin){
                    redMin = weightedSum[0];
                }
                if (weightedSum[1] > greenMax){
                    greenMax = weightedSum[1];
                }
                if (weightedSum[1] < greenMin){
                    greenMin = weightedSum[1];
                }

                if (weightedSum[2] > blueMax){
                    blueMax = weightedSum[2];
                }
                if (weightedSum[2] < blueMin){
                    blueMin = weightedSum[2];
                }
            }
        }

        //normalizes each pixels colour channels
        for (int y = 2; y < height - 2; y++) {
            for (int x = 2; x < width - 2; x++) {
                double[] colorChan = {weightedSumArray[y][x][0], weightedSumArray[y][x][1], weightedSumArray[y][x][2]};
                double[] normalized = normalize(colorChan, redMax, redMin,  greenMax,  greenMin,  blueMax,  blueMin);
                Color color = Color.color(normalized[0], normalized[1], normalized[2]);
                writableImage.setColor(x, y, color);
            }
        }

        return resultImage;
    }

    //Takes in position and lapacian filter, calculates weighted sum for a position, returns array of weighted colour values
    private double[] weightedSum(int x, int y, int[][] laplacianFilter) {
        double redWeighted = 0, greenWeighted = 0, blueWeighted = 0;
        for (int j = -2; j <= 2; j++) {
            for (int i = -2; i <= 2; i++) {
                Color color = originalImage.getPixelReader().getColor(x + i, y + j);
                int filterValue = laplacianFilter[j + 2][i + 2];
                redWeighted += color.getRed() * filterValue;
                greenWeighted += color.getGreen() * filterValue;
                blueWeighted += color.getBlue() * filterValue;
            }
        }
        return new double[] {redWeighted, greenWeighted, blueWeighted};
    }

    //normalizes colour values to within the necessary boundaries
    private double[] normalize(double[] colorChan, double redMax, double redMin, double greenMax, double greenMin, double blueMax, double blueMin) {
        //find range for formula
        double redRange = redMax - redMin;
        double greenRange = greenMax - greenMin;
        double blueRange = blueMax - blueMin;
            return new double[]{
                    ((colorChan[0] - redMin) / redRange),
                    ((colorChan[1] - greenMin) / greenRange),
                    ((colorChan[2] - blueMin) / blueRange)
            };
    }
}
