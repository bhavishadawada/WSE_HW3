2.1.1 

G = λS + (1-λ)(1/N)1

The power iteration method is an iterative process used to approximate the Pagerank vector. We can model the process as a random walk on graphs. In fact, one needs only to compute the first couple of interates to get a good approximation of the Pagerank vector, for the only reason that the web graph is sparse. 

The parameter λ plays a crucial role in the computation of Pagerank. If λ=1, then G=S,this means we are working with the original hyperlink structure of the web. If λ=0, then G=(1/N)1 we lost all the original hyperlink structure of the internet. Therefore, we should choose λ that close to 1 so that the hyperlink structure of the internet is weighted more heavily into the computation.

However, when λ is close to 1 the convergence of the power interation method is proved to be very slow. So as a compromise of these two competing interests, Brin and Page, in their original paper, choose λ=0.85. 

Thus for best approximation, among the 4 settings, we choose two iterations with λ = 0.90.