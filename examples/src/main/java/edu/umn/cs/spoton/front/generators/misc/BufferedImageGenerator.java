package edu.umn.cs.spoton.front.generators.misc;


import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import java.awt.image.BufferedImage;

public class BufferedImageGenerator extends Generator<BufferedImage> {
    private static GeometricDistribution geometricDistribution = new GeometricDistribution();
    private static final int MEAN_WIDTH = 100;
    private static final int MEAN_HEIGHT = 100;


    public BufferedImageGenerator() {
        super(BufferedImage.class);
    }

    @Override
    public BufferedImage generate(SourceOfRandomness random, GenerationStatus status) {
        //image dimension
        int width = 100; //Math.max(1,geometricDistribution.sampleWithMean(MEAN_WIDTH, random));
        int height = 100; //Math.max(1, geometricDistribution.sampleWithMean(MEAN_HEIGHT, random));
        //create buffered image object img
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        //create random image pixel by pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int a = (int) (random.nextDouble() * 256); //alpha
                int r = (int) (random.nextDouble() * 256); //red
                int g = (int) (random.nextDouble() * 256); //green
                int b = (int) (random.nextDouble() * 256); //blue

                int p = (a << 24) | (r << 16) | (g << 8) | b; //pixel

                img.setRGB(x, y, p);
            }
        }
        return img;
    }
}
