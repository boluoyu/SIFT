package com.gmail.msvonava;

import java.util.ArrayList;

/**
 * Created by msvon on 06-Sep-16.
 */
public class GaussianKernel {

    // TODO: Understand this, lets just let it do the magic.
    // https://softwarebydefault.com/2013/06/08/calculating-gaussian-kernels/
    // http://stackoverflow.com/questions/8204645/implementing-gaussian-blur-how-to-calculate-convolution-matrix-kernel
    public static float[] Calculate(int lenght, double weight) {
        float[][] kernel = new float[lenght][lenght];
        double sumTotal = 0;

        int kernelRadius = lenght / 2;
        double distance = 0;

        float calculatedEuler = (float) (1.0 /
                (2.0 * Math.PI * Math.pow(weight, 2)));

        for (int filterY = -kernelRadius;
             filterY <= kernelRadius; filterY++) {
            for (int filterX = -kernelRadius;
                 filterX <= kernelRadius; filterX++) {
                distance = ((filterX * filterX) +
                        (filterY * filterY)) /
                        (2 * (weight * weight));

                kernel[filterY + kernelRadius][filterX + kernelRadius] = (float) (calculatedEuler * Math.exp(-distance));

                sumTotal += kernel[filterY + kernelRadius]
                        [filterX + kernelRadius];
            }
        }

        for (int y = 0; y < lenght; y++) {
            for (int x = 0; x < lenght; x++) {
                kernel[y][x] = (float) (kernel[y][x] * (1.0 / sumTotal));
            }
        }
        // konverzia z 2D na 1D kvoli hlupemu java.awt.image.Kernel;
        ArrayList<Float> floatList = new ArrayList<>();

        for (int i = 0; i < kernel.length; i++)
            for (int j = 0; j < kernel[i].length; j++)
                floatList.add(kernel[i][j]);

        float[] vector = new float[floatList.size()];

        for (int i = 0; i < vector.length; i++) {
            vector[i] = floatList.get(i);
        }

        return vector;
    }
}

