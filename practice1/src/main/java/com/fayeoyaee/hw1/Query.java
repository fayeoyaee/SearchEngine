package com.fayeoyaee.hw1;

import java.util.*;
import java.io.*;
import java.net.InetAddress;

import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.index.query.QueryBuilders;
import com.google.gson.*;

public class Query {
    public static class Entry {
        String term;
        String docno;
    }

    public static int NQUERY = 50;
    private TransportClient client;

    public Query() {}

    public void activate() throws Exception {
        client = new PreBuiltTransportClient(Settings.EMPTY)
        .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300))
        .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));
    }

    public void deactivate() {
        client.close();
    }

    /**
     * Generate NQUERY docs matching the query string
     */
    public Set<String> query(String query) {
        // delete the first 3 words "Document will discuss/identify.."
        String[] tmp = Util.contentParser(query);
        String[] tokens = Arrays.copyOfRange(tmp, 3, tmp.length);

        Set<String> final_results = new HashSet<>();
        Gson gson = new Gson();
        for (String token : tokens) {
            SearchResponse response = client.prepareSearch("apdata")
            .setTypes("idict")
            .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
            .setQuery(QueryBuilders.termQuery("term", token))
            .setFrom(0).setSize(60).setExplain(true)
            .get();
            List<SearchHit> searchHits = Arrays.asList(response.getHits().getHits());
            List<Entry> new_results = new ArrayList<>();
            searchHits.forEach(
                hit -> new_results.add(gson.fromJson(hit.getSourceAsString(), Entry.class)));
            final_results = merge(final_results, new_results);
        }
        return final_results;
    }

    private Set<String> merge(Set<String> s1, List<Entry> l2){
        Set<String> s2 = entry2String(l2);
        if (s1.size() == 0) {
            return s2;
        }
        if (l2.size() == 0) {
            return s1;
        }
        s1.removeIf(s -> !s2.contains(s));
        return s1;
    }

    private Set<String> entry2String(List<Entry> l) {
        HashSet<String> ret = new HashSet<>();
        l.forEach(
            entry -> ret.add(entry.docno));
        return ret;
    }

    public static void main(String[] args) {
        try {
            Query query = new Query();
            query.activate();
            InputStream i = Indexer.class.getResourceAsStream("/AP_DATA/query_desc.51-100.short.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(i));
            String thisLine = br.readLine();
            List<String> queryStrs = new LinkedList<>();
            while (thisLine != null && !thisLine.isEmpty()) {
                queryStrs.add(thisLine);
                thisLine = br.readLine();
            }

            for (String queryStr : queryStrs) {
                Set<String> results = query.query(queryStr);
                System.out.printf("\n\nQuery string: \n%s\nCorresponding documents: \n", queryStr);
                results.forEach(str -> System.out.printf("%s, ", str));
            }
            System.out.println("");
            query.deactivate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
