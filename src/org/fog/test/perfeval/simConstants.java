package org.fog.test.perfeval;

public class simConstants {
	
	static final boolean CLOUD = false;
	static final boolean crit = true;
	static final int numOfGateways = 1;
	static final int numOfEndDevPerGateway = 16;
	static final double sensingInterval = 5; 
	static final double minInterval =2;
	static final double maxInterval =8;
	
	static final double secWeight = 0;
	static final double critWeight = 1-secWeight;
	
	static final double posCons =1;
	static final double negCons = 1;
	
	
    static final int secUnionLimitGW =4 ;
	
	static final int secUnionLimitProxy = 4;
	
	static final int secUnionLimitCloud = 6;
	
	
	
	
	static final int secUnionLimit = 4;
	
	
	
	
	
	static final protected int critUsagePercentage[] = {20,30,50};
	
	static final protected double critPercent =0.5;
}
