package com.fayeoyaee;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.stream.Collectors;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

/**
 * Crawl a list of urls and parse them
 *
 */
public class Crawler {
  public static int CONTEXT_MIN_LEN = 10;

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

  private class Context {
    int start;
    int end;

    Context(int s, int e) {
      this.start = s;
      this.end = e;
    }
  }

  private Queue<Post> posts = new LinkedList<>();

  /**
   * Given initial_urls, parse and store all the posts
   */
  private void start_requests(List<String> initial_urls) {
    initial_urls.forEach((url) -> {
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

  /**
   * Parse the article of a post
   */
  private void parse(Post p) {
    try {
      // Get the full article
      String text = Jsoup.connect(p.url).get().select(".Article").first().text();

      // NLP lexical parser
      List<Term> term_list = HanLP.segment(text);

      // Remove apostrophes
      term_list = term_list.stream().filter(term -> !term.nature.toString().equals("w")).collect(Collectors.toList());

      // Extract contexts: Each context are some words before and after the stock
      // index (tagged "sn") with some minimum length
      Stack<Context> stack = new Stack<>();
      for (int i = 0; i < term_list.size(); ++i) {
        if (term_list.get(i).nature.toString().equals("sn")) {
          if (!stack.isEmpty()) {
            Context context = stack.peek();
            if (context.end < i - CONTEXT_MIN_LEN) {
              // push new context
              stack.push(new Context(Math.max(i-CONTEXT_MIN_LEN,0), Math.min(i+CONTEXT_MIN_LEN, term_list.size())));
            } else {
              // extend the context
              stack.pop();
              stack.push(new Context(context.start, Math.min(i+CONTEXT_MIN_LEN, term_list.size())));
            }
          } else {
              stack.push(new Context(0, Math.min(i+CONTEXT_MIN_LEN, term_list.size())));
          }
        }
      }

      // Extract terms in each context
      List<List<Term>> context_terms = new ArrayList<>();
      Iterator<Context> iter = stack.iterator();
      while (iter.hasNext()) {
        Context context = iter.next();
        List<Term> to_add = new ArrayList<>();
        for (int i = context.start; i < context.end; ++i) {
          to_add.add(term_list.get(i));
        }
        context_terms.add(to_add);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void crawl() {
    List<String> initial_urls = Arrays.asList("http://app.cnfol.com/test/newlist_api.php?catid=4035&page=2");

    // add posts
    start_requests(initial_urls);

    // parse articles of the posts
    // TODO: make parse and crawl parallel
    while (!posts.isEmpty()) {
      parse(posts.remove());
      break; // debug
    }
  }
}
