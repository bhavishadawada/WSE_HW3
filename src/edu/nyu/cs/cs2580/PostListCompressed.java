package edu.nyu.cs.cs2580;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.bind.DatatypeConverter;

public class PostListCompressed  implements Serializable{
	private static final long serialVersionUID = 2244270778601225258L;
	String term;
	byte[] data;

	PostListCompressed(){
	}

	PostListCompressed(String term, byte[] data){
		this.term = term;
		this.data = data;
	}

	// before fish: (1,2,[2,4]), (2,3,[7,18,23]), (3,2,[2,6]), (4,2,[3,13]) 
	// after  fish: (1,2,[2,2]), (1,3,[7,11,5]), (1,2,[2,4]), (1,2,[3,10]) 

	PostListCompressed(PostListOccurence postLs){
		this.term = postLs.term;
		ArrayList<Integer> delta = new ArrayList<Integer>();

		TreeMap<Integer, ArrayList<Integer>> pdata = postLs.data;

		Integer prevDocId = 0;
		for(Entry<Integer, ArrayList<Integer>> entry : pdata.entrySet()) {
			Integer docId = entry.getKey();
			ArrayList<Integer> occurList = entry.getValue();

			delta.add(docId - prevDocId);
			prevDocId = docId;
			
			delta.add(occurList.size());
			
			delta.addAll(toDelta(occurList));
			
		}
		this.data = byteToArr(vbyteCompressLs(delta));
	}
	
	PostListOccurence deCompress(){
		TreeMap<Integer, ArrayList<Integer>> dataOut = new TreeMap<Integer, ArrayList<Integer>>();
		ArrayList<Integer> delta = vbyteDeCompress(this.data, 0, this.data.length);
		int prevDocId = 0;
		int i = 0;
		while(i < delta.size()){
			int docId = prevDocId + delta.get(i);
			prevDocId = docId;
			i++;

			int occNum = delta.get(i);
			i++;

			ArrayList<Integer> occList = deltaRev(delta, i, occNum);
			i+=occNum;

			dataOut.put(docId, occList);
		}
		
		return new PostListOccurence(term, dataOut);
	}
	
	public ArrayList<Integer> toDelta(ArrayList<Integer> in){
		ArrayList<Integer> out = new ArrayList<Integer>();
		if(!in.isEmpty()){
			out.add(in.get(0));
			for(int i = 1; i < in.size(); i++){
				out.add(in.get(i) - in.get(i-1));
			}
		}
		return out;
	}

	public ArrayList<Integer> deltaRev(ArrayList<Integer> in, int start, int size){
		ArrayList<Integer> out = new ArrayList<Integer>();
		if(size > 0){
			out.add(in.get(start));
			for(int i = 1; i < size; i++){
				out.add(in.get(start + i) + out.get(i-1));
			}
		}
		return out;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(term + "::");
		sb.append(data);
		return sb.toString();
	}
	
	public static ArrayList<Byte> vbyteCompressLs(ArrayList<Integer> numLs){
		ArrayList<Byte> bLs = new ArrayList<Byte>();
		for(int num : numLs){
			bLs.addAll(vbyteCompress(num));
		}
		return bLs;
	}

	public static ArrayList<Byte>  vbyteCompress(int num){
		ArrayList<Byte> bLs = new ArrayList<Byte>();
		int mask = (1<<7)-1;
		do{
			Byte b = (byte)(num & mask); 
			bLs.add(b);
			num = num>>7;
		}while(num != 0);

		//System.out.println(bLs.size());
		//add one bit at the first byte;
		bLs.set(0, (byte) (bLs.get(0) | (1<<7)));

		// from most significant byte to last byte
		Collections.reverse(bLs);
		return bLs;
	}
	
	public static ArrayList<Integer> vbyteDeCompress(byte[] bLs, int start, int end){
		ArrayList<Integer> result = new ArrayList<Integer>();
		int flag = (1<<7);
		int mask = (1<<7)-1;
		int i = start;
		int num = 0;
		while(i < end){
			if((bLs[i] & flag) == 0){
				//System.out.println("byte " + bLs[i]);
				// last byte
				num = (num << 7) + bLs[i];
			}
			else{
				//System.out.println("last byte " + (bLs[i]&mask));
				num = (num << 7) + (bLs[i]&mask);
				result.add(num);
				num = 0;
			}
			i++;
		}
		return result;
	}
	
	public static byte[] byteToArr(ArrayList<Byte> bLs){
		byte[] ret = new byte[bLs.size()];
		for(int i = 0; i < bLs.size(); i++){
			ret[i] = bLs.get(i);
		}
		return ret;
	}

	static public void main(String[] args){
		String line = "rayhan::398:[10239, 13927]  3549:[3393, 3549, 5733]  3554:[692]  4036:[2795]  4047:[5515]  4228:[1283, 4273, 4309]  6026:[4548]  7796:[11060]";
		IndexerInvertedOccurrence index = new IndexerInvertedOccurrence();
		PostListOccurence po = index.buildPostLs(line);
		System.out.println(po);
		
		PostListCompressed pc = new PostListCompressed(po);
		System.out.println(pc);
		
		System.out.println(pc.deCompress());
		
		int[] arr = {6, 130, 20000};
		for(int num : arr){
			byte[] bLs = byteToArr(vbyteCompress(num));
			String hex = bytesToHex(bLs);
			System.out.println(hex); // prints "7F0F00"
			System.out.println(vbyteDeCompress(bLs, 0, bLs.length));
		}
		
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
}
