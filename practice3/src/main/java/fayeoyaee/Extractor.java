package fayeoyaee;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import twitter4j.*;

/**
 * 
 *
 */
public class Extractor {

    /**
     * Given a hashtag query, extract tweets that contain links
     */
    public void extract(String querystr, int links, int depth, int rank) {
        try {
            // Create a crawler with specified links and depth
            Crawler c = new Crawler(links, depth, rank);

            // Build a twitter from factory
            Twitter twitter = TwitterFactory.getSingleton();

            // Build a query with default result_type (mixed) and max count of 100
            Query query = new Query(querystr);
            query.count(100);
            query.lang("en");

            // Get query results
            QueryResult result;
            do {
                result = twitter.search(query);
                for (Status status : result.getTweets()) {
                    URLEntity[] urls = status.getURLEntities();
                    if (urls.length != 0) {
                        for (int i = 0; i < urls.length; ++i) {
                            // Only crawl external links
                            String url = urls[i].getExpandedURL();
                            if (!url.startsWith("https://twitter.com")) {
                                // System.out.println("\nExtract: Crawling external link: " + url);
                                c.crawl(urls[i].getExpandedURL());
                            }
                            if (c.crawled >= c.target) {
                                PrintWriter writer = new PrintWriter("query_"+querystr+"_links_"+links+"_depth_"+depth+".txt", "UTF-8");

                                // Statistics a) and c)
                                writer.println("Summary:\n" + "Crawled " + c.crawled + " links for topic \""
                                        + querystr + "\" (depth=" + c.depth + "). There're "
                                        + c.linkTypeStatMap.values().stream().reduce(0, (a, b) -> a + b)
                                        + " unique links (TEXT=" + c.linkTypeStatMap.get(Crawler.LinkTypes.TEXT)
                                        + ", IMAGE=" + c.linkTypeStatMap.get(Crawler.LinkTypes.IMAGE) + ", VIDEO="
                                        + c.linkTypeStatMap.get(Crawler.LinkTypes.VIDEO) + ").\n");

                                // Statistics b)
                                writer.println("Domain Distribution (" + c.domainMap.size() + " domains):");
                                c.domainMap.entrySet().stream()
                                        .sorted((a, b) -> (b.getValue().size() - a.getValue().size()))
                                        .forEach(e -> writer.println(e.getKey() + ": " + e.getValue().size()));

                                // Statistics d)
                                writer.println(c.getTopLinks(c.inLinkMaxHeap).stream()
                                        .reduce("\nTop " + c.rank + " incoming links:", (a, b) -> a + "\n" + b));
                                writer.println(c.getTopLinks(c.outLinkMaxHeap).stream()
                                        .reduce("\nTop " + c.rank + " outgoing links:", (a, b) -> a + "\n" + b));

                                writer.close();
                                return;
                            }
                        }
                    }
                }
                query = result.nextQuery();
            } while (result.hasNext());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter query: ");
        String querystr = scanner.nextLine();

        System.out.println("Enter total number of links to crawl: ");
        int links = scanner.nextInt();

        System.out.println("Enter maximum depth: ");
        int depth = scanner.nextInt();

        System.out.println("Enter rank limit of in/out links: ");
        int rank = scanner.nextInt();

        Extractor e = new Extractor();
        e.extract(querystr, links, depth, rank);

        scanner.close();
    }
}
