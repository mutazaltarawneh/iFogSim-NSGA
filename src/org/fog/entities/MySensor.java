package org.fog.entities;

import java.util.ArrayList;

import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.MyApplication;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.GeoLocation;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.Distribution;

public class MySensor extends SimEntity{
	
	private int gatewayDeviceId;
	private GeoLocation geoLocation;
	private long outputSize;
	private String appId;
	private int userId;
	private String tupleType;
	private String sensorName;
	private String destModuleName;
	private Distribution transmitDistribution;
	private int controllerId;
	private MyApplication app;
	private double latency;
	
	private double posCons;
	private double negCons;
	
	protected int numEmittedTuples;
	
	public MySensor(String name, int userId, String appId, int gatewayDeviceId, double latency, GeoLocation geoLocation, 
			Distribution transmitDistribution, int cpuLength, int nwLength, String tupleType, String destModuleName) {
		super(name);
		this.setAppId(appId);
		this.gatewayDeviceId = gatewayDeviceId;
		this.geoLocation = geoLocation;
		this.outputSize = 3;
		this.setTransmitDistribution(transmitDistribution);
		setUserId(userId);
		setDestModuleName(destModuleName);
		setTupleType(tupleType);
		setSensorName(sensorName);
		setLatency(latency);
		this.setNumEmittedTuples(0);
	}
	
	public MySensor(String name, int userId, String appId, int gatewayDeviceId, double latency, GeoLocation geoLocation, 
			Distribution transmitDistribution, String tupleType) {
		super(name);
		this.setAppId(appId);
		this.gatewayDeviceId = gatewayDeviceId;
		this.geoLocation = geoLocation;
		this.outputSize = 3;
		this.setTransmitDistribution(transmitDistribution);
		setUserId(userId);
		setTupleType(tupleType);
		setSensorName(sensorName);
		setLatency(latency);
		setNumEmittedTuples(0);
	}
	
	/**
	 * This constructor is called from the code that generates PhysicalTopology from JSON
	 * @param name
	 * @param tupleType
	 * @param string 
	 * @param userId
	 * @param appId
	 * @param transmitDistribution
	 */
	public MySensor(String name, String tupleType, int userId, String appId, Distribution transmitDistribution) {
		super(name);
		this.setAppId(appId);
		this.setTransmitDistribution(transmitDistribution);
		setTupleType(tupleType);
		setSensorName(tupleType);
		setUserId(userId);
		this.setNumEmittedTuples(0);
	}
	
