package org.fog.utils.distribution;

import java.util.Random;

public class UniformDistribution extends Distribution{

	
	 public static final Random RNG = new Random(System.currentTimeMillis());
	private double min;
	private double max;
	
	public UniformDistribution(double min, double max){
		super();
		setMin(min);
		setMax(max);
	}
	
	@Override
	public double getNextValue() {
		
		//System.out.println("Max is:  " + getMax());
		//System.out.println("Min is:  " + getMin());
		//System.out.println("Random is: " + getRandom());
		double range = getMax() - getMin();
		double fraction = (range * RNG.nextDouble());
		return (fraction + getMin());
		
		
		
	//	return getRandom().nextDouble()*(getMax()-getMin())+getMin();
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}
	
	@Override
	public int getDistributionType() {
		return Distribution.UNIFORM;
	}

	@Override
	public double getMeanInterTransmitTime() {
		return (min+max)/2;
	}

}
