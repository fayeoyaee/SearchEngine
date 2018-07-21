# Single-threed Twitter Topic crawler and analyzer

## Steps to run the program:

#### Compile the program: 

`mvn compile`

#### Run Crawler: 

`mvn exec:java -Dexec.mainClass="fayeoyaee.Crawler"`

Follow the instruction to enter <query hashtag>, <number of links> and <depth>

#### Extractor and Statistics Result: 

A Extractor will extract 20,000 links for query and compute:

- Number of unique links extracted 

- Frequecy distribution by domain

- Breakdown of links by type (text, image, video)

- Number of incoming and outgoing links for eachdcrawled page

- Top 25 pages with highest number of incoming and outgoing links

For example, here's a summary for "#acai bowl":

![Result](Acaibowl.png)

For example, here's a summary for "#chinese tech company ZTE":

![Result](CTechZTE.png)

