# StockNLP

StockNLP is a search engine and analysis tool for market trend news extracted from a pool of authorative Chinese stock analysis website like [cnFOL.com](http://sc.stock.cnfol.com/ggzixun/). 

A web crawler is implemented from scratch to connect to these websites, parse the html and selelect relevant information using [Jsoup](https://jsoup.org/). The crawler will scrape the past 3 month's data in a batch as the initial database, and run every day as a daemon process after the project is finished.

Parsed articles are fed into a NLP lexical parser implemented in [Hanlp](https://github.com/hankcs/HanLP). With the parsed words, we index the terms by building the term-document postings and write to a MongoDB database on [mlab](https://www.mlab.com/company/).

A query is parsed by NLP parser and the ranked documents are ranked in decreasing cosine similarity order using vector space model. In addition, StockNLP also provides hot word analysis service where terms with high document frequency specific to that day's collections are returned.  

# Note 

All the coding will be in Java using external dependencies Jsoup, Hanlp, and mlab. The crawler, indexer, query, and analyzer will be built from scratch. 

The project will first implement something that "just work", like implementing a simple crawler for limited number of websites, then try to adapt to big data, like adding the distributed computing feature to the crawler, optimize term-document matrix data storage, etc. 

# Implementation details 

### Search engine and database

Compared to NoSQL database, search engines implement functions like relevance retrieval, partial search (useful for autocompletion). ElasticSearch is a popular choice, which is backed up by classic IR [theories](https://www.elastic.co/guide/en/elasticsearch/guide/current/scoring-theory.html) like boolean model, tf-idf, vector model. Additionally, it added query normalization factor (to make results from different queries comparable; doesn't work well), coordination factor (rewarding documents that contain a higher percentage of query terms; proportional to how many query terms in the document), filed-length norm (give higher weights if the term appears in a shorter term; nord(d) = 1 / sqrt(numTermsInField)). Inaddition to index-time boosting, they also add query-time boosting (eg, giving more weights to recent docs). In the end, scores are calculated as:

```
score(q,d)  =  
            queryNorm(q)  
          · coord(q,d)    
          · ∑ (           
                tf(t in d)   
              · idf(t)²      
              · t.getBoost() 
              · norm(t,d)    
            ) (t in q)    
```

However, it needs to be mentioned that ElasticSearch is not supposed to be the main database, as it's not optimized for inserting, maintaining, and storing. Furthermore, there's no cloud platform providing free ES database service for personal use in the same way as mlab for MongoDB. Also note that as a real-time stock news analyzer for traders, we focus on hot topics, stock trends in a short time window. That means we don't need to include old news in ES, but we'd like to keep them as records in MongoDB. So we integrated the use of MongoDB with ES, only synchronizing latest XX day's data to ES at a scheduled time everyday (by creating a cron job in Linux or plist in MacOS).

Another challenge of using ElasticSearch is how to parse Chinese. As a word can be formed by either a single character or multiple characters in Chinese, ElasticSearch needs to parse Chinese words correctly first. However, this could be a hard job, even for spcialized NLP parser. Smart Chinese Analysis Plugin is a possible solution, but it doesn't provide customized dictionary so there's no way to mark stock names as single words, 

Given that the core algorithm of ES is not hard to implement (basically go through all the documents and calculate some statistics), and using ES causes a lot of trouble, we build our ranking algorithm by ourselves.

### Ranking algorithm

#### Identify 'Information Need'

We want to provide traders with the following supplimentary statistic to assist trading:

- The most popular topics

- Ranked documents for a search word (eg, stock name, hot topic, important person, etc)

- Market reaction for a stock (positive/negative descriptions)

#### Pipeline

HanLP, a NLP library for Chinese language with customized dictionary (eg, for stock names, positive/negative words) is used to parse and identify target words. The crawler runs as a daemon process everyday and put the tagged article with some document-level metadata (timestamp, title, url, number of positive/negative words, etc) into MLab (AWS MongoDB instance). A article is tagged with the following information:

- Highlighted context words which surrounds a stock name. A context is formed by extracting X words before and after a stock name. If multiple stock names appear together, we expand the context. A context is supposed to capture the most important parts of the article and we'll give higher weights (boots) to words within the context.

An indexer is called to build term-document frequency postings, which reflects which terms should be highted in which doc, after daily crawler and store in MLab. A ranker retrieves information from MLab at the time of request, apply weights to terms and docs, and return a ranked list of documents related to the search. 