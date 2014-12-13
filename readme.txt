 == build code ==
javac -cp lib/jsoup-1.8.1.jar:lib/org.apache.commons.io.jar:lib/apache-commons-lang.jar:lib/lucene-analyzers-3.6.2.jar:lib/lucene-core-3.6.2.jar src/edu/nyu/cs/cs2580/*.java

 == mining ==
java -cp src:lib/jsoup-1.8.1.jar:lib/org.apache.commons.io.jar:lib/apache-commons-lang.jar:lib/lucene-analyzers-3.6.2.jar:lib/lucene-core-3.6.2.jar edu.nyu.cs.cs2580.SearchEngine --mode=mining --options=conf/engine.conf

 == index ==
java -cp src:lib/jsoup-1.8.1.jar:lib/org.apache.commons.io.jar:lib/apache-commons-lang.jar:lib/lucene-analyzers-3.6.2.jar:lib/lucene-core-3.6.2.jar edu.nyu.cs.cs2580.SearchEngine --mode=index --options=conf/engine.conf

 == serve ==
java -cp src:lib/jsoup-1.8.1.jar:lib/org.apache.commons.io.jar:lib/apache-commons-lang.jar:lib/lucene-analyzers-3.6.2.jar:lib/lucene-core-3.6.2.jar -Xmx512m edu.nyu.cs.cs2580.SearchEngine --options=conf/engine.conf --mode=serve --port=25811



2.1.1 Computing PageRank


java -cp src:lib/jsoup-1.8.1.jar:lib/org.apache.commons.io.jar:lib/apache-commons-lang.jar:lib/lucene-analyzers-3.6.2.jar:lib/lucene-core-3.6.2.jar edu.nyu.cs.cs2580.SearchEngine --mode=mining --options=conf/engine.conf

G = λS + (1-λ)(1/N)1

The power iteration method is an iterative process used to approximate the Pagerank vector. We can model the process as a random walk on graphs. In fact, one needs only to compute the first couple of interates to get a good approximation of the Pagerank vector, for the only reason that the web graph is sparse. 

The parameter λ plays a crucial role in the computation of Pagerank. If λ=1, then G=S,this means we are working with the original hyperlink structure of the web. If λ=0, then G=(1/N)1 we lost all the original hyperlink structure of the internet. Therefore, we should choose λ that close to 1 so that the hyperlink structure of the internet is weighted more heavily into the computation.

However, when λ is close to 1 the convergence of the power interation method is proved to be very slow. So as a compromise of these two competing interests, Brin and Page, in their original paper, choose λ=0.85. 

Thus for best approximation, among the 4 settings, we choose two iterations with λ = 0.90.

3 Spearamn

java -cp src edu.nyu.cs.cs2580.Spearman data/index/pageRank.txt data/index/numViews.txt

Results:
iteration 1 lambda 0.1
Spearman 0.4544572295363984

iteration 2 lambda 0.1
Spearman 0.4516135414108263

iteration 1 lambda 0.9
Spearman 0.4544571521738433

iteration 2 lambda 0.9
Spearman 0.4559922663694335

Using Spearman correlation, our experiment shows that PageRank predicts website traffic(NumViews) with somewhat surprising strength. A quick search on the Internet shows that, as reported by Quantcast, monthly page views, visits, and unique visitors are all significantly correlated with Pagerank.

4.1 PRF
curl "http://localhost:25811/prf?query="yahoo"&ranker=comprehensive&num=10&numterms=5"

4.2 Query Similarity

Bhattacharyya
sh prf.sh

