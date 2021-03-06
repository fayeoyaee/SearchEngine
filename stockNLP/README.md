# StockNLP

StockNLP is a search engine and analysis tool for market trend news extracted from a pool of authorative Chinese stock analysis website like [cnFOL.com](http://sc.stock.cnfol.com/ggzixun/). 

A web crawler is implemented from scratch to connect to these websites, parse the html and selelect relevant information using [Jsoup](https://jsoup.org/). The crawler will scrape the past 3 month's data in a batch as the initial database, and run every day as a daemon process after the project is finished.

Parsed articles are fed into a NLP lexical parser implemented in [Hanlp](https://github.com/hankcs/HanLP). With the parsed words, we index the terms by building the term-document postings and write to a MongoDB database on [mlab](https://www.mlab.com/company/).

A query is parsed by NLP parser and the ranked documents are ranked in decreasing cosine similarity order using vector space model. In addition, StockNLP also provides hot word analysis service where terms with high document frequency specific to that day's collections are returned.  

# Architecture: 

![pipeline](https://github.com/fayeoyaee/SearchEngine/blob/master/stockNLP/report/StockNLP_Pipeline.png)

# Run the program:

`mvn exec:java -Dexec.mainClass="com.fayeoyaee.Crawler" -Dexec.args="<n_pages> <db_username> <db_password>"` to crawl the latest 10 pages from cnFOL.com (if run as a daily daemon, give it a small value like "2")

`mvn exec:java -Dexec.mainClass="com.fayeoyaee.Query" -Dexec.args="<mm-dd> <db_username> <db_password>"` to calculate document frequecies for a certain data (eg, "08-09")

`mvn exec:java -Dexec.mainClass="com.fayeoyaee.Server" -Dexec.args="<db_username> <db_password>"` to start a server listening on localhost:9090

`cd client; yarn start` to start the react client

# Detailed report 

Detailed design and implementation can be found [here](https://github.com/fayeoyaee/SearchEngine/blob/master/stockNLP/report/stockNLP_project_report.pdf)