	public void transmit(){
		AppEdge _edge = null;
		long cpuLength;
		long nwLength;
		
		double cons;
		
		
		for(AppEdge edge : getApp().getEdges()){
			if(edge.getSource().equals(getTupleType()))
				_edge = edge;
		}
		MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(this.gatewayDeviceId);
		
		int crit = dv.getCurrentCriticalityLevel();
		
		if (crit ==0)
			cons =1;
		else cons =1.2;
		
		
			 cpuLength = (long)(_edge.getTupleCpuLength()*this.posCons);
			 nwLength = (long) (_edge.getTupleNwLength()* this.posCons);
	
		
		Tuple tuple = new Tuple(getAppId(), FogUtils.generateTupleId(), Tuple.UP, cpuLength, 1, nwLength, outputSize, 
				new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
		tuple.setUserId(getUserId());
		tuple.setTupleType(getTupleType());
		
		tuple.setDestModuleName(_edge.getDestination());
		tuple.setSrcModuleName(getSensorName());
		Logger.debug(getName(), "Sending tuple with tupleId = "+tuple.getCloudletId());

		//System.out.println(getName() +  "   Sending tuple with tupleId =   "+tuple.getCloudletId()+ "  Crit   " + dv.getCurrentCriticalityLevel() + "  @  " + CloudSim.clock());
		
		setNumEmittedTuples(getNumEmittedTuples()+1);
		
		int actualTupleId = updateTimings(getSensorName(), tuple.getDestModuleName());
		tuple.setActualTupleId(actualTupleId);
		tuple.setActualSrcId(CloudSim.getEntityName(this.gatewayDeviceId));
		tuple.setOriginalSrcDeviceId(this.gatewayDeviceId);
		
		
		
		
		dv.getEmittedTuplesIdList().add(tuple.getActualTupleId());
	//	System.out.println("My Sensor " + "Typle :   " + tuple.getActualTupleId() + "     Srd Device Id:   " + CloudSim.getEntityName(this.gatewayDeviceId));
		if (!TimeKeeper.getInstance().getTupleIdToDeviceMap().containsKey(tuple.getActualTupleId())) {
		TimeKeeper.getInstance().getTupleIdToDeviceMap().put(tuple.getActualTupleId(), dv.getName());
		}
		
		if (!TimeKeeper.getInstance().getTupleIdToCriticalityMap().containsKey(tuple.getActualTupleId()))
		{
			TimeKeeper.getInstance().getTupleIdToCriticalityMap().put(tuple.getActualTupleId(), dv.getCurrentCriticalityLevel());
		}
		
		/*
		 * System.out.println("My Sensor " + "Typle :   " + tuple.getActualTupleId() +
		 * "     Srd Device Id:   " + CloudSim.getEntityName(this.gatewayDeviceId) +
		 * "   Criticality:  " + dv.getCriticalityLevel() + " Current criticality:  " +
		 * TimeKeeper.getInstance().getCurrentDeviceCriticality().get(dv.getName()));
		 */
		  
		  
		/*
		 * System.out.println("My Sensor " + "Typle :   " + tuple.getActualTupleId() +
		 * "     Srd Device Id:   " + CloudSim.getEntityName(this.gatewayDeviceId) +
		 * "   Criticality:  " + dv.getCriticalityLevel() + " Current criticality:  " +
		 * dv.getCurrentCriticalityLevel());
		 */
		 
		 
		send(this.gatewayDeviceId, getLatency(), FogEvents.TUPLE_ARRIVAL,tuple);
	}
	
	private int updateTimings(String src, String dest){
		MyApplication application = getApp();
		for(AppLoop loop : application.getLoops()){
			if(loop.hasEdge(src, dest)){
				
				int tupleId = TimeKeeper.getInstance().getUniqueId();
				if(!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
					TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
				TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
				TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());
				return tupleId;
			}
		}
		return -1;
	}
	
	@Override
	public void startEntity() {
		send(gatewayDeviceId, CloudSim.getMinTimeBetweenEvents(), FogEvents.SENSOR_JOINED, geoLocation);
		send(getId(), getTransmitDistribution().getNextValue(), FogEvents.EMIT_TUPLE);
	}

	@Override
	public void processEvent(SimEvent ev) {
		
		MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(this.gatewayDeviceId);
		
		int crit = dv.getCurrentCriticalityLevel();
		double cons;
		if (crit ==0)
			cons = 1;
		else
			cons =0.8;
		
		//System.out.println(this.posCons);
		
		switch(ev.getTag()){
		case FogEvents.TUPLE_ACK:
			//transmit(transmitDistribution.getNextValue());
			break;
		case FogEvents.EMIT_TUPLE:
			transmit();
			
			//double tmp = getTransmitDistribution().getNextValue();
			
			//System.out.println(tmp);
			
			send(getId(), getTransmitDistribution().getNextValue() * this.negCons, FogEvents.EMIT_TUPLE);
			
			//send(getId(), 10.0, FogEvents.EMIT_TUPLE);
			break;
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

	public String getTupleType() {
		return tupleType;
	}

	public void setTupleType(String tupleType) {
		this.tupleType = tupleType;
	}

	public String getSensorName() {
		return sensorName;
	}

	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getDestModuleName() {
		return destModuleName;
	}

	public void setDestModuleName(String destModuleName) {
		this.destModuleName = destModuleName;
	}

	public Distribution getTransmitDistribution() {
		return transmitDistribution;
	}

	public void setTransmitDistribution(Distribution transmitDistribution) {
		this.transmitDistribution = transmitDistribution;
	}

	public int getControllerId() {
		return controllerId;
	}

	public void setControllerId(int controllerId) {
		this.controllerId = controllerId;
	}

	public MyApplication getApp() {
		return app;
	}

	public void setApp(MyApplication app) {
		this.app = app;
	}

	public Double getLatency() {
		return latency;
	}

	public void setLatency(Double latency) {
		this.latency = latency;
	}

	public void setNumEmittedTuples(int num)
	{
		this.numEmittedTuples = num;
	}
	
	public int getNumEmittedTuples()
	{
		return this.numEmittedTuples;
	}
	
	public void setPosConstant(double cons)
	{
		this.posCons = cons;
	}
	
	public void setNegConstant(double cons)
	{
		this.negCons = cons;
	}
	
	public double getPosConstant()
	{
		return this.posCons;
	}
	
	public double getNegConstant()
	{
		return this.negCons;
	}
}
