package fayeoyaee;

import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Crawling webpages starting from seeds, given total links and depth
 *
 */
public class Crawler {
    public static List<String> ImageFormats = Arrays.asList(".jpeg", ".gif", ".png", ".apng", ".svg", ".bmp", ".png");
    public static List<String> VideoFormats = Arrays.asList(".avi", ".flv", ".mpg", ".mpeg", ".mp4", ".mov", ".ogg",
            ".rm", ".rv", ".swf", ".wmv", ".webm");
    // These websites use dynamic loading, so it's hard to tell from html (either
    // from tags or from url)
    public static List<String> VideoDomains = Arrays.asList("https://youtube.com/watch?");

    public int target; // total links that will be crawled
    public int depth; // maximum depth of each url
    public int rank; // how many pages to retrieve for top in/out links
    public int crawled = 0;

    // We use a map here for extracting domain frequency distribution easily
    public Map<String, Set<String>> domainMap = new HashMap<>();

    // Here we assume only crawling text, image, audio, and video links, the sum of
    // which is the crawled urls
    enum LinkTypes {
        TEXT, IMAGE, AUDIO, VIDEO;
    }

    public Map<LinkTypes, Integer> linkTypeStatMap = new HashMap<>();

    // As we only need to return the top TopInlinkTarget urls, we can just use a
    // sorted map which maintains a certain size when inserting to it.
    private class Link {
        String url; 
        int cnt; 
        Link(String url, int cnt) {
            this.url = url;
            this.cnt = cnt;
        }
    }

    public PriorityQueue<Link> inLinkMaxHeap = new PriorityQueue<>((a, b) -> {
        return b.cnt - a.cnt;
    });

    public PriorityQueue<Link> outLinkMaxHeap = new PriorityQueue<>((a, b) -> {
        return b.cnt - a.cnt;
    });


    public Crawler(int links, int depth, int rank) {
        this.target = links;
        this.depth = depth;
        this.rank = rank;
    }

    // Assume the url is fully expanded url with www.xxxx.xxx as the domain
    private String getDomain(String url) {
        String ret = url.substring(url.indexOf("://") + 3).split("/")[0];
        // remove "www" prefix
        return ret.startsWith("www.") ? ret.substring(4) : ret;
    }

    private Link findLink(PriorityQueue<Link> pq, String url) {
        for (Link link : pq) {
            if (link.url.equals(url))
                return link;
        }
        return null;
    }

    // Update treemap and keep the size so we always store the top X items
    private void incLink(PriorityQueue<Link> pq, String url) {
        Link link;
        if ((link = findLink(pq, url)) != null) {
            pq.remove(link);
            pq.add(new Link(url, link.cnt+1));
        } else {
            pq.add(new Link(url,1));
        }
        // System.out.println("Inclink: pqsize="+pq.size()+" pqtopcnt="+pq.peek().cnt);
    }

    // Add a uri to generated datasets and update statistics
    // Return if links should be inserted again
    private boolean addUrl(String url, String parentUrl, LinkTypes linkType) {
        if (!url.startsWith("http")) return crawled<target;

        // System.out.println("Add: Adding "+url+" ("+linkType+") from "+parentUrl);
        // decide if url is unique by checking if it can be added to hashset
        // System.out.println("Add: domain="+getDomain(url));
        Set<String> urlSet;
        if ((urlSet = domainMap.getOrDefault(getDomain(url), new HashSet<>())).add(url)) {
            domainMap.put(getDomain(url), urlSet);
            // System.out.println("Add: Unique url");
            // only update link statistics if the url is new
            linkTypeStatMap.put(linkType, linkTypeStatMap.getOrDefault(linkType, 0) + 1);
            // System.out.println("Add: Increasing inlink "+url);
            incLink(inLinkMaxHeap, url);

            // if parentUrl is "", url is a seed url, no need to update in/out links
            if (!parentUrl.equals("")) {
                // System.out.println("Add: Increasing outlink "+parentUrl);
                incLink(outLinkMaxHeap, parentUrl);
            }
        }
        crawled++;

        return crawled < target;
    }

