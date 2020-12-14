package org.knapsack;

public class Item {
	
	public String name;
	public int value;
	public int weight;
	
	public Item(String name, int value, int weight) {
		this.name = name;
		this.value = value;
		this.weight = weight;
	}
	
	public String str() {
		return name + " [value = " + value + ", weight = " + weight + "]";
	}
	
	public String getItemName()
	{
		return this.name;
	}
	
	
	public void setItemName(String nm)
	{
		this.name = nm;
	}
	
	public void setItemValue(int val)
	{
		this.value = val;
	}
	
	public int getItemValue()
	{
		return this.value;
	}
	
	public int getItemWeight()
	{
		return this.weight;
	}
	
	public void setItemWeight(int wt)
	{
		 this.weight = wt;
	}

}
