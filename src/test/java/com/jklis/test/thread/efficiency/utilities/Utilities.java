package com.jklis.test.thread.efficiency.utilities;

import java.io.IOException;
import java.util.Properties;

public final class Utilities {

    private static final String URL_PR = "url";
    private static final String ATTEMPTS_PR = "attempts";
    private static final String NUMBER_OF_THREADS_PR = "numberOfThreads";

    public static String getURL() {
        return getProperty(URL_PR);
    }

    public static int getAttempts() {
        return Integer.parseInt(getProperty(ATTEMPTS_PR));
    }

    public static int getThreadOption(int option) {
        return Integer.parseInt(getProperty(NUMBER_OF_THREADS_PR)
                .split(",")[option]);
    }

    private static String getProperty(String property) {
        Properties prop = new Properties();
        try {
            prop.load(Utilities.class
                    .getClassLoader()
                    .getResourceAsStream("application.properties"));
            return prop.getProperty(property);
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }


}
