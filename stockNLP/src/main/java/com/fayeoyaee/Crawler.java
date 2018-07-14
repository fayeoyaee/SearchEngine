package com.fayeoyaee;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.hankcs.hanlp.HanLP;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

/**
 * Crawl a list of urls and parse them
 *
 */
public class Crawler {
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
  private void start_requests(List<String> initial_urls) {
    initial_urls.forEach((url) -> {
      try {
        String body = Jsoup.connect(url).userAgent("Mozilla").get().body().text();
        JSONArray list = new JSONArray(body.substring(1, body.length() - 1));
        for (int i = 0; i < list.length(); ++i) {
          JSONObject j = list.getJSONObject(i);
          if (!JSONObject.NULL.equals(j.get("Title")) && !JSONObject.NULL.equals(j.get("Url")) && !JSONObject.NULL.equals(j.get("CreatedTime"))) {
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
      // get the full article
      String text = Jsoup.connect(p.url).get().select(".Article").first().text();

      // NLP lexical parser 
      System.out.println(HanLP.segment(text));
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
