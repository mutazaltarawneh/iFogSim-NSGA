package org.knapsack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.knapsack.Solution;

public class Knapsack {
	
  // items of our problem
  private List<Item> items;
  protected Map<String,Integer> sackMap;
  // capacity of the bag
  private int capacity;

  public Knapsack(List<Item> items, int capacity) {
    this.items = items;
    this.capacity = capacity;
    this.sackMap = new HashMap<String,Integer>();
  }

  public void display() {
    if (items != null  &&  items.size() > 0) {
      System.out.println("Knapsack problem");
      System.out.println("Capacity : " + capacity);
      System.out.println("Items :");

      for (Item item : items) {
        System.out.println("- " + item.str());
      }
    }
  }

 
  
  // we write the solve algorithm
  public Solution solve() {
    int NB_ITEMS = items.size();
    // we use a matrix to store the max value at each n-th item
    double[][] matrix = new double[NB_ITEMS + 1][capacity + 1];

    // first line is initialized to 0
    for (int i = 0; i <= capacity; i++)
      matrix[0][i] = 0;

    // we iterate on items
    for (int i = 1; i <= NB_ITEMS; i++) {
      // we iterate on each capacity
      for (int j = 0; j <= capacity; j++) {
        if (items.get(i - 1).weight > j)
          matrix[i][j] = matrix[i-1][j];
        else
          // we maximize value at this rank in the matrix
          matrix[i][j] = Math.max(matrix[i-1][j], matrix[i-1][j - items.get(i-1).weight] 
				  + items.get(i-1).value);
      }
    }

    double res = matrix[NB_ITEMS][capacity];
    int w = capacity;
    List<Item> itemsSolution = new ArrayList<>();

    for (int i = NB_ITEMS; i > 0  &&  res > 0; i--) {
      if (res != matrix[i-1][w]) {
        itemsSolution.add(items.get(i-1));
       // items.remove(i-1);
        // we remove items value and weight
        res -= items.get(i-1).value;
        w -= items.get(i-1).weight;
      }
    }

    if (itemsSolution !=null && itemsSolution.size()>0)
    {
    	for (Item itm: itemsSolution)
    	{
    		this.sackMap.put(itm.getItemName(), 1);
    		//System.out.println("Item  " + itm.getItemName());
    	}
    }
    
    
    return new Solution(itemsSolution, matrix[NB_ITEMS][capacity]);
  }
  
  public List<Item> getKnapsackItems()
  {
	  return this.items;
  }
  
  public Map<String,Integer> getKnapSackMap()
  {
	  return this.sackMap;
  }

  public static void main(String[] args) {
    // we take the same instance of the problem displayed in the image
    List<Item> items = new ArrayList<Item>();
    	
    items.add(new Item("Elt1", 4, 12));
    items.add(new Item("Elt2", 2, 1));
    items.add(new Item("Elt3", 2, 2));
    items.add(new Item("Elt4", 1, 1));
    items.add( new Item("Elt5", 10, 4));
    	
	                 

    Knapsack knapsack = new Knapsack(items, 15);
    knapsack.display();
    Solution solution = knapsack.solve();
    solution.display();
    //knapsack.displayRemItems();
  }
}
	

