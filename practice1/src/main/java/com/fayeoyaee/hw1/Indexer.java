package com.fayeoyaee.hw1;

import java.util.*;
import java.io.*;
import java.net.*;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.common.xcontent.XContentFactory;

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

    private int cnt;
    private TransportClient client;

    public Indexer() {}

    public void activate() throws Exception {
        cnt = 0;
        client = new PreBuiltTransportClient(Settings.EMPTY)
        .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300))
        .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));
    }

    public void deactivate() {
        client.close();
    }

    /**
     * Add doc to indexer
     */
    public void add(String fileName) throws Exception {
        BulkRequestBuilder bulkRequest = client.prepareBulk();

        List<Document> docs = fileParser(fileName);
        for (Document doc : docs) {
            String[] tokens = Util.contentParser(doc.content);
            for (String token : tokens) {
                bulkRequest.add(client.prepareIndex("apdata", "idict", Integer.toString(cnt++))
                .setSource(
                    XContentFactory.jsonBuilder()
                        .startObject()
                            .field("term", token)
                            .field("docno", doc.id)
                        .endObject()
                    )
                );
            }
        }

        BulkResponse bulkResponse = bulkRequest.get();
        if (bulkResponse.hasFailures()) {
            throw(new Exception());
        }
    }

    /**
     * Parse file into Document array
     */
    private List<Document> fileParser(String fileName) {
        try {
            List<Document> docs = new ArrayList<>();
            InputStream i = Indexer.class.getResourceAsStream("/AP_DATA/ap89_collection/"+fileName);
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
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            Indexer indexer = new Indexer();
            indexer.activate();
            URL url = Indexer.class.getResource("/AP_DATA/ap89_collection");
            File folder = new File(url.toURI());
            File[] files = folder.listFiles();
            for (File file : files) {
                indexer.add(file.getName());
                break;
            }
            indexer.deactivate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
