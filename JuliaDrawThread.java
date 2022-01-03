import java.awt.*;
import java.awt.image.BufferedImage;

public class JuliaDrawThread extends Thread {

    private final int threadNumber;
    private final int totalThreads;
    private final int imageSectionHeight;
    private final BufferedImage partialImage;
    private final JuliaSet obj;

    public void run() {
        generateImagePart();
    }

    public BufferedImage getPartialImage() {
        return this.partialImage;
    }

    private void generateImagePart() {
        float pixelBrightness;
        double complexX, complexY;
        ComplexNumber z;
        int i;

        int startCoordinate = (int) Math.floor(((float) this.obj.imageRectY / totalThreads)) * this.threadNumber;

        int endCoordinate = startCoordinate + imageSectionHeight;

        double aspectRatio = (double) this.obj.imageRectY / (double) this.obj.imageRectX;

        double mouseX = obj.mouseOffsetX / (double) obj.imageRectX / obj.zoom;
        double mouseY = obj.mouseOffsetY / (double) obj.imageRectY / obj.zoom;

        // Iterate over every pixel
        for (int x = 0; x < this.obj.imageRectX; x++) {
            for (int y = startCoordinate; y < endCoordinate; y++) {
                pixelBrightness = 0.0 f;

                complexX = (((((double) obj.imageRectX / 2) - x) / (((double) obj.imageRectX / 2) / 2)) / obj.zoom) + (mouseX * obj.zoom);
                complexY = aspectRatio * ((((y - ((double) obj.imageRectY / 2)) / (((double) obj.imageRectY / 2) / 2))) / obj.zoom) - (mouseY * obj.zoom);

                z = new ComplexNumber(complexX, complexY);

                // Loop until we've reached the maximum number of iterations
                for (i = 0; i < obj.juliaIterations; i++) {
                    z.square();
                    z.add(new ComplexNumber(obj.juliaCX, obj.juliaCY));

                    // Break out of the loop
                    if (z.modulus() > 2) {
                        // Set brightness to max
                        pixelBrightness = 1.0 f;
                        break;
                    }
                }

                // Brings the color value from e.g 0-300 to somewhere between 0-1
                float colorValue = (i % this.obj.juliaIterations) / ((float) this.obj.juliaIterations - 1);

                // Variant 1
                if (this.obj.selectedColorMethod == 1) {
                    partialImage.setRGB(x, y - startCoordinate, Color.getHSBColor(colorValue, 1, pixelBrightness).getRGB());
                }

                // Variant 2: One colour
                else if (this.obj.selectedColorMethod == 2) {
                    if (colorValue < obj.blackThresholdSliderFactor) {
                        partialImage.setRGB(x, y - startCoordinate, Color.getHSBColor((float) obj.colorHueSliderFactor / 100, 1, 0).getRGB());
                    } else {
                        partialImage.setRGB(x, y - startCoordinate, Color.getHSBColor((float) obj.colorHueSliderFactor / 100, 1, Math.min(colorValue * (float)(obj.colorBrightnessSliderFactor), 1.0 f)).getRGB());
                    }
                }
            }
        }
    }

    JuliaDrawThread(int threadNr, int totalThreads, JuliaSet obj) {
        this.threadNumber = threadNr;
        this.totalThreads = totalThreads;
        this.obj = obj;

        if (this.threadNumber != this.totalThreads - 1) {
            this.imageSectionHeight = (int) Math.floor((float) this.obj.imageRectY / this.totalThreads);
        } else {
            this.imageSectionHeight = (int) Math.floor((float) this.obj.imageRectY / this.totalThreads) + (this.obj.imageRectY % this.totalThreads) + 1;
        }

        partialImage = new BufferedImage(obj.imageRectX, this.imageSectionHeight, BufferedImage.TYPE_INT_RGB);
    }
}