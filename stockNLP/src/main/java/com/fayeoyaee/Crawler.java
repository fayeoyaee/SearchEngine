package com.fayeoyaee;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.dictionary.NatureDictionaryMaker;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

/**
 * Crawl a list of urls and parse them
 *
 */
public class Crawler {
  public static int CONTEXT_MIN_LEN = 10;

  MongoDatabase db;

  /**
   * Post subclass to store the meta data of individual posts
   */
  private class Post {
    String title;
    String url;
    String time;

    Post(String title, String url, String time) {
      this.title = title;
      this.url = url;
      this.time = time;
    }
  }

  private Queue<Post> posts = new LinkedList<>();

  /**
   * Given initial_urls, parse and store all the posts
   */
  private void startRequests(List<String> initialUrls) {
    initialUrls.forEach((url) -> {
      try {
        String body = Jsoup.connect(url).userAgent("Mozilla").get().body().text();
        JSONArray list = new JSONArray(body.substring(1, body.length() - 1));
        for (int i = 0; i < list.length(); ++i) {
          JSONObject j = list.getJSONObject(i);
          if (!JSONObject.NULL.equals(j.get("Title")) && !JSONObject.NULL.equals(j.get("Url"))
              && !JSONObject.NULL.equals(j.get("CreatedTime"))) {
            posts.add(new Post((String) j.get("Title"), (String) j.get("Url"), (String) j.get("CreatedTime")));
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
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
   * Created a customized Term class because Term@HanLP is crappy (no implement hashcode)
   */
  private class MyTerm {
    String word; 
    String nature;
    MyTerm(Term term) {
      word = term.word;
      nature = term.nature.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MyTerm)
        {
            MyTerm term = (MyTerm)obj;
            if (this.nature == term.nature && this.word.equals(term.word))
            {
                return true;
            }
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + word.hashCode();
      result = 31 * result + nature.hashCode();
      return result;
    }
  }

  /**
   * Given a Post, parse it's article by passing to a NLP parser and a context
   * extractor
   */
  private void parse(Post p) {
    try {
      // Get the full article
      String text = Jsoup.connect(p.url).get().select(".Article").first().text();

      // Add title
      text = p.title + " " + text;
      
      // NLP lexical parser
      List<Term> term_list = HanLP.segment(text);

      // Remove apostrophes and stop words by filtering according to term nature
      term_list = term_list.stream()
          .filter(term -> !term.nature.toString().equals("w") && !term.nature.toString().equals("sw"))
          .collect(Collectors.toList());

     // Helper data structure to calculate term frequecny
      Map<MyTerm, Integer> tf = new HashMap<>();
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
        tf.put(new MyTerm(term_list.get(i)), (tf.getOrDefault(new MyTerm(term_list.get(i)), 0) + 1));
      }

      // Extract terms in each context
      Set<MyTerm> context_terms = new HashSet<>();
      Iterator<Context> iter = stack.iterator();
      while (iter.hasNext()) {
        Context context = iter.next();
        for (int i = context.start; i < context.end; ++i) {
          context_terms.add(new MyTerm(term_list.get(i)));
        }
      }

      // For storing data to be write to db. We insert terms in batch mode.
      List<Document> postings = new ArrayList<Document>();
      List<Document> terms = new ArrayList<Document>();

      tf.keySet().forEach(term -> {
        // update postings and terms
        postings.add(new Document("hash", term.word.hashCode())
        .append("freq", tf.getOrDefault(term, 0))
        .append("doc", p.title.hashCode()) // a doc is identified by its url
        .append("pos", context_terms.contains(term) ? 3 : (p.time.contains(term.word) ? 2 : 1)));

        if (db.getCollection("terms").countDocuments(Filters.eq("_id", term.word.hashCode())) == 0) {
          terms.add(new Document("_id", term.word.hashCode())
          .append("term", term.word)
          .append("pos", term.nature.toString()));
        }
      });

      // get today's date in string
      String collection_name = Instant.now().toString().substring(5,10);

      // insert to mongodb
      db.getCollection(collection_name).insertMany(postings);
      db.getCollection("terms").insertMany(terms);
      db.getCollection("docs").insertOne(new Document("_id", p.title.hashCode())
      .append("url", p.url)
      .append("pos", pos)
      .append("neg", neg)
      .append("relatedStocks", relatedStocks.toString()));
 
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void crawl() {
    // init mongodb collection
    // TODO: today
    // TODO: hide env vars
    db = MongoClients.create("mongodb://:@ds253831.mlab.com:53831/quant_nlp")
        .getDatabase("quant_nlp");

    // TODO: multiple pages
    List<String> initial_urls = Arrays.asList("http://app.cnfol.com/test/newlist_api.php?catid=4035&page=1");

    // add posts
    startRequests(initial_urls);

    // parse articles of the posts
    int cnt = 0;
    while (!posts.isEmpty()) {
      parse(posts.remove()); 
      cnt ++;
      if (cnt == 2) {
        break; // debug
      }
    }
  }
}
