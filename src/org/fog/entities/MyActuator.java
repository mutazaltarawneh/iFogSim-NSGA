package org.fog.entities;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppLoop;
import org.fog.application.MyApplication;
import org.fog.utils.FogEvents;
import org.fog.utils.GeoLocation;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;

public class MyActuator extends SimEntity{

	private int gatewayDeviceId;
	private double latency;
	private GeoLocation geoLocation;
	private String appId;
	private int userId;
	private String actuatorType;
	private MyApplication app;
	
	protected int numRecievedTuples;
	
	public MyActuator(String name, int userId, String appId, int gatewayDeviceId, double latency, GeoLocation geoLocation, String actuatorType, String srcModuleName) {
		super(name);
		this.setAppId(appId);
		this.gatewayDeviceId = gatewayDeviceId;
		this.geoLocation = geoLocation;
		setUserId(userId);
		setMyActuatorType(actuatorType);
		setLatency(latency);
		setNumRecievedTuples(0);
	}
	
	public MyActuator(String name, int userId, String appId, String actuatorType) {
		super(name);
		this.setAppId(appId);
		setUserId(userId);
		setMyActuatorType(actuatorType);
		setNumRecievedTuples(0);
	}

	@Override
	public void startEntity() {
		sendNow(gatewayDeviceId, FogEvents.ACTUATOR_JOINED, getLatency());
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.TUPLE_ARRIVAL:
			processTupleArrival(ev);
			break;
		}		
	}

	private void processTupleArrival(SimEvent ev) {
		Tuple tuple = (Tuple)ev.getData();
		Logger.debug(getName(), "Received tuple "+tuple.getCloudletId()+"on "+tuple.getDestModuleName());
		
	//System.out.println(this.getName() +"   " + tuple.getCloudletId() + "    " +tuple.getTupleType());
		
		//if (tuple.getDirection() ==Tuple.ACTUATOR)
		//{
		//this.setNumRecievedTuples(this.getNumRecievedTuples()+1);
		//}
	//	if (this.getName().equals("a-0-1"))
	//	{
	//	System.out.println(getName()+ "   Received tuple "+tuple.getCloudletId()+"  on "+tuple.getDestModuleName());
	//	}
		
		String srcModule = tuple.getSrcModuleName();
		String destModule = tuple.getDestModuleName();
		MyApplication app = getApp();
		
		for(AppLoop loop : app.getLoops()){
			if(loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)){
				
				//System.out.println("Tuple:   " + tuple.getActualTupleId() + " Original Src Device:   " + tuple.getActualSrcId());
				
				
				Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				if(startTime==null)
					break;
				
			//	if (tuple.getTupleType().equals("Response") && ev.getSource() == this.getGatewayDeviceId());
				setNumRecievedTuples(getNumRecievedTuples()+1);
				
				
				if(!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())){
					TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
					TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
				}
				double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
				int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
				double delay = CloudSim.clock()- TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				
				
				MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(this.getGatewayDeviceId());

			//	System.out.println("@  " + CloudSim.clock() +"   Actuator:  " + this.getName() + "  Connected to: " + dv.getName() + "  Revieved Tuple:    " + tuple.getActualTupleId() + "  Emitted From: " + TimeKeeper.getInstance().getTupleIdToDeviceMap().get(tuple.getActualTupleId()) + "  @  " + TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId()));
                updateEndDeviceDelay(tuple, delay);
                updateCoreDeviceDelay(tuple, delay);
                updateCriticalityTypleDelay(tuple,delay);
				
				TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
				
				//System.out.println(" My Actuator:  " + " Remaining Emit Times:   " + TimeKeeper.getInstance().getEmitTimes().size());
				
				
				double newAverage = (currentAverage*currentCount + delay)/(currentCount+1);
				TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
				TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount+1);
				break;
			}
		}
	}

	@Override
	public void shutdownEntity() {
		
	}

	public int getGatewayDeviceId() {
		return gatewayDeviceId;
	}

	public void setGatewayDeviceId(int gatewayDeviceId) {
		this.gatewayDeviceId = gatewayDeviceId;
	}

	public GeoLocation getGeoLocation() {
		return geoLocation;
	}

	public void setGeoLocation(GeoLocation geoLocation) {
		this.geoLocation = geoLocation;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getMyActuatorType() {
		return actuatorType;
	}

	public void setMyActuatorType(String actuatorType) {
		this.actuatorType = actuatorType;
	}

	public MyApplication getApp() {
		return app;
	}

	public void setApp(MyApplication app) {
		this.app = app;
	}

	public double getLatency() {
		return latency;
	}

	public void setLatency(double latency) {
		this.latency = latency;
	}
	
	public void setNumRecievedTuples(int num)
	{
		this.numRecievedTuples = num;
	}

	public int getNumRecievedTuples()
	{
		return this.numRecievedTuples;
	}
	
	public void updateEndDeviceDelay(Tuple tuple, double delay)
	{
		
		String name = TimeKeeper.getInstance().getTupleIdToDeviceMap().get(tuple.getActualTupleId());
		
		if(!TimeKeeper.getInstance().getEndDeviceToDelayMap().containsKey(name)){
			TimeKeeper.getInstance().getEndDeviceToDelayMap().put(name, delay);
			TimeKeeper.getInstance().getEndDeviceNumRetiredTuples().put(name, 1);
			
		} else{
			double currentAverage = TimeKeeper.getInstance().getEndDeviceToDelayMap().get(name);
			int currentCount = TimeKeeper.getInstance().getEndDeviceNumRetiredTuples().get(name);
			TimeKeeper.getInstance().getEndDeviceToDelayMap().put(name, (currentAverage*currentCount+delay)/(currentCount+1));
		    TimeKeeper.getInstance().getEndDeviceNumRetiredTuples().put(name,currentCount+1);
		}
		}
	
	
	public void updateCriticalityTypleDelay(Tuple tuple, double delay)
	{
		
		String name = TimeKeeper.getInstance().getTupleIdToDeviceMap().get(tuple.getActualTupleId());
		String tmp;
		
		MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(name);
		
		//int crit = dv.getCriticalityLevel();
		
		//int crit = TimeKeeper.getInstance().getTupleIdToCriticalityMap().get(tuple.getActualTupleId());
		
		int crit = TimeKeeper.getInstance().getTupleIdToCriticalityMap().get(tuple.getActualTupleId());
		
		//System.out.println("  Crit = " + crit + "  Delay:   " + delay);
		
		
		if (crit == 0)
			tmp = "Low";
		//else if (crit==1)
		//	tmp = "Medium";
		else
			tmp = "High";
		
		if(!TimeKeeper.getInstance().getCritTypleToAverageResponseTime().containsKey(tmp)){
			TimeKeeper.getInstance().getCritTypleToAverageResponseTime().put(tmp, delay);
			TimeKeeper.getInstance().getCritTypleToTupleCount().put(tmp, 1);
			
		} else{
			double currentAverage = TimeKeeper.getInstance().getCritTypleToAverageResponseTime().get(tmp);
			int currentCount = TimeKeeper.getInstance().getCritTypleToTupleCount().get(tmp);
			TimeKeeper.getInstance().getCritTypleToAverageResponseTime().put(tmp, (currentAverage*currentCount+delay)/(currentCount+1));
		    TimeKeeper.getInstance().getCritTypleToTupleCount().put(tmp,currentCount+1);
		}
		}
	
	public void updateCoreDeviceDelay(Tuple tuple, double delay)
	{
		
		String name = TimeKeeper.getInstance().getTupleIdToDeviceMap().get(tuple.getActualTupleId());
		String name_1 = TimeKeeper.getInstance().getEndDeviceToDeviceMap().get(name);
		
		if(!TimeKeeper.getInstance().getCoreDeviceToDelayMap().containsKey(name_1)){
			TimeKeeper.getInstance().getCoreDeviceToDelayMap().put(name_1, delay);
			TimeKeeper.getInstance().getCoreDeviceNumRetiredTuples().put(name_1, 1);
			
		} else{
			double currentAverage = TimeKeeper.getInstance().getCoreDeviceToDelayMap().get(name_1);
			int currentCount = TimeKeeper.getInstance().getCoreDeviceNumRetiredTuples().get(name_1);
			TimeKeeper.getInstance().getCoreDeviceToDelayMap().put(name_1, (currentAverage*currentCount+delay)/(currentCount+1));
		    TimeKeeper.getInstance().getCoreDeviceNumRetiredTuples().put(name_1,currentCount+1);
		}
		}
	
}
