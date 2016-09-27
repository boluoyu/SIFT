package com.gmail.msvonava;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by msvon on 01-Sep-16.
 */
public class Main {

    public static final int NUM_OCTAVES = 4;
    public static final int[] BLUR_KERNEL_SIZES = {3, 5, 7, 9, 11};
    public static final int[] BLUR_KERNEL_WEIGHTS = {7, 11, 15, 19, 23};

    public static void main(String[] args) {

        BufferedImage image = null;

        try {

            image = ImageIO.read(new File("resources/input.bmp"));

            long start = System.currentTimeMillis();
            ScaleSpace sspace = new ScaleSpace(image, NUM_OCTAVES, BLUR_KERNEL_SIZES, BLUR_KERNEL_WEIGHTS);
            System.out.println(System.currentTimeMillis() - start);
            BufferedImage[][] keyPoints = sspace.getKeyPoints();

            for(int i =0;i<keyPoints.length; i++)
                for(int j =0;j<keyPoints[i].length; j++)
                    ImageIO.write(
                            keyPoints[i][j],
                            "bmp",
                            new File("resources/output/keypoints/out_key_" + i + "_" +j +".bmp")
                    );

//            BufferedImage up, down, mid;
//            up = ImageIO.read(new File("resources/output/diffs/out_diff_0_0.bmp"));
//            mid = ImageIO.read(new File("resources/output/diffs/out_diff_1_0.bmp"));
//            down = ImageIO.read(new File("resources/output/diffs/out_diff_2_0.bmp"));
//
//
//            BufferedImage keypointImage = sspace.getExtremaImage(up, mid, down);
//
//            ImageIO.write(keypointImage, "bmp", new File("keyPoints.bmp"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
