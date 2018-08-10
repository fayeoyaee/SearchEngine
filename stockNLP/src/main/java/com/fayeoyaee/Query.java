package com.fayeoyaee;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.hankcs.hanlp.HanLP;
import com.mongodb.client.*;
import com.mongodb.client.model.*;

import org.bson.Document;
import org.json.*;

public class Query {
  static MongoDatabase db;
  static MongoClient client;

  public static void init(String username, String password) {
    client = MongoClients.create(
        "mongodb://"+username+":"+password+"@ds253831.mlab.com:53831/quant_nlp");
    db = client.getDatabase("quant_nlp");
  }

  public static void close() { client.close(); }

  int QueryNDocs = 10; // the number of query docs to return for /query

  /**
   * A daemon process to calculate document frequency for each term in doc
   */
  public static void calDf(String[] args) {
    String collectionName = args[0]; 
    Query.init(args[1], args[2]);

    Set<Integer> ids = new HashSet<>(); // to assure no duplicated terms
    List<Document> toWrite = new ArrayList<>();

    MongoCursor<Document> cursor =
        db.getCollection(collectionName).find().iterator();
    while (cursor.hasNext()) {
      Document doc = cursor.next();
      int id = doc.getInteger("term");
      if (!ids.contains(id)) {
        Document newDoc = new Document("_id", id).append(
            "df", (int)db.getCollection(collectionName)
                      .countDocuments(Filters.eq("term", id)));
        toWrite.add(newDoc);
        ids.add(id);
        System.out.println(newDoc);
      }
    }

    db.getCollection(collectionName + "-Df").insertMany(toWrite);

    Query.close();
  }

  // Get doc frequency of a term in a collection
  private int getdf(String collectionName, Object termHash) {
    System.out.println("60"+collectionName);
    return (int)db.getCollection(collectionName)
        .countDocuments(Filters.eq("term", termHash));
  }

  private String hashToTerm(int hash) {
    return db.getCollection("terms")
        .find(Filters.eq("_id", hash))
        .first()
        .getString("term");
  }

  /**
   * Retrive the words with highest df (for hot words) and return json string
   */
  public String popWords(String collectionName, int nPopWords) {
    // find term-df pairs and sort according to df
    collectionName = collectionName + "-Df";
    System.out.println(
        "[Query.popWords]: querying dababase and sorting documents");
    // System.out.println("80"+collectionName);
    MongoCursor<Document> cursor = db.getCollection(collectionName)
                                       .find()
                                       .sort(Sorts.descending("df"))
                                       .iterator();

    // add the first NPopWords to json array
    System.out.println("[Query.popWords]: constructing JSON");
    JSONArray ja = new JSONArray();
    int i = 0;
    while (cursor.hasNext() && i < nPopWords) {
      Document doc = cursor.next();
      ja.put(new JSONObject()
                 .put("term", hashToTerm(doc.getInteger("_id")))
                 .put("df", doc.getInteger("df")));
      i++;
    }

    System.out.println("[Query.popWords]: return popWords: " + ja.toString());
    return ja.toString();
  }

  private Document hashToDoc(int hash) {
    return db.getCollection("docs").find(Filters.eq("_id", hash)).first();
  }

  /**
   * Return a json with all the docs describing the stock in collections and the
   * number of pos/neg words describing the stock.
   */
  public String queryStock(String stockName, List<String> collectionNames) {
    // calculate how many pos and neg in all the docs that describe the stock
    int totalPos = 0, totalNeg = 0;
    JSONArray ja = new JSONArray();

    // get all related postings
    for (String collectionName : collectionNames) {
    // System.out.println("117"+collectionName);
      MongoCursor<Document> cursor =
          db.getCollection(collectionName)
              .find(Filters.eq("term", stockName.hashCode()))
              .iterator();
      while (cursor.hasNext()) {
        // get related doc
        Document doc = hashToDoc(cursor.next().getInteger("doc"));

        totalPos += doc.getInteger("pos");
        totalNeg += doc.getInteger("neg");

        ja.put(new JSONObject()
                   .put("url", doc.getString("url"))
                   .put("title", doc.getString("title"))
                   .put("snippet", doc.getString("snippet")));
      }
    }

    return new JSONObject()
        .put("docs", ja.toString())
        .put("pos", totalPos)
        .put("neg", totalNeg)
        .toString();
  }

  // Get term freq, and norm of a term in a doc
  private int[] getTfAndNorm(String collectionName, Object termHash,
                             Object docHash) {
    // System.out.println("146"+collectionName);
    Document doc =
        db.getCollection(collectionName)
            .find(new Document("term", termHash).append("doc", docHash))
            .first();

    if (doc != null) {
      int tf = doc.getInteger("freq");
      int norm = doc.getInteger("norm");
      return new int[] {tf, norm};
    }

    return new int[] {0, 0};
  }

