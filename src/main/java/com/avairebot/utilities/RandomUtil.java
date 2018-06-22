package com.avairebot.utilities;

import java.security.SecureRandom;

public class RandomUtil {

    /**
     * The globally used random instance that
     * is used for all randomized... things.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Returns true or false randomly.
     *
     * @return Either true or false.
     */
    public static boolean getBoolean() {
        return RANDOM.nextBoolean();
    }

    /**
     * Get a random integer within the bounds given, so the
     * integer returned will be a random integer between
     * 0 and the given bound integer - 1.
     *
     * @param bound The max bound for the random integer.
     * @return The random integer.
     */
    public static int getInteger(int bound) {
        return RANDOM.nextInt(bound);
    }
}
