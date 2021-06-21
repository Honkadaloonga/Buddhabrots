package com.p5zf2c46j;

import com.p5zf2c46j.util.Complex;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static com.p5zf2c46j.util.Complex.*;
import static com.p5zf2c46j.util.P3Utils.*;

public class BuddhaThreaded {
    public static void main(String[] args) throws Exception {
        System.out.println(getCurrentTimeStamp() + " : Rendering started");
        long totalTime = -System.currentTimeMillis();

        for (int i = 4; i < 14; i++) {
            long time = -System.currentTimeMillis();
            RendererThread[] threads = new RendererThread[4]; // preferably change this 4 to the amount of threads your processor has
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

            String fileName = "/data/out/triple newton 2/v_"+(1<<i)+"_"+java.time.Instant.now().getEpochSecond()+".png";
            File outFile = new File(Paths.get("").toAbsolutePath() + fileName);
            ImageIO.write(cvs, "png", outFile);

            // then make the actual grayscale image

            double top = max(vals)+1;

            for (int j = 0; j < pixels.length; j++) {
                int br = (int) (Math.pow(vals[j]/top, 1/3D) * 256);
                pixels[j] = new Color(br, br, br).getRGB();
            }

            fileName = "/data/out/triple newton 2/"+(1<<i)+"_"+java.time.Instant.now().getEpochSecond()+".png";
            outFile = new File(Paths.get("").toAbsolutePath() + fileName);
            ImageIO.write(cvs, "png", outFile);

            // this is just a bunch of debug timing stuff
            time += System.currentTimeMillis();
            System.out.println(getCurrentTimeStamp()+" : Completed "+threads[0].name+" in "+formatMillis(time));
        }

        totalTime += System.currentTimeMillis();
        System.out.println(getCurrentTimeStamp() + " : Rendering took " + formatMillis(totalTime));
    }

    private static long max(long[] array) {
        long m = array[0];
        for (long a : array)
            m = Math.max(a, m);
        return m;
    }
}

class RendererThread implements Runnable {
    // Static vars
    public static final int width = 3456;
    public static final int height = 2160;
    private static final double xmin = -4.1;
    private static final double xmax = 5.5;
    private static final double ymin = -3;
    private static final double ymax = 3;

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
        double delta = 0.125;
        // the random is here because when I started x and y at 0 there were a bunch of weird lines in the end result
        Random r = new Random();
        for (double x = -r.nextDouble() * delta; x < width; x += delta) {
            for (double y = -r.nextDouble() * delta; y < height; y += delta) {
                // a crude way of making the thread only calculate what it needs to
                if (index++ % numThreads != mod)
                    continue;


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

                // adding this and comparing z to it later speeds up a few fractals by a lot
                Complex next = new Complex();
                for (int k = 0; k < maxIter; k++) {

                    // z = (sqr(z) + recip(z) + sqr(sqr(z))/-8)/2 + #pixel
                    next.set(sqr(z).add(recip(z)).add(sqr(sqr(z)).mult(-0.125)).mult(0.5).add(c));

                    // if z equals next we have reached a fixed point which means z will never escape the bail condition
                    // so we mark the array as incomplete with a -1 and break
                    if (z.equals(next)) {
                        nums.add(-1);
                        break;
                    }

                    z.set(next);

                    // checking if z tends to infinity would be too slow so we just check if its distance from the origin is
                    // greater than some arbitrary value
                    if (magSqr(z) > 256)
                        break;

                    int indX = (int) Math.floor(map(z.x, xmin, xmax, 0, width));
                    int indY = (int) Math.floor(map(z.y, ymax, ymin, 0, height));
                    nums.add(indX < 0 || indX >= width || indY < 0 || indY >= height ? 0 : indX + width * indY);
                }

                // if the number of indexes equals the max iterations we know the value of z never escaped the bail condition
                if (nums.size() == maxIter)
                    continue;

                if (nums.size() > 0) {
                    if (nums.get(nums.size()-1) == -1) {
                        continue;
                    }
                }

                // we increment the sample count of the pixel at each of the indexes in vals
                for (int num : nums) {
                    if (num != 0) {
                        vals[num]++;
                    }
                }

            }
        }

        vals[0] = vals[1];

        latch.countDown();
    }
}