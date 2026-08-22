package com.pankaj.mvm.util;

import java.time.Year;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class SkuGenerator {

    private SkuGenerator() {
        // Utility class - prevent direct instantiation
    }

    public static String generate(String categoryCode) {
        String cleanPrefix = (categoryCode != null && !categoryCode.isBlank())
                ? categoryCode.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT)
                : "GEN";

        if (cleanPrefix.length() > 4) {
            cleanPrefix = cleanPrefix.substring(0, 4);
        }

        int currentYear = Year.now().getValue();
        int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);

        return String.format("CAT-%s-%d-%04d", cleanPrefix, currentYear, randomSuffix);
    }
}