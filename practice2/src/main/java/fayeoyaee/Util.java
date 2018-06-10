package fayeoyaee;

import java.util.*;
import java.io.*;

/**
 * Utility for Indexer and Query
 */
public class Util {

    public static Set<String> stopSet = new HashSet<>();
    public static Map<String, String> wordToStem = new HashMap<>();

    /**
     * construct stopset
     */
    public static void initStopSet() {
        try {
            InputStream i = Util.class.getResourceAsStream("/AP_DATA/stoplist.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(i));
            String thisLine;
            while ((thisLine = br.readLine()) != null) {
                stopSet.add(thisLine);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * construct wordToStem Map
     */
    public static void initStemMap() {
        try {
            InputStream i = Util.class.getResourceAsStream("/AP_DATA/stem-classes.lst");
            BufferedReader br = new BufferedReader(new InputStreamReader(i));
            String thisLine = br.readLine(); // skip the first line
            while ((thisLine = br.readLine()) != null) {
                String[] parts = thisLine.toLowerCase().split(" \\| +");
                String root = parts[0].trim();
                String[] words = parts[1].split(" +");
                for (String word : words) {
                    wordToStem.put(word, root);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