  // we use this helper structure only to sort doc by scores
  private class DocWithScore {
    Document doc;
    double score;

    DocWithScore(Document doc, double score) {
      this.doc = doc;
      this.score = score;
    }
  }

  /**
   * Get a json array with top X documents related with the query
   */
  public String query(String queryStr, int queryWinSize) {
    // pass query string through nlp segmenter
    List<Integer> queryHashes = HanLP.segment(queryStr)
                                    .stream()
                                    .map(term -> term.word.hashCode())
                                    .collect(Collectors.toList());
    System.out.println("[Query.query]: number of parsed terms in query: " + queryHashes.size());

    PriorityQueue<DocWithScore> minHeap =
        new PriorityQueue<>(QueryNDocs, (a, b) -> (a.score - b.score < 0 ? -1 : 1));

    // we keep a search window of QueryWinSize days
    for (int i = 0; i < queryWinSize; i++) {
      int boost = queryWinSize - i;
      // String collectionName = LocalDate.now().plusDays(i).format(
      String collectionName = LocalDate.now().minusDays(i).format(
          DateTimeFormatter.ofPattern("MM-dd"));
      // collectionName = "b_"+collectionName;
      System.out.println("[Query.query]: iterating document for " +
                         collectionName + "; found " +
                         db.getCollection(collectionName)
                             .countDocuments(Filters.in("term", queryHashes)) +
                         " collections");
      MongoCursor<Document> cursor = db.getCollection(collectionName)
                                         .find(Filters.in("term", queryHashes))
                                         .iterator();
      double n = db.getCollection("docs").countDocuments(
          Filters.eq("time", collectionName));

      // we store idfs before iterating the docs -- only when there's document
      // that contains the term
      double[] idfs = new double[queryHashes.size()];
      for (int j = 0; j < queryHashes.size(); j++) {
        int df = getdf(collectionName, queryHashes.get(j));
        if (df == 0) {
          idfs[j] = 0;
        } else {
          idfs[j] = Math.log(n / (double)df);
        }
      }

      // to record parsed docs
      Set<Integer> docIds = new HashSet<>(); 
      while (cursor.hasNext()) {
        Document doc = hashToDoc(cursor.next().getInteger("doc"));
        if (!docIds.contains(doc.getInteger("_id"))) {
          System.out.println("[Query.query]: checking doc with id: "+doc.getInteger("_id"));
          docIds.add(doc.getInteger("_id"));

          if (doc != null) {
            // calculate score
            double sum = 0;
            int coord = 0;

            for (int j = 0; j < queryHashes.size(); j++) {
              int[] stat = getTfAndNorm(collectionName, queryHashes.get(j),
                                        doc.getInteger("_id"));
              sum += stat[0] * Math.pow(idfs[j], 2) * boost * stat[1];

              // record how many query words appear in doc
              coord += stat[0] != 0 ? 1 : 0;
              // System.out.println("[Query.query]: for each word each doc: tf="+stat[0]+" norm="+stat[1]+" idf="+idfs[j]+" boost="+boost+" coord="+coord);
            }
            double score = coord * sum;
            // System.out.println("[Query.query]: for each doc: score="+score);

            // add doc to min heap; poll if min heap is full
            minHeap.add(new DocWithScore(doc, score));
            // System.out.println("[Query.query]: adding "+score+" to minHeap");
            if (minHeap.size() > QueryNDocs) {
              DocWithScore docP = minHeap.poll();
              // System.out.println("[Query.query]: polling "+docP.score+"from minHeap");
            }
          }
        }
      }
    }

    System.out.println("[Query.query]: constructing JSON; minheap size="+minHeap.size());
    JSONArray ja = new JSONArray();
    int i = 1;
    while (!minHeap.isEmpty()) {
      DocWithScore docS = minHeap.poll();

      System.out.println("[Query.query]: "+docS.doc.getString("url"));
      System.out.println("[Query.query]: "+docS.doc.getString("title"));
      System.out.println("[Query.query]: "+docS.doc.getString("snippet"));
      ja.put(QueryNDocs-i,new JSONObject()
                    .put("url", docS.doc.getString("url"))
                    .put("score", docS.score)
                    .put("title", docS.doc.getString("title"))
                    .put("snippet", docS.doc.getString("snippet")));
      i++;
    }
    return ja.toString();
  }

  public static void main(String[] args) {
    Query.calDf(args);
  }
}