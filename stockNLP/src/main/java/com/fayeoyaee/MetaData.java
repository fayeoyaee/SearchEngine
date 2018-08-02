package com.fayeoyaee;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

class MetaData {
  public static int CONTEXT_MIN_LEN = 10;
  public static MongoDatabase db;

  String title;
  String url;
  String time;


  MetaData(String title, String url, String time) {
    this.title = title;
    this.url = url;
    this.time = time;
  }

  public static void init() {
    // TODO: hide env vars
    db = MongoClients.create("mongodb://testuser2:testuser2@ds253831.mlab.com:53831/quant_nlp")
        .getDatabase("quant_nlp");
  }

  private class Context {
    int start;
    int end;

    Context(int s, int e) {
      this.start = s;
      this.end = e;
    }
  }

  /**
   * Given a MetaData, parse it's article by passing to a NLP parser and a context
   * extractor
   */
  protected void parse() {
    try {
      // avoid parsing the same doc
      if (db.getCollection("docs").countDocuments(Filters.eq("_id", this.title.hashCode())) != 0) {
        return;
      }

      // Get the full article
      String text = Jsoup.connect(this.url).get().select(".Article").first().text();

      // Add title
      text = this.title + " " + text;
      
      // NLP lexical parser
      List<Term> term_list = HanLP.segment(text);

      // Remove apostrophes and stop words by filtering according to term nature
      term_list = term_list.stream()
          .filter(term -> !term.nature.toString().equals("w") && !term.nature.toString().equals("sw"))
          .collect(Collectors.toList());

      // Helper data structure to calculate term frequecny
      Map<String, Integer> tf = new HashMap<>();
      Set<String> relatedStocks = new HashSet<>();

      int pos = 0, neg = 0;

      Stack<Context> stack = new Stack<>();
      for (int i = 0; i < term_list.size(); ++i) {
        // Extract contexts: Each context are some words before and after the stock
        // index (tagged "sn") with some minimum length
        if (term_list.get(i).nature.toString().equals("sn")) {
          relatedStocks.add(term_list.get(i).word);

          if (!stack.isEmpty()) {
            Context context = stack.peek();
            if (context.end < i - CONTEXT_MIN_LEN) {
              // push new context
              stack
                  .push(new Context(Math.max(i - CONTEXT_MIN_LEN, 0), Math.min(i + CONTEXT_MIN_LEN, term_list.size())));
            } else {
              // extend the context
              stack.pop();
              stack.push(new Context(context.start, Math.min(i + CONTEXT_MIN_LEN, term_list.size())));
            }
          } else {
            stack.push(new Context(0, Math.min(i + CONTEXT_MIN_LEN, term_list.size())));
          }
        }

        // calculate how many pos/neg words in the document
        pos += term_list.get(i).nature.toString().equals("pos") ? 1 : 0;
        neg += term_list.get(i).nature.toString().equals("neg") ? 1 : 0;

        // To form term frequency
        tf.put(term_list.get(i).word, (tf.getOrDefault(term_list.get(i).word, 0) + 1));
      }

      // Extract terms in each context
      Set<String> context_terms = new HashSet<>();

      // Form snippet of each doc by concat contexts
      StringBuilder snippet = new StringBuilder("......");

      Iterator<Context> iter = stack.iterator();
      while (iter.hasNext()) {
        Context context = iter.next();
        StringBuilder sb = new StringBuilder();
        for (int i = context.start; i < context.end; ++i) {
          context_terms.add(term_list.get(i).word);
          sb.append(term_list.get(i).word);
          sb.append(",");
        }
        snippet.append(sb.toString());
        snippet.append("......");
      }

      // Store data to be writen to db in the following lists. We insert terms in batch mode.
      List<Document> postings = new ArrayList<Document>();
      List<Document> terms = new ArrayList<Document>();

      tf.keySet().forEach(term -> {
        // update postings and terms
        postings.add(new Document("term", term.hashCode())
        .append("freq", tf.getOrDefault(term, 0))
        .append("doc", this.title.hashCode()) // a doc is identified by its url
        .append("norm", context_terms.contains(term) ? 3 : (this.time.contains(term) ? 2 : 1)));

        if (db.getCollection("terms").countDocuments(Filters.eq("_id", term.hashCode())) == 0) {
          terms.add(new Document("_id", term.hashCode())
          .append("term", term));
        }
      });

      // insert to mongodb
      if (!postings.isEmpty()) {
        db.getCollection(this.time.substring(0,5)).insertMany(postings);
      }

      if (!terms.isEmpty()) {
        db.getCollection("terms").insertMany(terms);
      }

      db.getCollection("docs").insertOne(new Document("_id", this.title.hashCode())
      .append("url", this.url)
      .append("title", this.title)
      .append("time", this.time.substring(0,5))
      .append("pos", pos)
      .append("neg", neg)
      .append("snippet", snippet.toString())
      .append("len",term_list.size())
      .append("relatedStocks", relatedStocks.toString()));

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

