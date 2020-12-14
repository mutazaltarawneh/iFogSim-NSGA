package org.knapsack;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Solution {
	
	// list of items to put in the bag to have the maximal value
	public List<Item> items;
	// maximal value possible
	public double value;
	
	public Solution(List<Item> items, double value) {
		this.items = items;
		this.value = value;
	}
	
	public void display() {
		
		int sum=0;
		
		
		if (items != null  &&  !items.isEmpty()){
			
			for (Item item : items) {
				sum = sum + item.weight;
			}
			System.out.println("\nKnapsack solution");
			System.out.println("Value = " + value + "  Weight= " + sum);
			System.out.println("Items to pick :");
			
		
			for (Item item : items) {
				System.out.println("- " + item.str());
			}
		}
	}
	
	public List<String> getSolutionItemNames()
	{
		List<String> lst = new ArrayList<String>();
         if (items != null  &&  !items.isEmpty()){
			
			for (Item item : items) {
				lst.add(item.getItemName());
			}
		}
         
         return lst;
		
	}
	
	public List<Item> getKnapsackSolutionItems()
	{
		return this.items;
	}
	
	

}