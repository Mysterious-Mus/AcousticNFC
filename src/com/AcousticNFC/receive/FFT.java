package com.AcousticNFC.receive;

public class FFT {

    // compute the FFT of x[], assuming its length is a power of 2
    public static void fft(float[] x) {
        int N = x.length;

        if (Integer.highestOneBit(N) != N || N < 1)
            throw new IllegalArgumentException("x.length must be a power of 2 and greater than 0");

        fft(x, 0, N, 1);
    }
    
    // Compute the FFT of x[start:start+n] at stride locations, assuming n is a power of 2
    private static void fft(float[] x, int start, int n, int stride) {
        if (n == 1)
            return;

        fft(x, start, n / 2, 2 * stride);
        fft(x, start + stride, n / 2, 2 * stride);

        for (int i = 0; i < n / 2; i++) {
            float even = x[start + 2 * i * stride];
            float odd = x[start + (2 * i + 1) * stride];

            if (start + n + 2 * i * stride < x.length)
                even += x[start + n + 2 * i * stride];

            if (start + n + (2 * i + 1) * stride < x.length)
                odd -= x[start + n + (2 * i + 1) * stride];

            float t = (float) -2 * i * (float)Math.PI / n;
            float exp = (float) Math.exp(t);

            x[start + 2 * i * stride] = even + exp * odd;
            x[start + (2 * i + 1) * stride] = even - exp * odd;
        }
    }
}
