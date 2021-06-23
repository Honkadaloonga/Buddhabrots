package com.p5zf2c46j;

import com.p5zf2c46j.util.Complex;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
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
    public static void main(String[] args) throws Exception {
        System.out.println(getCurrentTimeStamp() + " : Rendering started");
        long totalTime = -System.currentTimeMillis();

        for (int i = 4; i <= 13; i++) {
            long time = -System.currentTimeMillis();

            // for max speed change the length of this array
            // to the amount of threads your processor has
            RendererThread[] threads = new RendererThread[4];
            CountDownLatch latch = new CountDownLatch(threads.length);

            for (int j = 0; j < threads.length; j++) {
                threads[j] = new RendererThread(1<<i, j, threads.length, latch);
                threads[j].start();
            }

            latch.await();

            BufferedImage cvs = new BufferedImage(RendererThread.width, RendererThread.height, BufferedImage.TYPE_INT_ARGB);
            int[] pixels = ((DataBufferInt)cvs.getRaster().getDataBuffer()).getData();
            long[] vals = new long[pixels.length];

            for (RendererThread thread : threads) {
                for (int j = 0; j < thread.vals.length; j++) {
                    vals[j] += thread.vals[j];
                }
            }

            // make an image containing just the raw number of samples in each pixel
            for (int j = 0; j < pixels.length; j++) {
                pixels[j] = (int) vals[j];
            }

            {
                String fileName = "/data/out/z + tanh(z)/v_" + (1 << i) + "_" + now().getEpochSecond() + ".png";
                File outFile = new File(Paths.get("").toAbsolutePath() + fileName);
                ImageIO.write(cvs, "png", outFile);
            }

            // then make the actual grayscale image
            double top = max(vals)+1;

            for (int j = 0; j < pixels.length; j++) {
                int br = (int) (Math.pow(vals[j]/top, 0.5) * 256);
                pixels[j] = new Color(br, br, br).getRGB();
            }

            {
                String fileName = "/data/out/z + tanh(z)/" + (1 << i) + "_" + now().getEpochSecond() + ".png";
                File outFile = new File(Paths.get("").toAbsolutePath() + fileName);
                ImageIO.write(cvs, "png", outFile);
            }
            // this is just a bunch of debug timing stuff
            time += System.currentTimeMillis();
            System.out.println(getCurrentTimeStamp()+" : Completed "+threads[0].name+" in "+formatMillis(time));
        }

        totalTime += System.currentTimeMillis();
        System.out.println(getCurrentTimeStamp() + " : Rendering took " + formatMillis(totalTime));
    }

    private static long max(long[] array) {
        long m = array[0];
        for (long a : array) {
            m = Math.max(a, m);
        }
        return m;
    }
}

class RendererThread implements Runnable {
    // Static vars
    public static final int width = 2160;
    public static final int height = 2160;
    private static final double xmin = -7;
    private static final double xmax = 3;
    private static final double ymin = -5;
    private static final double ymax = 5;

    // Thread stuff
    public Thread thread = null;
    public final String name;

    // Input vars
    private final CountDownLatch latch;
    private final int maxIter;
    public final int mod;
    private final int numThreads;

    // Output vars
    public final int[] vals;

    public RendererThread(int maxIter, int mod, int numThreads, CountDownLatch latch) {
        this.maxIter = maxIter;
        this.mod = mod;
        this.numThreads = numThreads;
        this.latch = latch;
        this.vals = new int[width*height];
        this.name = "Renderer ["+maxIter+", "+mod+"]";
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, name);
            thread.start();
        }
    }

    @Override
    public void run() {
        long index = 0;
        // this value determines how grainy/noisy the final image is (lower = slower but less noise)
        double delta = 0.125;
        // the random is here because when I started x and y at 0 there were a bunch of weird lines in the end result
        Random r = new Random();
        for (double x = -r.nextDouble() * delta; x < width; x += delta) {
            for (double y = -r.nextDouble() * delta; y < height; y += delta) {
                // a crude way of making the thread only calculate what it needs to
                if (index++ % numThreads != mod) {
                    continue;
                }

                Complex c;
                {
                    double a = map(x, 0, width, xmin, xmax);
                    double b = map(y, 0, height, ymax, ymin);
                    c = new Complex(a, b);
                }
                Complex z = c.copy();

                // so to make the buddhabrot we need to store all the indexes we're supposed to add to
                // but only add to them once we know the value of z tends to infinity
                ArrayList<Integer> nums = new ArrayList<>();

                // we make an array of a few previous values
                // comparing z to it later speeds up a few fractals
                Complex[] p = new Complex[4];
                Arrays.setAll(p, i -> new Complex());
                Complex n = new Complex();

                iter:
                for (int k = 0; k < maxIter; k++) {

                    // this is the main formula
                    // z = sqr((tanh(z) + z)*0.5) + #pixel
                    n.set(sqr(tanh(z).add(z).mult(0.5)).add(c));

                    // if z is close enough to one of the values in p we have reached a fixed point or a period with the max
                    // length of p.length which means z will never escape the bail condition which we mark by adding -1 to nums
                    for (Complex o : p) {
                        if (magSqr(sub(n, o)) < 1E-20) {
                            nums.add(-1);
                            break iter;
                        }
                    }

                    z.set(n);
                    p[k%p.length].set(n);

                    // checking if z tends to infinity would be too slow so we just check if its distance from the origin is
                    // greater than some arbitrary value (might have to change this when using different fractals)
                    if (magSqr(z) > 1024) {
                        break;
                    }

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
        }

        vals[0] = vals[1];

        latch.countDown();
    }
}