package com.p5zf2c46j;

import com.p5zf2c46j.util.Complex;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static com.p5zf2c46j.util.Complex.*;
import static com.p5zf2c46j.util.P3Utils.*;
import static java.time.Instant.now;

public class BuddhaThreaded {
    public static final int numThreads = 4;

    public static void main(String[] args) throws Exception {
        System.out.println(getCurrentTimeStamp() + " : Rendering started");
        long totalTime = -System.currentTimeMillis();

        for (int i = 4; i <= 13; i++) {
            long time = -System.currentTimeMillis();

            RendererThread[] threads = new RendererThread[numThreads];
            CountDownLatch latch = new CountDownLatch(numThreads);

            for (int j = 0; j < numThreads; j++) {
                threads[j] = new RendererThread(1<<i, j, latch);
                threads[j].start();
            }

            latch.await();

            time += System.currentTimeMillis();
            System.out.println("\n"+getCurrentTimeStamp()+" : Completed "+threads[0].name+" in "+formatMillis(time));

            BufferedImage cvs = new BufferedImage(RendererThread.width, RendererThread.height, BufferedImage.TYPE_USHORT_GRAY);
            short[] pixels = ((DataBufferUShort)cvs.getRaster().getDataBuffer()).getData();
            int[] vals = new int[pixels.length];

            for (RendererThread thread : threads) {
                for (int j = 0; j < thread.vals.length; j++) {
                    vals[j] += thread.vals[j];
                }
            }

            double bot = min(vals);
            double top = max(vals)+1;

            for (int j = 0; j < pixels.length; j++) {
                double m = map(vals[j], bot, top, 0, 1);
                pixels[j] = (short) (Math.pow(m, 0.5) * 65536);
            }

            String fileName = "/data/out/mceltic/" + (1<<i) + "_" + now().getEpochSecond() + ".png";
            File outFile = new File(Paths.get("").toAbsolutePath() + fileName);
            ImageIO.write(cvs, "png", outFile);
        }

        totalTime += System.currentTimeMillis();
        System.out.println(getCurrentTimeStamp() + " : Rendering took " + formatMillis(totalTime));
    }

    private static int max(int[] array) {
        int m = array[0];
        for (int a : array) {
            m = Math.max(a, m);
        }
        return m;
    }

    private static int min(int[] array) {
        int m = array[0];
        for (int a : array) {
            m = Math.min(a, m);
        }
        return m;
    }
}

class RendererThread implements Runnable {
    // 8:5 - 2732 x 1708
    // 2:1 - 3040 x 1520

    // Static vars
    public static final int width = 2160;
    public static final int height = 2160;
    private static final double xcenter = -0.125;
    private static final double ycenter = 0;
    private static final double magn = 1;

    // Thread stuff
    public Thread thread = null;
    public final String name;

    // Inputs
    private final CountDownLatch latch;
    private final int maxIter;
    public final int index;

    // Outputs
    public final int[] vals;

    @Override
    public void run() {
        double xmin, xmax, ymin, ymax;
        {
            double xreach = 2.0/magn;
            double yreach = (2.0*height)/(magn*width);
            xmin = xcenter - xreach;
            xmax = xcenter + xreach;
            ymin = ycenter - yreach;
            ymax = ycenter + yreach;
        }
        int prev = -1;

        // this value determines how noisy the final image is (lower = slower but less noise)
        final double delta = 0.05;
        // the random is here because when I started x and y at 0 there was a bunch of weird lines in the end result
        Random r = new Random(index + 2137);
        for (double x = -r.nextDouble() * delta; x < width; x += delta) {
            for (double y = -r.nextDouble() * delta; y < height; y += delta) {
                // so to make the buddhabrot we need to store all the indexes we're supposed to add to
                // but only add to them once we know the value of z tends to infinity
                ArrayList<Integer> nums = new ArrayList<>();

                // we make an array of a few previous values
                // comparing z to it later speeds up a few fractals
                Complex[] p = new Complex[3];
                Arrays.setAll(p, i -> new Complex(Double.NaN, Double.NaN));

                Complex pixel;
                {
                    double a = map(x, 0, width, xmin, xmax);
                    double b = map(y, 0, height, ymax, ymin);
                    pixel = new Complex(a, b);
                }

                Complex z = new Complex();
                Complex n = new Complex();

                iter:
                for (int k = 0; k < maxIter; k++) {

                    // this is the main formula
                    n.set(sqr(z));
                    n.x = Math.abs(n.x);
                    n.add(pixel);

                    // checking if z tends to infinity would be too slow so we just check if its distance from the origin is
                    // greater than some arbitrary value (might have to change this when using different fractals)
                    if (magSqr(n) > 256) {
                        break;
                    }

                    // if z is close enough to one of the values in p we have reached a fixed point or a period with the max
                    // length of p.length which means z will never escape the bail condition which we mark by adding -1 to nums
                    for (Complex o : p) {
                        if (magSqr(sub(n, o)) < 1E-30) {
                            nums.add(-1);
                            break iter;
                        }
                    }

                    z.set(n);
                    p[k % p.length].set(n);

                    // if the pixel is off screen we can mark it as such and continue
                    if (z.x < xmin || z.x >= xmax || z.y < ymin || z.y >= ymax) {
                        nums.add(-2);
                        continue;
                    }

                    int indX = (int) Math.floor(map(z.x, xmin, xmax, 0, width));
                    int indY = (int) Math.floor(map(z.y, ymax, ymin, 0, height));
                    nums.add(indX + width * indY);
                }

                // if the number of indexes equals the max iterations we know the value of z never escaped the bail condition
                if (nums.size() == maxIter) {
                    continue;
                }

                // making sure we dont get an ArrayIndexOutOfBoundsException
                if (nums.size() > 0) {
                    // if we found a period we also know the value of z will never escape the bail condition
                    if (nums.get(nums.size()-1) == -1) {
                        continue;
                    }
                }

                // we increment the sample count of the pixel at each of the indexes in vals
                for (int num : nums) {
                    // except for the ones previously marked as off screen
                    if (num != -2) {
                        vals[num]++;
                    }
                }

            }

            int cur = (int) map(x, 0, width, 0, 56);
            if (cur != prev) {
                prev = cur;
                System.out.print("#");
            }
        }

        latch.countDown();
    }

    public RendererThread(int maxIter, int index, CountDownLatch latch) {
        this.maxIter = maxIter;
        this.index = index;
        this.latch = latch;
        this.vals = new int[width*height];
        this.name = "Renderer ["+maxIter+", "+index+"]";
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, name);
            thread.start();
        }
    }
}