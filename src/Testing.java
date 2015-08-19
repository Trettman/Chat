import java.util.ArrayList;
import java.util.Stack;

import org.json.JSONException;
import org.json.JSONObject;

public class Testing{
	
	static String[] test;
	static String[] test2;
	static JSONObject potatis;

	public static void main(String[] args) throws JSONException{
	
		int x = 1523;
		int y = 15233;
		
		System.out.println("x: " + x);
		System.out.println("y: " + y);
		
		x = x^y;
		y = x^y;
		x = x^y;
		
		System.out.println("x: " + x);
		System.out.println("y: " + y);
		
		ArrayList<Integer> testing = new ArrayList<Integer>();
		
		testing.add(5);
		testing.add(9);
		testing.add(1);
		testing.add(3);
		testing.add(2);	
		
		sort(testing);
		
		for(int out : testing)
			System.out.println(out);
	}
	
	public static void sort(ArrayList<Integer> arrayList){
		
		while(true){

			int count = 0;
			
			for(int i = 0; i < arrayList.size() - 1; i++){
				
				if(arrayList.get(i) > arrayList.get(i + 1)){
					arrayList.set(i, arrayList.get(i)^arrayList.get(i + 1));
					arrayList.set(i + 1, arrayList.get(i)^arrayList.get(i + 1));
					arrayList.set(i, arrayList.get(i)^arrayList.get(i + 1));				
				}
				else
					count++;
			}
			if(count == arrayList.size() - 1)
				break;
		}
	}
}
