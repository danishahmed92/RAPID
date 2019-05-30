package utils;

import config.IniConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author DANISH AHMED
 */
public class Utils {
    public static Utils utilsInstance;
    static {
        utilsInstance = new Utils();
    }

    public Utils() {
        loadStopWords();
    }

    public Set<String> stopWords = new HashSet<>();

    /**
     *
     * @param directory directory path
     * @return all file names that are present in directory
     */
    public static List<String> getFilesInDirectory(String directory) {
        List<String> filesInDirectory = new ArrayList<>();
        Path path = Paths.get(directory);
        if (Files.isDirectory(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                        filesInDirectory.add(filePath.getFileName().toString());
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            filesInDirectory.add(path.getFileName().toString());
        }
        return filesInDirectory;
    }

    /**
     * loads stop words from file and store as set
     */
    private void loadStopWords() {
        IniConfig config = IniConfig.configInstance;
        String stopWordsPath = config.stopWords;

        try {
            BufferedReader input = new BufferedReader(new FileReader(stopWordsPath));
            String word;

            while ((word = input.readLine()) != null)
                stopWords.add(word);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param directoryPath creates folder to specified string
     */
    public static void createFolderIfNotExist(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists())
            directory.mkdir();
    }

    /**
     *
     * @param str string
     * @return replaces alpha numeric with space
     */
    public static String filterAlphaNum (String str) {
        str = str.replaceAll("[^A-Za-z0-9]", " ");
        str = str.replaceAll("^ +| +$|( )+", "$1");
        return str.toLowerCase();
    }

    /**
     *
     * @param str string
     * @return removes stop words from string and returns new string
     */
    public String removeStopWordsFromString (String str) {
        List<String> strWithoutStopWords = new ArrayList<>();
        String[] split = str.split(" ");

        for (String splitStr : split) {
            if (!stopWords.contains(splitStr))
                strWithoutStopWords.add(splitStr);
        }
        return String.join(" ", strWithoutStopWords);
    }

    /**
     *
     * @param str string
     * @return word list from provided string, after removing stop words
     */
    public List<String> getListRemovedStopWords (String str) {
        List<String> strWithoutStopWords = new ArrayList<>();
        String[] split = str.split(" ");

        for (String splitStr : split) {
            if (!stopWords.contains(splitStr))
                strWithoutStopWords.add(splitStr);
        }
        return strWithoutStopWords;
    }

    /**
     *
     * @param arr array of int/double values
     * @return sum of array
     */
    public static double sum(Object[] arr) {
        double sum = 0.0;
        for (Object i : arr)
            sum += Double.parseDouble(i.toString());
        return sum;
    }
	
	/**
     *
     * @param arr array of int/double values
     * @return mean amongst array elements
     */
    public static double mean(Object[] arr) {
        return sum(arr) / arr.length;
    }

    /**
     *
     * @param arr array of int/double values
     * @return variance amongst array elements
     */
    public static double variance(Object[] arr) {
        double mean = mean(arr);
        double temp = 0;
        for(Object i :arr)
            temp += (Double.parseDouble(i.toString()) - mean) * (Double.parseDouble(i.toString()) - mean);
        return temp / (arr.length - 1);
    }

    /**
     *
     * @param arr array of int/double values
     * @return standard deviation amongst array elements
     */
    public static double standardDeviation(Object[] arr) {
        return Math.sqrt(variance(arr));
    }
}
