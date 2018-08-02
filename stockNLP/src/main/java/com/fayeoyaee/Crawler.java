package com.fayeoyaee;

import java.io.IOException;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

/**
 * Crawl a list of urls and parse them
 *
 */
public class Crawler {

  private Queue<MetaData> posts = new LinkedList<>();

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
            posts.add(new MetaData((String) j.get("Title"), (String) j.get("Url"), (String) j.get("CreatedTime")));
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  public void crawl() {
    // init mongodb collection
    MetaData.init();

    // add MetaDatas
    List<String> initial_urls = new ArrayList<>();
    for (int i = 10; i < 20; ++i) {
      initial_urls.add("http://app.cnfol.com/test/newlist_api.php?catid=4035&page=" + String.valueOf(i));
    }
    startRequests(initial_urls);

    // parse MetaDatas
    while (!posts.isEmpty()) {
      MetaData post = posts.remove();
      post.parse();
      System.out.println("crawling " + post.url + " on" + post.time);
    }
  }

  public static void main(String[] args) {
    Crawler c = new Crawler();
    c.crawl();
  }
}