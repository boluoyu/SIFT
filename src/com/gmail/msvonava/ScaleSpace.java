package com.gmail.msvonava;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by msvon on 01-Sep-16.
 */
public class ScaleSpace {

    private int numOfOctaves;
    private int[] blurValues;
    private int[] blurWeights;
    private int numOfBlursPlusOriginal;

    private BufferedImage originalImage;
    private BufferedImage[][] scaleSpace; // original + blury (1 + 5)
    private BufferedImage[][] blurDiffs; //
    private BufferedImage[][] keyPoints;

    public ScaleSpace(BufferedImage f, int numOfOctaves,
                      int[] blurValues, int[] blurWeights) throws IOException {

        this.numOfOctaves = numOfOctaves;

        this.blurValues = blurValues;
        this.blurWeights = blurWeights;

        numOfBlursPlusOriginal = blurValues.length + 1; // original + n blurov
        originalImage = f;

        generateScaleSpace();
        generateBlurDiffs();
        findKeypoints();
    }

    /*
     * nulty je original, preto zaciatok od jednotky
     * [0][0] - original velkost, ziadny blur
     * [0][1] - original velkost, prvy blur
     * ...
     *[1][0] - polovicna velkost, ziadny blur
     * [2][1] - stvrtinova velkost, prvy blur
     *
     */
    private void generateScaleSpace() throws IOException {
        scaleSpace = new BufferedImage[numOfOctaves][numOfBlursPlusOriginal];
        scaleSpace[0][0] = originalImage;

        for (int i = 1; i < numOfOctaves; i++)
            scaleSpace[i][0] = shrinkByHalf(scaleSpace[i - 1][0]);

        for (int i = 0; i < numOfOctaves; i++)
            generateBlursOfOctave(scaleSpace[i]);
    }

    private void generateBlurDiffs() throws IOException {
        blurDiffs = new BufferedImage[numOfOctaves][numOfBlursPlusOriginal]; //numOfBlursPlusOriginal-1?
        for (int i = 0; i < scaleSpace.length; i++)
            blurDiffs[i] = generateLoG(scaleSpace[i]);
    }

    // Laplacian of Gaussians - vezmes oktavu (5 obrazkov v tomto pripade)
    // a odcitas ostry obrazok od prveho bluru, prvy blur od druheho bluru, a ziskas 4
    // obrazky ktore su rozdielmi blurov v ramci jednej oktavy
    private BufferedImage[] generateLoG(BufferedImage[] octave) throws IOException {
        BufferedImage[] LoGs = new BufferedImage[octave.length - 1];

        for (int i = 1; i < octave.length; i++)
            LoGs[i - 1] = substractImages(octave[i - 1], octave[i]);
        return LoGs;
    }

    // TODO: 16-Sep-16 private
    public static BufferedImage substractImages(BufferedImage image1, BufferedImage image2) throws IOException {
        BufferedImage difference = new BufferedImage(image1.getWidth(), image1.getHeight(), image1.getType());
        int color;
        for (int x = 0; x < image1.getWidth(); x++) {
            for (int y = 0; y < image1.getHeight(); y++) {
                color = Math.abs(image2.getRGB(x, y) - image1.getRGB(x, y));
                difference.setRGB(x, y, color);
            }
        }
        return difference;
    }

    private void generateBlursOfOctave(BufferedImage[] octave) {
        // prvy v oktave je vzdy bez bluru
        BufferedImage noBlur = octave[0];

        for (int i = 1; i < octave.length; i++)
            octave[i] = blur(noBlur, blurValues[i - 1], blurWeights[i - 1]);

    }

    private BufferedImage shrinkByHalf(BufferedImage source) throws IOException {
        int dstWidth = source.getWidth() / 2;
        int dstHeight = source.getHeight() / 2;
        BufferedImage originalImage = source;
        BufferedImage resizedImage = new BufferedImage(
                dstWidth
                , dstHeight
                , source.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, dstWidth, dstHeight, null);
        g.dispose();
        return resizedImage;

    }

    private BufferedImage blur(BufferedImage image, int size, double weight) {
        Kernel kernel = new Kernel(size, size, GaussianKernel.Calculate(size, weight));
        BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        image = op.filter(image, null);

        return image;
    }

    // Najde pixely s najvyssou a najnizsou hodnotou z okolia 26tich neighbours
    // 8 vo svojej scale, a 9 v scale nad a 9 scale pod
    // najvyssia a najizsia skala sa necekuje pretoze nemozes ist o skale vyssie/nizsie
    // ked sa taky bod najde, prida sa do zoznamu
    //
    /*
          ^
    O O O | S
    O O O |
    O O O | C
          |
    O O O | A
    O X O |
    O O O | L
          |
    O O O | E
    O O O |
    O O O | U
          |
          | P
    */
    private void findKeypoints() {
        // keypointy nedetekujeme na najnizsej a najvyssej vrstve
        // cize ak mam 5 scaleov, 0,1,2,3,4 - tak zistujem len na oktavach 1,2,3 (0,1,2_1,2,3_2,3,4)
        // blury idem vsetky
        // potom sa robi difference of gaussians (DOG)( mam 5 blurov a odcitavam prvy od druheho atd)
        // tym padom mi ostanu len 4 DOGy (0, 1, 2 ,3 )
        // a dalej hladam pixely s extremnymi hodnotami (najvyssi/nizsi)
        // takym sposobom ze sa pozrem z urovne 1 na uroven 0 a 2 potom z
        // 2 na 1 a 3... tot vse. zo 4och DOGov mi ostanu len 2 obrazky s keypointami.
        keyPoints = new BufferedImage[numOfOctaves - 2][numOfBlursPlusOriginal - 1];
        // prva a posledna oktava vynechana
        for (int i = 1; i < numOfOctaves - 1; i++) {
            for (int j = 0; j < numOfBlursPlusOriginal - 1; j++) {
                keyPoints[i - 1][j] = getExtremaImage(
                        blurDiffs[i - 1][j],
                        blurDiffs[i][j],
                        blurDiffs[i + 1][j]);
            }
        }
    }

