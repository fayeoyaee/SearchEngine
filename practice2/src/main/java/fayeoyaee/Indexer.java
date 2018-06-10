package fayeoyaee;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * From an index from docs and send to elasticsearch
 */
public class Indexer {

    public class Document {
        public String content;
        public String id;

        public Document() {
        }

        public Document(String id) {
            this.id = id;
        }
    }

    public static Map<Integer, String> hashToTerm = new HashMap<>();
    public static Map<Integer, String> hashToDocno = new HashMap<>(90000);
    public static Map<Integer, Map<Integer, Integer>> termFreqInDoc = new HashMap<>();

    public Indexer() {
    }

    /**
     * Parse file into Document array
     * 
     * @throws IOException
     */
    private List<Document> fileParser(String fileName) throws IOException {
        List<Document> docs = new ArrayList<>();
        InputStream i = Indexer.class.getResourceAsStream("/AP_DATA/ap89_collection/" + fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(i));
        String thisLine;
        StringBuilder sb = new StringBuilder();
        Document doc = new Document();
        boolean isRecord = false;
        String potentialContent;
        while ((thisLine = br.readLine()) != null) {
            if (thisLine.contains("<DOCNO>")) {
                String id = thisLine.substring(thisLine.indexOf("<DOCNO>") + 8, thisLine.indexOf("</DOCNO>") - 1);
                doc = new Document(id);
            }
            if (thisLine.contains("<TEXT>")) {
                isRecord = true;
                sb = new StringBuilder();
                potentialContent = thisLine.substring(thisLine.indexOf("<TEXT>") + 6);
                if (potentialContent.isEmpty()) {
                    sb.append(potentialContent.trim());
                    sb.append(" ");
                }
            }
            if (thisLine.contains("</TEXT>")) {
                potentialContent = thisLine.substring(0, thisLine.indexOf("</TEXT>"));
                if (potentialContent.isEmpty()) {
                    sb.append(potentialContent.trim());
                    sb.append(" ");
                }
                isRecord = false;
                doc.content = sb.toString();
                docs.add(doc);
            }
            if (isRecord) {
                sb.append(thisLine.trim());
                sb.append(" ");
            }
        }
        return docs;
    }

    /**
     * Add doc to indexer: update term map, doc map, tuple list
     * 
     * @throws IOException
     */
    public void add(String fileName) throws IOException {
        List<Document> docs = fileParser(fileName);
        for (Document doc : docs) {
            Util.indexerTokenizer(doc, hashToTerm, hashToDocno, termFreqInDoc);
        }
    }

    /**
     * Drive program for Indexer and Query
     * 
     * @throws URISyntaxException
     * @throws IOException
     */
    public static void main(String[] args) throws URISyntaxException, IOException {
        Indexer indexer = new Indexer();
        URL url = Indexer.class.getResource("/AP_DATA/ap89_collection");
        File folder = new File(url.toURI());
        File[] files = folder.listFiles();
        // for (int i = 0; i < 5; ++i) { // debug
        for (int i = 0; i < files.length; ++i) {
            System.out.printf("Indexing the %dth file out of %d files: now have %d terms %d docs, using %d%n mb\n", i,
                    files.length, hashToTerm.size(), hashToDocno.size(), (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / (1024L*1024L));
            indexer.add(files[i].getName());
        }

        Query query = new Query();
        InputStream i = Indexer.class.getResourceAsStream("/AP_DATA/query_desc.51-100.short.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(i));
        String queryStr = br.readLine();
        int queryCnt = 0;
        while (queryStr != null && !queryStr.isEmpty()) {
            // get query results
            Deque<Query.Entry> results = query.query(queryStr, hashToTerm, hashToDocno, termFreqInDoc);

            // print query results
            System.out.printf("\n\nQuery string: \n%s\nFetched %d documents: \n", queryStr, results.size());
            results.forEach(entry -> System.out.printf("%s : %.2f, ", hashToDocno.get(entry.docHash), entry.rank));

            // read next query
            queryStr = br.readLine();
            queryCnt++;
            System.out.printf("\nRunning the %dth query \n", queryCnt);

            // if (queryCnt > 3) break; // debug
        }

        System.out.printf("Indexed %d docs in %d files. Processed %d queries.", hashToDocno.size(), files.length,
                queryCnt);
    }
}