    /**
     * Check if the seed url is media
     */
    private boolean isSeedMedia(String seed_url) {
        if (Crawler.ImageFormats.stream().filter(fmt -> seed_url.endsWith(fmt)).count() > 0) {
            addUrl(seed_url, "", LinkTypes.IMAGE);
            return true;
        }
        if (Crawler.VideoFormats.stream().filter(fmt -> seed_url.endsWith(fmt)).count() > 0) {
            addUrl(seed_url, "", LinkTypes.VIDEO);
            return true;
        }
        if (Crawler.VideoDomains.stream().filter(dm -> seed_url.startsWith(dm)).count() > 0) {
            addUrl(seed_url, "", LinkTypes.VIDEO);
            return true;
        }
        return false;
    }

    /**
     * Parse the url and return links for further crawling
     */
    private List<String> getOutUrls(String url) {
        List<String> out_urls = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url).get();

            // Treat media as dead ends: only add to dataset but not crawl it recursively
            // (return empty out_urls)
            Elements media = doc.select("[src]");
            for (Element src : media) {
                if (src.tagName().equals("img")) {
                    // System.out.println("\nGetOutUrls: image url="+src.attr("abs:src"));
                    if (!addUrl(src.attr("abs:src"), url, LinkTypes.IMAGE))
                        return out_urls;
                } else if (src.tagName().equals("audio")) {
                    // System.out.println("\nGetOutUrls: audio url="+src.attr("abs:src"));
                    if (!addUrl(src.attr("abs:src"), url, LinkTypes.AUDIO))
                        return out_urls;
                } else if (src.tagName().equals("video") || src.tagName().equals("iframe")) {
                    // System.out.println("\nGetOutUrls: video url="+src.attr("abs:src"));
                    if (!addUrl(src.attr("abs:src"), url, LinkTypes.VIDEO))
                        return out_urls;
                }
            }

            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String href = link.attr("abs:href");
                // Add to queue to crawl next
                out_urls.add(href);
                // Update generated data
                // System.out.println("\nGetOutUrls: text url="+href);
                if (!addUrl(href, url, LinkTypes.TEXT))
                    return out_urls;

                // System.out.println(href);
            }

            // System.out.println("########links" + links);
            // System.out.println("########media" + media);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out_urls;
    }

    /**
     * Crawl from a seed url
     */
    public void crawl(String seed) {
        // Preprocess: return null if it's a image / video
        if (isSeedMedia(seed))
            return;

        // Add base to dataset
        if (!addUrl(seed, "", LinkTypes.TEXT)) 
            return;
        int d = 1;
        Queue<String> queue = new LinkedList<>();
        queue.add(seed);
        // Add seed to crawled_urls
        while (d <= depth && !queue.isEmpty()) {
            // System.out.println("\n\nCrawl: Depth "+d+" of seed "+seed);
            Queue<String> tmp = new LinkedList<>();
            for (int i = 0; i < queue.size() && crawled < target; i++) {
                String cur_url = queue.poll();
                List<String> out_urls = getOutUrls(cur_url);
                for (String out_url : out_urls) {
                    // System.out.println("Crawl: Adding "+out_url+" to queue");
                    tmp.add(out_url);
                }
            }
            queue = tmp;
            ++d;
        }
    }

    public List<String> getTopLinks(PriorityQueue<Link> maxHeap) {
        List<String> ret = new ArrayList<>();
        int cnt = 1;
        while (cnt <= rank && !maxHeap.isEmpty()) {
            Link l = maxHeap.poll();
            ret.add("#"+cnt+": "+l.url+" ("+l.cnt+")");
            cnt++;
        }
        // System.out.println("GetTopLinks: retsize="+ret.size());
        return ret;
    }

    public static void main(String[] args) {
        // Sample driver for crawling from seed urls
        Crawler c = new Crawler(2000, 2, 25);
        c.crawl("https://www.northwestern.edu/");
        c.crawl("https://vimeo.com/");
    }
}
