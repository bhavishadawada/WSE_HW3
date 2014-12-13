rm -f ./data/prf/prf*.tsv
i=0
while read q ; do
i=$((i + 1));
prfout=prf-$i.tsv;
curl "http://localhost:25811/prf?query=$q&ranker=comprehensive&num=10&numterms=5" > ./data/prf/$prfout;
echo $q:$prfout > prf.tsv
done < queries.tsv
javac -cp lib/jsoup-1.8.1.jar:lib/org.apache.commons.io.jar:lib/apache-commons-lang.jar:lib/lucene-analyzers-3.6.2.jar:lib/lucene-core-3.6.2.jar src/edu/nyu/cs/cs2580/*.java
java -cp src edu.nyu.cs.cs2580.Bhattacharyya ./data/prf/ ./data/index/qsim.tsv
