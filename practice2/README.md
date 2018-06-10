# A search engine built over Tf-Idf model

## Code design

1. Documents are indexed by natural number 1 - N. The mapping from document index to document number string is stored in an ArrayList<String> because we only need to do add and sequential get operation, which are both O(1) for ArrayList.
1. Terms are indexed by it's hash code. The mapping from hash code to term string is stored in an HashMap<Integer, String>, 

84678 docs - doc dic array string  = 84678 * 16 bytes = 1.3 mb
Indexing: 
-----------------------------------------------
When adding the first document: 
index document 
index term
add (term document frequecy) to some data struct

When adding the second document:
index new document
index term
find term index 
if the term already exits, get the term entry 
add (document frequency) information to the term entry
change the term entry


Query:
------------------------------------------------
get the size of document to get N
index term
for each term in query
get term entry 
for each doc 
get tf
calculate how many (document, frequency) not 0 to get idf
(for each term)
get the top 50 docid (total = doc size)
get the docno string by looking at doc idx table

(Store tf-idf if necessary)




## Steps to run the program:

* Compile the program: 

`mvn compile`

* Run Indexer and Query: 

`mvn exec:java -Dexec.mainClass="fayeoyaee.Indexer"`

* Query result example:

![Example](QueryResultExample.png)