    // TODO: Tato metoda trvala 880milisekund, neni to trosku vela? neviem si predstavit v realtime...
    private BufferedImage getExtremaImage(BufferedImage up, BufferedImage mid, BufferedImage down) {
        BufferedImage extremaImage = new BufferedImage(mid.getWidth(), mid.getHeight(), mid.getType());

        ArrayList<Point> neighboursUp;
        ArrayList<Point> neighboursMid;
        ArrayList<Point> neighboursDown;

        ArrayList<Integer> values = new ArrayList<>();

        int maxHeight, maxWidth, value;
        maxHeight = mid.getHeight();
        maxWidth = mid.getWidth();

        // ked je y=126 tak spravi neighboura ktory je 127 lenze sice pixlov je
        // 127, ale rataju sa od 0 do 126 cize out of bounds, toto sa deje len pri
        // neparnych rozmeroch, preto to treba spravit na parne a o jedna znizit
        // TODO: nebude to vadit potom , keby bol uplna na rohu nejaky keyPoint?

        if(!(maxHeight%2 == 0))
            maxHeight--;

        if(!(maxWidth%2 == 0))
            maxWidth--;

        //keby zacnem na suradniciach nula nula, nemozem pozriet neighbours v okoli lebo
        // by som bol mimo obrazku, ked zacnem na 1,1 , pre nizsiu oktavu sa to vydeli dvomi
        // ostane mi nula a som v riti takze zacneme na dvojke
        // a ano osi x a y su v takomto poradi, ach ja debil... :D
        for (int y = 2; y < maxHeight - 2; y++) {
            for (int x = 2; x < maxWidth - 2; x++) {
                value = mid.getRGB(x, y);

                neighboursUp = getNeighbours(x * 2, y * 2, true);
                neighboursMid = getNeighbours(x, y, false);
                neighboursDown = getNeighbours(x / 2, y / 2, true);

                try {
                    for (Point p : neighboursUp)
                        values.add(up.getRGB(p.getX(), p.getY()));

                    for (Point p : neighboursMid)
                        values.add(mid.getRGB(p.getX(), p.getY()));

                    for (Point p : neighboursDown) {
                        values.add(down.getRGB(p.getX(), p.getY()));
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Y SIZE MAX:" + (down.getHeight()-1));
                }
                if (isBiggest(values, value) || isSmallest(values, value))
                    extremaImage.setRGB(x, y, value);

                values.removeAll(values);

            }
        }

        return extremaImage;
    }

    private boolean isBiggest(ArrayList<Integer> neighbourValues, int midValue) {
        for (Integer i : neighbourValues) {
            if (i > midValue)
                return false;
        }
        return true;
    }

    private boolean isSmallest(ArrayList<Integer> neighbourValues, int midValue) {
        for (Integer i : neighbourValues) {
            if (i < midValue)
                return false;
        }
        return true;
    }

    // addSelf - pridat k okolitym bodom aj ten z ktoreho hladame?
    private ArrayList<Point> getNeighbours(int x, int y, boolean addSelf) {
        ArrayList<Point> neighbours = new ArrayList<>();
        // riadok nad
        neighbours.add(new Point(x - 1, y - 1));
        neighbours.add(new Point(x - 1, y));
        neighbours.add(new Point(x - 1, y + 1));
        // aktualny riadok
        neighbours.add(new Point(x, y - 1));
        neighbours.add(new Point(x, y + 1));
        // riadok pod
        neighbours.add(new Point(x + 1, y - 1));
        neighbours.add(new Point(x + 1, y));
        neighbours.add(new Point(x + 1, y - 1));

        if (addSelf)
            neighbours.add(new Point(x, y));

        return neighbours;
    }

    // vytvori obrazok rovnakych rozmerov no nic v nom nebude len bude rovnakeho typu
    private BufferedImage cloneImageWithoutData(BufferedImage orig) {
        return new BufferedImage(orig.getWidth(), orig.getHeight(), orig.getType());
    }

    // TODO: 16-Sep-16 fuck this
    private BufferedImage greyScaleImage(BufferedImage colorImage) {
        BufferedImage greyImage = new BufferedImage(colorImage.getWidth(), colorImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = greyImage.getGraphics();
        g.drawImage(colorImage, 0, 0, null);
        g.dispose();

        return greyImage;
    }

    public BufferedImage[][] getKeyPoints() {
        return keyPoints;
    }


    public BufferedImage[][] getScaleSpace() {
        return scaleSpace;
    }

    public BufferedImage[][] getBlurDiffs() {
        return blurDiffs;
    }
}
