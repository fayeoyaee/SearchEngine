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

    private double calculateTfIdf(int termHash, int docHash, int nDocs,
            Map<Integer, Map<Integer, Integer>> termFreqInDoc) {
        int tf_t_d = termFreqInDoc.getOrDefault(termHash, new HashMap<>()).getOrDefault(docHash, 0); // if doc not
                                                                                                     // contains term,
                                                                                                     // return 0
        int df_t = termFreqInDoc.getOrDefault(termHash, new HashMap<>()).size();
        double idf_t = Math.log(nDocs / df_t);
        return tf_t_d * idf_t;
    }

    /**
     * Generate top N docs
     */
    public Deque<Entry> query(String query, Map<Integer, String> hashToTerm, Map<Integer, String> hashToDocno,
            Map<Integer, Map<Integer, Integer>> termFreqInDoc) {
        // debug
        // System.out.printf("Querying %s, termCollectionSize=%d,
        // docCollectionSize=%d\n", query, hashToTerm.size(), hashToDocno.size());

        // Get token arrays and delete the first 3 words "Document will
        // discuss/identify.."
        String[] tmp = Util.queryTokenizer(query);
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
                // if query term not in term collecion, return 0
                rank += calculateTfIdf(queryToken.hashCode(), docHash, hashToDocno.size(), termFreqInDoc);
            }
            retPQ.add(new Entry(docHash, rank));
            if (retPQ.size() > N)
                retPQ.poll();
        }

        Deque<Entry> retDQ = new LinkedList<>();
        retPQ.forEach(entry -> retDQ.addFirst(entry));

        return retDQ;
    }
}
