package fayeoyaee;

import java.util.*;

public class Query {
    public static int N = 50;

    public Query() {
    }

    public class Entry {
        Integer docHash;
        Double rank;

        Entry(int docHash, double rank) {
            this.docHash = docHash;
            this.rank = rank;
        }
    }

    /**
     * parse content into tokens for query
     */
    private String[] queryTokenizer(String content) {
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


    private double calculateTfIdf(int termHash, int docHash, int nDocs,
            Map<Integer, Map<Integer, Integer>> termFreqInDoc) {
        int tf_t_d = termFreqInDoc.getOrDefault(termHash, new HashMap<>()).getOrDefault(docHash, 0); 
        if (tf_t_d == 0) return 0;

        int df_t = termFreqInDoc.getOrDefault(termHash, new HashMap<>()).size();
        double idf_t = Math.log(nDocs / df_t);

        return tf_t_d * idf_t;
    }

    /**
     * Generate top N docs
     */
    public Deque<Entry> query(String query, Map<Integer, String> hashToTerm, Map<Integer, String> hashToDocno,
            Map<Integer, Map<Integer, Integer>> termFreqInDoc) {
        // Get token arrays and delete the first 3 words
        String[] tmp = queryTokenizer(query);
        String[] queryTokens = Arrays.copyOfRange(tmp, 3, tmp.length);

        // keep a priorityQueue for docs ranking top N
        PriorityQueue<Entry> retPQ = new PriorityQueue<>(N, (entry1, entry2) -> {
            if (entry1.rank < entry2.rank)
                return -1;
            else if (entry1.rank > entry2.rank)
                return 1;
            else
                return 0;
        });

        // compute tfidf for each doc
        for (int docHash : hashToDocno.keySet()) {
            double rank = 0;
            for (String queryToken : queryTokens) {
                rank += calculateTfIdf(queryToken.hashCode(), docHash, hashToDocno.size(), termFreqInDoc);
            }
            retPQ.add(new Entry(docHash, rank));
            if (retPQ.size() > N)
                retPQ.poll();
        }

        // use a deque to reverse priority queue
        Deque<Entry> retDQ = new LinkedList<>();
        retPQ.forEach(entry -> retDQ.addFirst(entry));
        return retDQ;
    }
}
