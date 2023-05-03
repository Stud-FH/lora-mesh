package util;

import java.util.Random;

public class Randomized {

    private static final Random r = new Random();
    private static final char[] alphanumerics;

    public static String string(int length) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < length; i++) s.append(alphanumerics[r.nextInt(alphanumerics.length)]);
        return s.toString();
    }

    static {
        alphanumerics = new char[62];
        int i = 0;
        for (char b = '0'; b <= '9'; b++) alphanumerics[i++] = b;
        for (char b = 'a'; b <= 'z'; b++) alphanumerics[i++] = b;
        for (char b = 'A'; b <= 'Z'; b++) alphanumerics[i++] = b;
    }
}
