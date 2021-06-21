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

            double top = max(vals)+1;

            for (int j = 0; j < pixels.length; j++) {
                pixels[j] = (int) vals[j];
            }

            String fileName = "/data/out/bncm/v_"+(1<<i)+"_"+java.time.Instant.now().getEpochSecond()+".png";
            File outFile = new File(Paths.get("").toAbsolutePath() + fileName);
            ImageIO.write(cvs, "png", outFile);

            for (int j = 0; j < pixels.length; j++) {
                int br = (int) (Math.pow(vals[j]/top, 1/3D) * 256);
                pixels[j] = new Color(br, br, br).getRGB();
            }

            fileName = "/data/out/bncm/"+(1<<i)+"_"+java.time.Instant.now().getEpochSecond()+".png";
            outFile = new File(Paths.get("").toAbsolutePath() + fileName);
            ImageIO.write(cvs, "png", outFile);

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
    public static final int width = 216;
    public static final int height = 216;
    private static final double xmin = -9;
    private static final double xmax = 3;
    private static final double ymin = -4;
    private static final double ymax = 8;

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
        Random r = new Random();
        for (double x = -r.nextDouble() * delta; x < width; x += delta) {
            for (double y = -r.nextDouble() * delta; y < height; y += delta) {
                if (index++ % numThreads != mod)
                    continue;

                Complex c;
                {
                    double a = map(x, 0, width, xmin, xmax);
                    double b = map(y, 0, height, ymax, ymin);
                    c = new Complex(a, b);
                }
                Complex z = c.copy();

                ArrayList<Integer> nums = new ArrayList<>();

                Complex next = new Complex();
                for (int k = 0; k < maxIter; k++) {

                    // z = z^2 + c
                    next.set(sqr(z).add(c));

                    if (z.equals(next)) {
                        nums.add(-1);
                        break;
                    }

                    z.set(next);

                    if (magSqr(z) > 256)
                        break;

                    int indX = (int) Math.floor(map(z.x, xmin, xmax, 0, width));
                    int indY = (int) Math.floor(map(z.y, ymax, ymin, 0, height));
                    nums.add(indX < 0 || indX >= width || indY < 0 || indY >= height ? 0 : indX + width * indY);
                }

                if (nums.size() == maxIter)
                    continue;

                if (nums.size() > 0) {
                    if (nums.get(nums.size()-1) == -1) {
                        continue;
                    }
                }

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