package com.fayeoyaee.hw1;

import java.util.*;
import java.io.*;

/**
 * Utility for Indexer and Query
 */
public class Util {

    public static HashSet<String> stopSet = new HashSet<>();

    /**
     * construct stopset
     */
    public static void initStopSet() {
        try {
            InputStream i = Util.class.getResourceAsStream("/AP_DATA/stoplist.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(i));
            String thisLine;
            while ((thisLine = br.readLine()) != null) {
                Util.stopSet.add(thisLine);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * parse content into tokens
     */
    public static String[] contentParser(String content) {
        if (Util.stopSet.isEmpty()) {
            Util.initStopSet();
        }
        content = content.replaceAll("[^a-zA-Z0-9 ]+", "");
        String[] tokens = content.trim().toLowerCase().split(" +");
        tokens = Arrays.stream(tokens).filter(token -> !Util.stopSet.contains(token)).toArray(String[]::new);
        return tokens;
    }

    /**
     * A simple test for stopset
     */
    public static void main(String[] args) {
        String content = "   Exception \"smote   been Dost\\ adhfisdfijadsif aaa mostly 1998";
        String[] tokens = Util.contentParser(content);
        System.out.printf("Input:   \"%s\"\nActual:   ",content);
        for (String s : tokens) {
            System.out.printf("%s, ", s);
        }
        System.out.printf("\nExpected: %s\n","adhfisdfijadsif, aaa, 1998,");
  }

}
