package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

class Spearman{

	static public void main(String[] args) throws Exception{
		String pageRankPath = args[0];
		String numViewPath = args[1];
		System.out.println(pageRankPath);
		System.out.println(numViewPath);

		int docNum = 10;
		double[] pageRank = new double[docNum];
		double[] numView  = new double[docNum];
		readRank(pageRankPath, pageRank);
		readRank(numViewPath, numView);
		System.out.println(cmpRank(numView, pageRank));
	}

	static public void readRank(String path, double[] rank ) throws Exception{
		File file = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;

		while((line = br.readLine()) != null){
			String[] arr = line.split(" ");
			int docId = Integer.parseInt(arr[0]);
			rank[docId] = Integer.parseInt(arr[1]);
		}

		br.close();
	}
	
	static public double cmpRank(double[] rank0, double[] rank1){
		double tho = 0;
		double w = 0;
		double z = ((double)rank0.length + 1.0)/2;
		for(int i = 0; i < rank0.length; i++){
			tho += (rank0[i] - z)*(rank1[i] - z);
			w += Math.pow((rank0[i] - z), 2);
		}
		return tho/(w*w);
	}
}