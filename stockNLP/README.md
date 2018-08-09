# StockNLP

StockNLP is a search engine and analysis tool for market trend news extracted from a pool of authorative Chinese stock analysis website like [cnFOL.com](http://sc.stock.cnfol.com/ggzixun/). 

A web crawler is implemented from scratch to connect to these websites, parse the html and selelect relevant information using [Jsoup](https://jsoup.org/). The crawler will scrape the past 3 month's data in a batch as the initial database, and run every day as a daemon process after the project is finished.

Parsed articles are fed into a NLP lexical parser implemented in [Hanlp](https://github.com/hankcs/HanLP). With the parsed words, we index the terms by building the term-document postings and write to a MongoDB database on [mlab](https://www.mlab.com/company/).

A query is parsed by NLP parser and the ranked documents are ranked in decreasing cosine similarity order using vector space model. In addition, StockNLP also provides hot word analysis service where terms with high document frequency specific to that day's collections are returned.  