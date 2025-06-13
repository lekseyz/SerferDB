package core.search.btree.utils;

import java.util.Random;

public class RandomState {
    private final Random random;

    public RandomState(int seed) {
        random = new Random(seed);
    }

    public Random getRandom() {
        return random;
    }

    public String stringGen(int length) {
        return random.ints('a', 'z' + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public int intGen(int from, int to) {
        return random.nextInt(from, to);
    }
}
