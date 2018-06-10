# A search engine built over vector space (tf-idf) model

## Code design

#### Indexing: 

1. Iterating each file and each doc to update term collection, docno collection, and term frequency map.
1. Index terms and docnos by their hash code. Use HashMap to store the mapping from hash code to term string or docno string.
1. Use Map<Integer, Map<Integer, Integer>> to store term frequency for each doc. Using two maps has several advantages: 
   1. Judging if term is already in the collection takes O(1).
   1. Updating term frequency (by looking for docHash in the term entry map) in the same doc takes O(1).
   1. Add new tf(t,d) takes O(1).
   1. Looking for tf(t,d) (during query) only takes O(1).

#### Querying:

1. Iterate over query tokens and all docs to calculate rank for each doc.
1. TF is stored in termFreqInDoc map, df is calculated by the size of term entry map.
1. Keep a priority queue of fixed size (50) to get the docnos ranking top 50.


## Steps to run the program:

#### Check memory 

Default memory assigned to maven is 3.5 GB. Change "-Xmx3584m" if you want to change the maximum memory. There's ~100,000 document and ~2000,000 terms in ap89_collection, so large memory is recommended.

#### Compile the program: 

`mvn compile`

#### Run Indexer and Query: 

`mvn exec:java -Dexec.mainClass="fayeoyaee.Indexer"`

#### Query result example:

![Example](QueryResultExample.png)