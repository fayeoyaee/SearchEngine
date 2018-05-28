# Steps to run the program:

### If you have new dataset, replace /src/main/resources/AP_DATA/ap89_collection folder.

### If you have new query file, rename and replace /src/main/resources/AP_DATA/query_desc.51-100.short.txt.

### If you have new stoplist file, rename and replace /src/main/resources/AP_DATA/stoplist.txt.

### Run elasticsearch on port 9300

### Compile the program: `mvn compile`

### Run Indexer: 

`mvn exec:java -Dexec.mainClass="com.fayeoyaee.hw1.Indexer" -Dexec.cleanupDaemonThreads=false`

### Run Query: 

`mvn exec:java -Dexec.mainClass="com.fayeoyaee.hw1.Query" -Dexec.cleanupDaemonThreads=false`
