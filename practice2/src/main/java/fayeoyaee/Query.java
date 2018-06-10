package fayeoyaee;

import java.util.*;

public class Query {
    public static int N = 50;

    public Query() {
    }

    public class Entry {
		Integer docHash;
        Integer rank;

        Entry(int docHash, int rank) {
            this.docHash = docHash;
            this.rank = rank;
        }
    }

    /**
     * Generate top N docs 
     */
    public Deque<Entry> query(String query, Map<Integer, String> hashToTerm, Map<Integer, String> hashToDocno,
            Map<Integer, Map<Integer, Integer>> tfIdf) {
        // debug
        // System.out.printf("Querying %s, termCollectionSize=%d, docCollectionSize=%d\n", query, hashToTerm.size(), hashToDocno.size());

        // Get token arrays and delete the first 3 words "Document will discuss/identify.."
        String[] tmp = Util.queryTokenizer(query);
        String[] queryTokens = Arrays.copyOfRange(tmp, 3, tmp.length);

        // keep a priorityQueue for docs ranking top N
        PriorityQueue<Entry> retPQ = new PriorityQueue<>(N, (entry1, entry2) -> entry1.rank - entry2.rank);

        // compute tfidf for each doc 
        for (int docHash : hashToDocno.keySet()) {
            int rank = 0;
            for (String queryToken : queryTokens) {
                // if query term not in term collecion, return 0
                rank += tfIdf.getOrDefault(queryToken.hashCode(), new HashMap<>()).getOrDefault(docHash, 0);
            }
            retPQ.add(new Entry(docHash, rank));
            if (retPQ.size() > N) retPQ.poll();
        }

        Deque<Entry> retDQ = new LinkedList<>();
        retPQ.forEach(entry -> retDQ.addFirst(entry));

        return retDQ;
    }
}
