# Twitter focused crawler 

## Summary 

After getting tweets for a specific topic, the Extractor extracts all **external** links (that's not in twitter.com domain) and feed to the Crawler until the target links are crawled. The Crawler takes the links from tweets as seeds, parse webpages, get all the outgoing links, and crawl all the **non-image, non-video** outgoing links.

## Compile the program: 

`mvn compile`

## Run tweet extractor (which will call the crawler): 

`mvn exec:java -Dexec.mainClass="fayeoyaee.Extractor"`

Follow the instruction to enter query string, link limits, depth, and rank limits.

## Statistics Result: 

The crawler will generate the following statistics:

- Number of unique links extracted 

- Frequecy distribution by domain

- Breakdown of links by type (text, image, video)

- Top 25 pages with highest number of incoming and outgoing links

For example, here's a [summary](query_Trump_links_20000_depth_3.txt) which queries "#Trump" on 07/22/2018:

Note that we can't calculate PageRank given the above statistics alone. But if we further know the incoming links of each page, rather than just the number of the incoming links, we can calculate simple PageRank in the following steps:

- Calculate the initial probability (at t = 0) of each page by dividing 1 by the number of total unique pages

- Update each page's PageRank at time t+1: Iterate all the incoming links of a certain page. For each of the incoming page, add up it's contribution to the target page. The contritution is calculated by the incoming page's PageRank at time t divided by the number of it's outgoing pages

- Do the above update process until the PageRank difference at t and t+1 is smaller than a certain value 