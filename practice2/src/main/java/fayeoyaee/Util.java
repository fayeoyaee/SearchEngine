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
                Util.stopSet.add(thisLine);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Construct wordToStem Map
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
                    Util.wordToStem.put(word, root);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * parse content into tokens for query
     */
    public static String[] queryTokenizer(String content) {
        if (Util.stopSet.isEmpty()) {
            Util.initStopSet();
        }
        if (Util.wordToStem.isEmpty()) {
            Util.initStemMap();
        }

        content = content.replaceAll("[^a-zA-Z0-9 ]+", "");
        String[] tokens = content.trim().toLowerCase().split(" +");
        tokens = Arrays.stream(tokens).filter(token -> !Util.stopSet.contains(token))
                .map(token -> Util.wordToStem.containsKey(token) ? Util.wordToStem.get(token) : token)
                .distinct()
                .toArray(String[]::new);
        return tokens;
    }

    /**
     * parse content into tokens for indexer
     */
    public static void indexerTokenizer(Indexer.Document doc, Map<Integer, String> hashToTerm, Map<Integer, String> hashToDocno,
            Map<Integer, Map<Integer, Integer>> termFreqInDoc) {
        if (Util.stopSet.isEmpty()) {
            Util.initStopSet();
        }
        if (Util.wordToStem.isEmpty()) {
            Util.initStemMap();
        }

        // match reg expr
        String content = doc.content.replaceAll("[^a-zA-Z0-9 ]+", "");

        // to lower case
        String[] terms = content.trim().toLowerCase().split(" +");

        // do stopping, stemming
        terms = Arrays.stream(terms).filter(term -> !Util.stopSet.contains(term))
                .map(term -> Util.wordToStem.containsKey(term) ? Util.wordToStem.get(term) : term)
                .distinct()
                .toArray(String[]::new);

        for (String term : terms) {
            // update term map & doc map
            hashToTerm.putIfAbsent(term.hashCode(), term);
            hashToDocno.putIfAbsent(doc.id.hashCode(), doc.id);

            // update termFreqInDoc map
            Map<Integer, Integer> docToFreq = termFreqInDoc.getOrDefault(term.hashCode(),
                    new HashMap<Integer, Integer>());
            docToFreq.put(doc.id.hashCode(), docToFreq.getOrDefault(doc.id.hashCode(), 0) + 1);
            termFreqInDoc.put(term.hashCode(), docToFreq);
        }
    }
}
