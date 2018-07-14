# StockNLP

StockNLP is a search engine and analysis tool for market trend news extracted from a pool of authorative Chinese stock analysis website like [cnFOL.com](http://sc.stock.cnfol.com/ggzixun/). 

A web crawler is implemented from scratch to connect to these websites, parse the html and selelect relevant information using [Jsoup](https://jsoup.org/). The crawler will scrape the past 3 month's data in a batch as the initial database, and run every day as a daemon process after the project is finished.

Parsed articles are fed into a NLP lexical parser implemented in [Hanlp](https://github.com/hankcs/HanLP). With the parsed words, we index the terms by building the term-document postings and write to a MongoDB database on [mlab](https://www.mlab.com/company/).

A query is parsed by NLP parser and the ranked documents are ranked in decreasing cosine similarity order using vector space model. In addition, StockNLP also provides hot word analysis service where terms with high document frequency specific to that day's collections are returned.  

# Note 

All the coding will be in Java using external dependencies Jsoup, Hanlp, and mlab. The crawler, indexer, query, and analyzer will be built from scratch. 

The project will first implement something that "just work", like implementing a simple crawler for limited number of websites, then try to adapt to big data, like adding the distributed computing feature to the crawler, optimize term-document matrix data storage, etc. 
