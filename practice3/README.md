# Twitter Topic crawler and analyzer

## Summary 

After getting tweets for a specific topic, the Extractor extracts all **external** links (that's not in twitter.com domain) and feed to the Crawler until the target links are crawled. The Crawler takes the links from tweets as seeds, parse webpages, get all the outgoing links, and crawl all the **non-image, non-video** outgoing links.

## Steps to run the program:

#### Compile the program: 

`mvn compile`

#### Run Crawler: 

`mvn exec:java -Dexec.mainClass="fayeoyaee.Extractor"`

Follow the instruction to enter <query hashtag>, <number of links>, <depth>, and <rank limits>

#### Extractor and Statistics Result: 

A Extractor will extract 20,000 links for query and compute:


- Number of unique links extracted 

- Frequecy distribution by domain

- Breakdown of links by type (text, image, video)

- Number of incoming and outgoing links for eachdcrawled page

- Top 25 pages with highest number of incoming and outgoing links


For example, here's a [summary](query_Trump_links_20000_depth_3.txt) for "#Trump" on 07/22/2018: