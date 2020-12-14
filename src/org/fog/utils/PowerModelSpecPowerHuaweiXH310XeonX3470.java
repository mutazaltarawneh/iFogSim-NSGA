package org.fog.utils;

public class PowerModelSpecPowerHuaweiXH310XeonX3470 extends FogSpecPowerModel{
	
	
	/** 
     * The power consumption according to the utilization percentage. 
     * @see #getPowerData(int) 
     */
private final double[] power = { 109, 166, 188, 210, 239, 275, 314, 362, 399, 435, 472 };

@Override
protected double getPowerData(int index) {
	return power[index];
}

}
