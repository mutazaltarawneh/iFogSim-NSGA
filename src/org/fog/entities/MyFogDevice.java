package org.fog.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Queue;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.MyApplication;

import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.security.SecurityConstants;
import org.fog.security.SecurityCounterMeasure;
import org.fog.security.SecurityTaxonomy;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.ModuleLaunchConfig;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;

import org.fog.test.perfeval.simConstants;

import org.knapsack.Knapsack;
import org.knapsack.Item;
import org.knapsack.Solution;

public class MyFogDevice extends PowerDatacenter {
	protected Queue<Tuple> northTupleQueue;
	protected Queue<Pair<Tuple, Integer>> southTupleQueue;
	
	protected List<String> activeMyApplications;
	
	protected Map<String, MyApplication> applicationMap;
	public Map<String, List<String>> appToModulesMap;
	protected Map<Integer, Double> childToLatencyMap;
 
	
	protected Map<Integer, Integer> cloudTrafficMap;
	
	List<String> associatedEndDevices;
	
	protected double lockTime;
	
	/**	
	 * ID of the parent Fog Device
	 */
	protected int parentId;
	
	protected int numUpTuples;
	
	protected int numDownTuples;
	
	List<Integer> emittedTuplesId;
	
	/**
	 * ID of the Controller
	 */
	protected int controllerId;
	/**
	 * IDs of the children Fog devices
	 */
	protected List<Integer> childrenIds;

	protected Map<Integer, List<String>> childToOperatorsMap;
	
	/**
	 * Flag denoting whether the link southwards from this FogDevice is busy
	 */
	protected boolean isSouthLinkBusy;
	
	/**
	 * Flag denoting whether the link northwards from this FogDevice is busy
	 */
	protected boolean isNorthLinkBusy;
	
	protected double uplinkBandwidth;
	protected double downlinkBandwidth;
	protected double uplinkLatency;
	protected List<Pair<Integer, Double>> associatedMyActuatorIds;
	
	protected double energyConsumption;
	protected double lastUtilizationUpdateTime;
	protected double lastUtilization;
	private int level;
	
	protected double ratePerMips;
	
	protected double totalCost;
	
	protected double totalPower;
	
	protected Map<String, Map<String, Integer>> moduleInstanceCount;
	
	
	 private List<SecurityCounterMeasure> securityMeasures;
	 
	 protected int criticalityCeling;
	 
	 protected int criticalityLevel;
	 protected int currentCriticalityLevel;
	 
	 protected Knapsack sack;
	 protected Solution solution;
	 protected List<Item> rollOverItems; 
	 
	 protected double maxSuitabilityFactor;
	 
	 List<Integer> childEndDeviceIds;
	 
	 Map<Integer,Double> childCritMap;
		Map<Integer,Double> childSecMap;
	    Map<Integer,Double> childMipsMap ;
	    
	 protected double aggProfitValue;
	 protected String prefFogDeviceName;
	 
	 protected List<String> essSecReq;
	 protected List<String> nonEssSecReq;
	 
	 protected double minCritVal;
	 protected double maxCritVal;
	 
	 protected double minSecVal;
	 protected double maxSecVal;
	 
	 
	
	public MyFogDevice(
			String name, 
			FogDeviceCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList,
			double schedulingInterval,
			double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		setCharacteristics(characteristics);
		setVmAllocationPolicy(vmAllocationPolicy);
		setLastProcessTime(0.0);
		setStorageList(storageList);
		setVmList(new ArrayList<Vm>());
		setSchedulingInterval(schedulingInterval);
		setUplinkBandwidth(uplinkBandwidth);
		setDownlinkBandwidth(downlinkBandwidth);
		setUplinkLatency(uplinkLatency);
		setRatePerMips(ratePerMips);
		setAssociatedMyActuatorIds(new ArrayList<Pair<Integer, Double>>());
		for (Host host : getCharacteristics().getHostList()) {
			host.setDatacenter(this);
		}
		setActiveMyApplications(new ArrayList<String>());
		// If this resource doesn't have any PEs then no useful at all
		if (getCharacteristics().getNumberOfPes() == 0) {
			throw new Exception(super.getName()
					+ " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
		}
		// stores id of this class
		getCharacteristics().setId(super.getId());
		
		applicationMap = new HashMap<String, MyApplication>();
		appToModulesMap = new HashMap<String, List<String>>();
		northTupleQueue = new LinkedList<Tuple>();
		southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
		setNorthLinkBusy(false);
		setSouthLinkBusy(false);
		
		
		setChildrenIds(new ArrayList<Integer>());
		setChildToOperatorsMap(new HashMap<Integer, List<String>>());
		
		this.cloudTrafficMap = new HashMap<Integer, Integer>();
		
		this.lockTime = 0;
		
		this.energyConsumption = 0;
		this.lastUtilization = 0;
		setTotalCost(0);
		setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
		setChildToLatencyMap(new HashMap<Integer, Double>());
		
		setNumUpTuples(0);
		setNumDownTuples(0);
		
		setEmittedTuplesIdList(new ArrayList<Integer>());
		setAssociatedEndDevices(new ArrayList<String>());
		setCrticalityCeiling(-1);
		setCriticalityLevel(-1);
		setCurrentCriticalityLevel(-1);
		setRollOverItems(new ArrayList<Item>());
		setMaxSuitabilityFactor(2.0);
		setChildEndDeviceIds(new ArrayList<Integer>());
		setAggProfitValue(Double.MIN_VALUE);
		setEssSecReq(new ArrayList<String>());
		setNonEssSecReq(new ArrayList<String>());
		setTotalPower(this.getPower());
		
		setMinCritVal(Double.MAX_VALUE);
		setMinSecVal(Double.MAX_VALUE);
		
		setMaxCritVal(Double.MIN_VALUE);
		setMaxSecVal(Double.MIN_VALUE);
	}

	public MyFogDevice(
			String name, long mips, int ram, 
			double uplinkBandwidth, double downlinkBandwidth, double ratePerMips, PowerModel powerModel) throws Exception {
		super(name, null, null, new LinkedList<Storage>(), 0);
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				powerModel
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		setVmAllocationPolicy(new AppModuleAllocationPolicy(hostList));
		
		String arch = Config.FOG_DEVICE_ARCH; 
		String os = Config.FOG_DEVICE_OS; 
		String vmm = Config.FOG_DEVICE_VMM;
		double time_zone = Config.FOG_DEVICE_TIMEZONE;
		double cost = Config.FOG_DEVICE_COST; 
		double costPerMem = Config.FOG_DEVICE_COST_PER_MEMORY;
		double costPerStorage = Config.FOG_DEVICE_COST_PER_STORAGE;
		double costPerBw = Config.FOG_DEVICE_COST_PER_BW;

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		setCharacteristics(characteristics);
		
		setLastProcessTime(0.0);
		setVmList(new ArrayList<Vm>());
		setUplinkBandwidth(uplinkBandwidth);
		setDownlinkBandwidth(downlinkBandwidth);
		setUplinkLatency(uplinkLatency);
		setAssociatedMyActuatorIds(new ArrayList<Pair<Integer, Double>>());
		for (Host host1 : getCharacteristics().getHostList()) {
			host1.setDatacenter(this);
		}
		setActiveMyApplications(new ArrayList<String>());
		if (getCharacteristics().getNumberOfPes() == 0) {
			throw new Exception(super.getName()
					+ " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
		}
		
		
		getCharacteristics().setId(super.getId());
		
		applicationMap = new HashMap<String, MyApplication>();
		appToModulesMap = new HashMap<String, List<String>>();
		northTupleQueue = new LinkedList<Tuple>();
		southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
		setNorthLinkBusy(false);
		setSouthLinkBusy(false);
		
		
		setChildrenIds(new ArrayList<Integer>());
		setChildToOperatorsMap(new HashMap<Integer, List<String>>());
		
		this.cloudTrafficMap = new HashMap<Integer, Integer>();
		
		this.lockTime = 0;
		
		this.energyConsumption = 0;
		this.lastUtilization = 0;
		setTotalCost(0);
		setChildToLatencyMap(new HashMap<Integer, Double>());
		setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
		setNumUpTuples(0);
		setNumDownTuples(0);
		this.setEmittedTuplesIdList(new ArrayList<Integer>());
		
		setAssociatedEndDevices(new ArrayList<String>());
		setCrticalityCeiling(-1);
		
		setCriticalityLevel(-1);
		setCurrentCriticalityLevel(-1);
		setRollOverItems(new ArrayList<Item>());
		setMaxSuitabilityFactor(2.0);
		setChildEndDeviceIds(new ArrayList<Integer>());
		setAggProfitValue(Double.MIN_VALUE);
		setEssSecReq(new ArrayList<String>());
		setNonEssSecReq(new ArrayList<String>());
		setTotalPower(this.getPower());
		
		setMinCritVal(Double.MAX_VALUE);
		setMinSecVal(Double.MAX_VALUE);
		
		setMaxCritVal(Double.MIN_VALUE);
		setMaxSecVal(Double.MIN_VALUE);
	}
	
	/**
	 * Overrides this method when making a new and different type of resource. <br>
	 * <b>NOTE:</b> You do not need to override {@link #body()} method, if you use this method.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void registerOtherEntity() {
		
	}
	
	@Override
	protected void processOtherEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.TUPLE_ARRIVAL:
			processTupleArrival(ev);
			break;
		case FogEvents.LAUNCH_MODULE:
			processModuleArrival(ev);
			break;
		case FogEvents.RELEASE_OPERATOR:
			processOperatorRelease(ev);
			break;
		case FogEvents.SENSOR_JOINED:
			processMySensorJoining(ev);
			break;
		case FogEvents.SEND_PERIODIC_TUPLE:
			sendPeriodicTuple(ev);
			break;
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.UPDATE_NORTH_TUPLE_QUEUE:
			updateNorthTupleQueue();
			break;
		case FogEvents.UPDATE_SOUTH_TUPLE_QUEUE:
			updateSouthTupleQueue();
			break;
		case FogEvents.ACTIVE_APP_UPDATE:
			updateActiveMyApplications(ev);
			break;
		case FogEvents.ACTUATOR_JOINED:
			processMyActuatorJoined(ev);
			break;
		case FogEvents.LAUNCH_MODULE_INSTANCE:
			updateModuleInstanceCount(ev);
			break;
		case FogEvents.RESOURCE_MGMT:
			manageResources(ev);
		case FogEvents.FLIP_CRIT:
			flipCriticality(ev);
			break;
		default:
			break;
		}
	}
	
	/**
	 * Perform miscellaneous resource management tasks
	 * @param ev
	 */
	private void manageResources(SimEvent ev) {
		updateEnergyConsumption();
		send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
	}
	
	
	private void flipCriticality(SimEvent ev)
	{
		
	/*if (getLevel()==3)
		{
		//	System.out.println(getName() + "  Criticality Flip @ :  " + CloudSim.clock());
			
			if (getCurrentCriticalityLevel()==0)
			{
				int tmp = getCurrentCriticalityLevel();
				this.setCurrentCriticalityLevel(2);
				TimeKeeper.getInstance().getCurrentDeviceCriticality().put(this.getName(), 2);
			//	System.out.println(this.getName() + "  Current criticality level:   " + tmp + "   Flipped to:   " + this.getCurrentCriticalityLevel());
			}
		//	if (getCurrentCriticalityLevel()==2)
			else
			{
			//	System.out.println(getName() + "  Criticality Flip @ :  " + CloudSim.clock());
				int tmp = getCurrentCriticalityLevel();
				this.setCurrentCriticalityLevel(0);
				TimeKeeper.getInstance().getCurrentDeviceCriticality().put(this.getName(), 0);
			//	System.out.println(this.getName() + "  Current criticality level:   " + tmp + "   Flipped to:   " + this.getCurrentCriticalityLevel());
				
			}
		}*/
		
		//System.out.println("Criticality Flip Request By:  " +  getName());
		//send(getId(), Config.CRITICALITY_FLIP_INTERVAL + this.getId() , FogEvents.FLIP_CRIT );
	}

	/**
	 * Updating the number of modules of an application module on this device
	 * @param ev instance of SimEvent containing the module and no of instances 
	 */
	private void updateModuleInstanceCount(SimEvent ev) {
		ModuleLaunchConfig config = (ModuleLaunchConfig)ev.getData();
		String appId = config.getModule().getAppId();
		if(!moduleInstanceCount.containsKey(appId))
			moduleInstanceCount.put(appId, new HashMap<String, Integer>());
		moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
		System.out.println(getName()+ " Creating "+config.getInstanceCount()+" instances of module "+config.getModule().getName());
	}

	private AppModule getModuleByName(String moduleName){
		AppModule module = null;
		for(Vm vm : getHost().getVmList()){
			if(((AppModule)vm).getName().equals(moduleName)){
				module=(AppModule)vm;
				break;
			}
		}
		return module;
	}
	
	/**
	 * Sending periodic tuple for an application edge. Note that for multiple instances of a single source module, only one tuple is sent DOWN while instanceCount number of tuples are sent UP.
	 * @param ev SimEvent instance containing the edge to send tuple on
	 */
	private void sendPeriodicTuple(SimEvent ev) {
		AppEdge edge = (AppEdge)ev.getData();
		String srcModule = edge.getSource();
		AppModule module = getModuleByName(srcModule);
		
		if(module == null)
			return;
		
		int instanceCount = module.getNumInstances();
		/*
		 * Since tuples sent through a DOWN application edge are anyways broadcasted, only UP tuples are replicated
		 */
		for(int i = 0;i<((edge.getDirection()==Tuple.UP)?instanceCount:1);i++){
			//System.out.println(CloudSim.clock()+" : Sending periodic tuple "+edge.getTupleType());
			Tuple tuple = applicationMap.get(module.getAppId()).createTuple(edge, getId(), module.getId());
			updateTimingsOnSending(tuple);
			sendToSelf(tuple);			
		}
		send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
	}

	protected void processMyActuatorJoined(SimEvent ev) {
		int actuatorId = ev.getSource();
		double delay = (double)ev.getData();
		
		//System.out.println(actuatorId + "  Joined " + this.getName());
		
		getAssociatedMyActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
	}

	
	protected void updateActiveMyApplications(SimEvent ev) {
		MyApplication app = (MyApplication)ev.getData();
		getActiveMyApplications().add(app.getAppId());
	}

	
	public String getOperatorName(int vmId){
		for(Vm vm : this.getHost().getVmList()){
			if(vm.getId() == vmId)
				return ((AppModule)vm).getName();
		}
		return null;
	}
	
	/**
	 * Update cloudet processing without scheduling future events.
	 * 
	 * @return the double
	 */
	protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
		double currentTime = CloudSim.clock();
		double minTime = Double.MAX_VALUE;
		double timeDiff = currentTime - getLastProcessTime();
		double timeFrameDatacenterEnergy = 0.0;

		for (PowerHost host : this.<PowerHost> getHostList()) {
			Log.printLine();

			double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
			if (time < minTime) {
				minTime = time;
			}

			Log.formatLine(
					"%.2f: [Host #%d] utilization is %.2f%%",
					currentTime,
					host.getId(),
					host.getUtilizationOfCpu() * 100);
		}

		if (timeDiff > 0) {
			Log.formatLine(
					"\nEnergy consumption for the last time frame from %.2f to %.2f:",
					getLastProcessTime(),
					currentTime);

			for (PowerHost host : this.<PowerHost> getHostList()) {
				double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
				double utilizationOfCpu = host.getUtilizationOfCpu();
				double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
						previousUtilizationOfCpu,
						utilizationOfCpu,
						timeDiff);
				timeFrameDatacenterEnergy += timeFrameHostEnergy;

				Log.printLine();
				Log.formatLine(
						"%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
						currentTime,
						host.getId(),
						getLastProcessTime(),
						previousUtilizationOfCpu * 100,
						utilizationOfCpu * 100);
				Log.formatLine(
						"%.2f: [Host #%d] energy is %.2f W*sec",
						currentTime,
						host.getId(),
						timeFrameHostEnergy);
			}

			Log.formatLine(
					"\n%.2f: Data center's energy is %.2f W*sec\n",
					currentTime,
					timeFrameDatacenterEnergy);
		}

		setPower(getPower() + timeFrameDatacenterEnergy);

		checkCloudletCompletion();

		/** Remove completed VMs **/
		/**
		 * Change made by HARSHIT GUPTA
		 */
		/*for (PowerHost host : this.<PowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}*/
		
		Log.printLine();

		setLastProcessTime(currentTime);
		return minTime;
	}


	protected void checkCloudletCompletion() {
		boolean cloudletCompleted = false;
		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			Host host = list.get(i);
			for (Vm vm : host.getVmList()) {
				while (vm.getCloudletScheduler().isFinishedCloudlets()) {
					Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
					if (cl != null) {
						
						cloudletCompleted = true;
						Tuple tuple = (Tuple)cl;
						TimeKeeper.getInstance().tupleEndedExecution(tuple);
						MyApplication application = getMyApplicationMap().get(tuple.getAppId());
						Logger.debug(getName(), "Completed execution of tuple "+tuple.getCloudletId()+"on "+tuple.getDestModuleName());
						List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId(), vm.getId());
						for(Tuple resTuple : resultantTuples){
							resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
							resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
							updateTimingsOnSending(resTuple);
							sendToSelf(resTuple);
						}
						sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
					}
				}
			}
		}
		if(cloudletCompleted)
			updateAllocatedMips(null);
	}
	
	protected void updateTimingsOnSending(Tuple resTuple) {
		// TODO ADD CODE FOR UPDATING TIMINGS WHEN A TUPLE IS GENERATED FROM A PREVIOUSLY RECIEVED TUPLE. 
		// WILL NEED TO CHECK IF A NEW LOOP STARTS AND INSERT A UNIQUE TUPLE ID TO IT.
		String srcModule = resTuple.getSrcModuleName();
		String destModule = resTuple.getDestModuleName();
		for(AppLoop loop : getMyApplicationMap().get(resTuple.getAppId()).getLoops()){
			if(loop.hasEdge(srcModule, destModule) && loop.isStartModule(srcModule)){
				int tupleId = TimeKeeper.getInstance().getUniqueId();
				resTuple.setActualTupleId(tupleId);
				if(!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
					TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
				TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
				TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());
				
				//Logger.debug(getName(), "\tSENDING\t"+tuple.getActualTupleId()+"\tSrc:"+srcModule+"\tDest:"+destModule);
				
			}
		}
	}

	protected int getChildIdWithRouteTo(int targetDeviceId){
		for(Integer childId : getChildrenIds()){
			if(targetDeviceId == childId)
				return childId;
			if(((FogDevice)CloudSim.getEntity(childId)).getChildIdWithRouteTo(targetDeviceId) != -1)
				return childId;
		}
		return -1;
	}
	
	protected int getChildIdForTuple(Tuple tuple){
		if(tuple.getDirection() == Tuple.ACTUATOR){
			int gatewayId = ((MyActuator)CloudSim.getEntity(tuple.getActuatorId())).getGatewayDeviceId();
			return getChildIdWithRouteTo(gatewayId);
		}
		return -1;
	}
	
	protected void updateAllocatedMips(String incomingOperator){
		getHost().getVmScheduler().deallocatePesForAllVms();
		for(final Vm vm : getHost().getVmList()){
			if(vm.getCloudletScheduler().runningCloudlets() > 0 || ((AppModule)vm).getName().equals(incomingOperator)){
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add((double) getHost().getTotalMips());}});
			}else{
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add(0.0);}});
			}
		}
		
		updateEnergyConsumption();
		
	}
	
	protected void updateTotalCost(Tuple tuple)
	{
		
		double currentCost = getTotalCost();
		double newcost = currentCost + getRatePerMips()*tuple.getCloudletLength();
		setTotalCost(newcost);
		
	}
	
	private void updateEnergyConsumption() {
		double totalMipsAllocated = 0;
		for(final Vm vm : getHost().getVmList()){
			AppModule operator = (AppModule)vm;
			operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler()
					.getAllocatedMipsForVm(operator));
			totalMipsAllocated += getHost().getTotalAllocatedMipsForVm(vm);
		}
		
		double timeNow = CloudSim.clock();
		double currentEnergyConsumption = getEnergyConsumption();
		double newEnergyConsumption = currentEnergyConsumption + (timeNow-lastUtilizationUpdateTime)*getHost().getPowerModel().getPower(lastUtilization);
		setEnergyConsumption(newEnergyConsumption);
	
		/*if(getName().equals("d-0")){
			System.out.println("------------------------");
			System.out.println("Utilization = "+lastUtilization);
			System.out.println("Power = "+getHost().getPowerModel().getPower(lastUtilization));
			System.out.println(timeNow-lastUtilizationUpdateTime);
		}*/
		
		//double currentCost = getTotalCost();
		//double newcost = currentCost + (timeNow-lastUtilizationUpdateTime)*getRatePerMips()*lastUtilization*getHost().getTotalMips();
		//setTotalCost(newcost);
		
		lastUtilization = Math.min(1, totalMipsAllocated/getHost().getTotalMips());
		lastUtilizationUpdateTime = timeNow;
	}

	protected void processAppSubmit(SimEvent ev) {
		MyApplication app = (MyApplication)ev.getData();
		applicationMap.put(app.getAppId(), app);
	}

	protected void addChild(int childId){
		if(CloudSim.getEntityName(childId).toLowerCase().contains("sensor"))
			return;
		if(!getChildrenIds().contains(childId) && childId != getId())
			getChildrenIds().add(childId);
		if(!getChildToOperatorsMap().containsKey(childId))
			getChildToOperatorsMap().put(childId, new ArrayList<String>());
	}
	
	protected void updateCloudTraffic(){
		int time = (int)CloudSim.clock()/1000;
		if(!cloudTrafficMap.containsKey(time))
			cloudTrafficMap.put(time, 0);
		cloudTrafficMap.put(time, cloudTrafficMap.get(time)+1);
	}
	
	protected void sendTupleToMyActuator(Tuple tuple){
		/*for(Pair<Integer, Double> actuatorAssociation : getAssociatedMyActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			if(actuatorId == tuple.getMyActuatorId()){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		int childId = getChildIdForTuple(tuple);
		if(childId != -1)
			sendDown(tuple, childId);*/
		for(Pair<Integer, Double> actuatorAssociation : getAssociatedMyActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			String actuatorType = ((MyActuator)CloudSim.getEntity(actuatorId)).getMyActuatorType();
			if(tuple.getDestModuleName().equals(actuatorType)){
			//	System.out.println("  SendTupleToMyActuator   " + this.getName() + "  is sending tuple "  + tuple.getActualTupleId() +  "   " + tuple.getTupleType() +"  to   " + CloudSim.getEntityName(actuatorId));

				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		for(int childId : getChildrenIds()){
			
			sendDown(tuple, childId);
		}
	}
	int numClients=0;
	protected void processTupleArrival(SimEvent ev){
		Tuple tuple = (Tuple)ev.getData();
		MyApplication app = (MyApplication) getMyApplicationMap().get(tuple.getAppId());
		
		//setTotalPower(this.getTotalPower() + getHost().getEstimatedPower(tuple.getCloudletLength()));
	//	System.out.println(this.getName() +  "    "+getHost().getTotalMips()  + "    " + tuple.getTupleType() + "   " + tuple.getCloudletLength() + " Power:  " + getHost().getEstimatedPower(tuple.getCloudletLength()));
		
		//System.out.println(tuple.getAppId());
		
		//double critWeight = app.getCritWeight();
		
	//	System.out.println(app.getCritWeight());
		
		if (getLevel() ==3)
		{
			//System.out.println("I am here !");
			
			if (tuple.getDirection() == Tuple.UP )
			{
				//System.out.println("I am here !");
				//System.out.println(this.getName() + "   " + this.getNumUpTuples());
				
				//getEmittedTuplesIdList().add(tuple.getActualTupleId());
				
				tuple.setOriginalSrcDeviceId(getId());
				setNumUpTuples(getNumUpTuples()+1);
			}
			
			if (tuple.getDirection() == Tuple.DOWN && ev.getSource() !=this.getId());
			{
				//System.out.println("I am here !");
				//System.out.println(this.getName() + "   " + tuple.getTupleType());
				setNumDownTuples(getNumDownTuples()+1);
			}
			
		}
		
		if (getLevel() ==2 || getLevel()==1)
		{
			//System.out.println("I am here !");
			
			if (tuple.getDirection() == Tuple.UP )
		//	if (tuple.getDirection() == Tuple.UP && tuple.getTupleType().equals("RawData") )
			{
				//System.out.println("I am here !");
				//System.out.println(this.getName() + "   " + tuple.getTupleType());
				setNumUpTuples(getNumUpTuples()+1);
			}
		}
		
		//System.out.println("Current device:  " + this.getName());
		
		
		/*
		 * if (this.getLevel() ==1) {
		 * 
		 * System.out.println("Current device:  " + this.getName() + "    " +
		 * tuple.getTupleType());
		 * 
		 * 
		 * //System.out.println(getName() + "    Recieved   " + tuple.getTupleType() +
		 * "   " + tuple.getDirection()+ " From  " + ev.getSource()); }
		 */
		
		if(getName().equals("cloud")){
			updateCloudTraffic();
		}
		
		/*if(getName().equals("d-0") && tuple.getTupleType().equals("_SENSOR")){
			System.out.println(++numClients);
		}*/
		Logger.debug(getName(), "Received tuple "+tuple.getCloudletId()+"with tupleType = "+tuple.getTupleType()+"\t| Source : "+
		CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.getDestination()));
		
		/*
		 * if (getName()=="cloud" && tuple.getTupleType().equals("RawData")) {
		 * System.out.println(getName()+
		 * "  Received tuple "+tuple.getActualTupleId()+"  with tupleType = "+tuple.
		 * getTupleType()+"\t| Source : "+
		 * CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.
		 * getDestination()) + "  " +
		 * TimeKeeper.getInstance().getTupleIdToDeviceMap().get(tuple.getActualTupleId()
		 * )); }
		 */
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
		
		if(FogUtils.appIdToGeoCoverageMap.containsKey(tuple.getAppId())){
		}
		
		if(tuple.getDirection() == Tuple.ACTUATOR ){
			
			//sendTupleToMyActuator(tuple);
			
			//  if (getEmittedTuplesIdList().contains(tuple.getActualTupleId())) {
			 // System.out.println("My FogDevice:   tuple " + tuple.getActualTupleId() +
			  //"  is in  " + this.getName());
			  
			  sendTupleToMyActuator(tuple);
			  return;
			  //}	
		}
		
		if(getHost().getVmList().size() > 0){
			final AppModule operator = (AppModule)getHost().getVmList().get(0);
			if(CloudSim.clock() > 0){
				getHost().getVmScheduler().deallocatePesForVm(operator);
				getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add((double) getHost().getTotalMips());}});
			}
		}
		
		if(getName().equals("cloud") && tuple.getDestModuleName()==null){
			sendNow(getControllerId(), FogEvents.TUPLE_FINISHED, null);
		}
		
		if(appToModulesMap.containsKey(tuple.getAppId())){
			if(appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())){
				
				
				if (getLevel() == 2 || getLevel()==1 || getLevel()==0)
				//if (getLevel() == 2 || getLevel()==1 || getLevel()==0 )
				{
					if (tuple.getDirection() == Tuple.UP)
					{
						
						//System.out.println("I am here !");
						if (tuple.getDestModuleName().equals("mainModule"))
						{
							
						//System.out.println(getHost().getAvailableMips()/getHost().getTotalMips());
							
							String name = TimeKeeper.getInstance().getTupleIdToDeviceMap().get(tuple.getActualTupleId());
							int crit = TimeKeeper.getInstance().getTupleIdToCriticalityMap().get(tuple.getActualTupleId());
							
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(name);
							
							List<String> essSec = dv.getEssSecReq();
							List<String> nonEssSec = dv.getNonEssSecReq();
							
							double suit = this.computeSuitabilityToAppModule(essSec, nonEssSec);
							
							
							
				if ((getAssociatedEndDevices().contains(name))) {
			//	if ((getAssociatedEndDevices().contains(name) || ((crit == 2) && (suit >=1.0)))) {
				//	if ((getAssociatedEndDevices().contains(name) || ((crit == 2)))) {
						//	if ((getAssociatedEndDevices().contains(name)  && (suit >=1.0))) {
					//	System.out.println(this.getName() + "  Suit to   " + name + "   " + suit);	
	
								int vmId = -1;
								for(Vm vm : getHost().getVmList()){
									if(((AppModule)vm).getName().equals(tuple.getDestModuleName())){
										vmId = vm.getId();
									}
								}
								if(vmId < 0
										|| (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) && 
												tuple.getModuleCopyMap().get(tuple.getDestModuleName())!=vmId )){
									return;
								}
								tuple.setVmId(vmId);
								//Logger.error(getName(), "Executing tuple for operator " + moduleName);
								
								updateTimingsOnReceipt(tuple);
								
								/*MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(name);
								
								List<String> essSec = dv.getEssSecReq();
								List<String> nonEssSec = dv.getNonEssSecReq();
								
								double suit = this.computeSuitabilityToAppModule(essSec, nonEssSec);*/
								
								updateSecTypleCount(suit);
								
							//	System.out.println(this.getName() + "  Suitability to:  " + name + "  is:   " + suit);
								
								executeTuple(ev, tuple.getDestModuleName());
							}
							else
							{
								sendUp(tuple);
								//System.out.println("I am here ! ... Sending to:  " + CloudSim.getEntityName(getParentId()) );
							}
							
						}
						else
						{
							//System.out.println("I am here !");
							int vmId = -1;
							for(Vm vm : getHost().getVmList()){
								if(((AppModule)vm).getName().equals(tuple.getDestModuleName())){
									vmId = vm.getId();
								}
							}
							if(vmId < 0
									|| (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) && 
											tuple.getModuleCopyMap().get(tuple.getDestModuleName())!=vmId )){
								return;
							}
							tuple.setVmId(vmId);
							//Logger.error(getName(), "Executing tuple for operator " + moduleName);
							
							updateTimingsOnReceipt(tuple);
							
							executeTuple(ev, tuple.getDestModuleName());
						}
					}
				}
					
				else
				{
								
				int vmId = -1;
				for(Vm vm : getHost().getVmList()){
					if(((AppModule)vm).getName().equals(tuple.getDestModuleName())){
						vmId = vm.getId();
					}
				}
				if(vmId < 0
						|| (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) && 
								tuple.getModuleCopyMap().get(tuple.getDestModuleName())!=vmId )){
					return;
				}
				tuple.setVmId(vmId);
				//Logger.error(getName(), "Executing tuple for operator " + moduleName);
				
				updateTimingsOnReceipt(tuple);
				
				executeTuple(ev, tuple.getDestModuleName());
				}
			}else if(tuple.getDestModuleName()!=null){
				if(tuple.getDirection() == Tuple.UP)
				{
					if (this.getLevel() ==3)
					{
						if (this.getEmittedTuplesIdList().contains(tuple.getActualTupleId()))
							sendUp(tuple);	
					}
					else
					sendUp(tuple);
				}
				else if(tuple.getDirection() == Tuple.DOWN){
					for(int childId : getChildrenIds())
					{
						
						//System.out.println("I am here !");
						
						MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(childId);
						
						//System.out.println(" Sending Down Tuples  " + this.getName() + "  is sending tuple "  + tuple.getActualTupleId() +  "   " + tuple.getTupleType() +"  to   " + CloudSim.getEntityName(childId));
						sendDown(tuple, childId);
					/*	if (dv.getLevel() == 3)
						//	if (dv.getLevel() > 0)
						{
							if (dv.getEmittedTuplesIdList().contains(tuple.getActualTupleId()))
							{
							//	System.out.println(" Sending Down Tuples  " + this.getName() + "  is sending tuple "  + tuple.getActualTupleId() +  "   " + tuple.getTupleType() +"  to   " + dv.getName());
								sendDown(tuple, childId);
							}		
						}
						else
						{
							//System.out.println(" Sending Down Tuples  " + this.getName() + "  is sending tuple "  + tuple.getActualTupleId() +  "   " + tuple.getTupleType() +"  to   " + CloudSim.getEntityName(childId));

						sendDown(tuple, childId);
						}*/
						//sendDown(tuple, childId);
					}
				}
			}else{
				sendUp(tuple);
			}
		}else{
			if(tuple.getDirection() == Tuple.UP)
				sendUp(tuple);
			else if(tuple.getDirection() == Tuple.DOWN){
				for(int childId : getChildrenIds())
				{
				//	System.out.println("I am here !");
					sendDown(tuple, childId);
					MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(childId);
					//System.out.println(this.getName() + " Here 2 is sending "  + tuple.getCloudletId() +  "   " + tuple.getTupleType() +"  to   " + childId);

				}
			}
		}
	}

	protected void updateTimingsOnReceipt(Tuple tuple) {
		MyApplication app = getMyApplicationMap().get(tuple.getAppId());
		String srcModule = tuple.getSrcModuleName();
		String destModule = tuple.getDestModuleName();
		List<AppLoop> loops = app.getLoops();
		for(AppLoop loop : loops){
			if(loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)){				
				Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				if(startTime==null)
					break;
				if(!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())){
					TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
					TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
				}
				double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
				int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
				double delay = CloudSim.clock()- TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
				double newAverage = (currentAverage*currentCount + delay)/(currentCount+1);
				TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
				TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount+1);
				break;
			}
		}
	}

	protected void processMySensorJoining(SimEvent ev){
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
	}
	
	protected void executeTuple(SimEvent ev, String moduleName){
		Logger.debug(getName(), "Executing tuple on module "+moduleName);
		
		
		
		Tuple tuple = (Tuple)ev.getData();
		setTotalPower(this.getTotalPower() + getHost().getEstimatedPower(tuple.getCloudletLength()));
		//System.out.println(getName() + "  Executing tuple:  " + tuple.getActualTupleId() +  " on module  "+ moduleName + " Emitted From " + TimeKeeper.getInstance().getTupleIdToDeviceMap().get(tuple.getActualTupleId()));
		AppModule module = getModuleByName(moduleName);
		
		if(tuple.getDirection() == Tuple.UP){
			String srcModule = tuple.getSrcModuleName();
			if(!module.getDownInstanceIdsMaps().containsKey(srcModule))
				module.getDownInstanceIdsMaps().put(srcModule, new ArrayList<Integer>());
			if(!module.getDownInstanceIdsMaps().get(srcModule).contains(tuple.getSourceModuleId()))
				module.getDownInstanceIdsMaps().get(srcModule).add(tuple.getSourceModuleId());
			
			int instances = -1;
			for(String _moduleName : module.getDownInstanceIdsMaps().keySet()){
				instances = Math.max(module.getDownInstanceIdsMaps().get(_moduleName).size(), instances);
			}
			module.setNumInstances(instances);
		}
		
		TimeKeeper.getInstance().tupleStartedExecution(tuple);
		updateAllocatedMips(moduleName);
		processCloudletSubmit(ev, false);
		updateAllocatedMips(moduleName);
		this.updateTotalCost(tuple);
		/*for(Vm vm : getHost().getVmList()){
			Logger.error(getName(), "MIPS allocated to "+((AppModule)vm).getName()+" = "+getHost().getTotalAllocatedMipsForVm(vm));
		}*/
	}
	
	protected void processModuleArrival(SimEvent ev){
		AppModule module = (AppModule)ev.getData();
		String appId = module.getAppId();
		if(!appToModulesMap.containsKey(appId)){
			appToModulesMap.put(appId, new ArrayList<String>());
		}
		appToModulesMap.get(appId).add(module.getName());
		processVmCreate(ev, false);
		if (module.isBeingInstantiated()) {
			module.setBeingInstantiated(false);
		}
		
		initializePeriodicTuples(module);
		
		module.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(module).getVmScheduler()
				.getAllocatedMipsForVm(module));
		System.out.println(CloudSim.clock()+" "+module.getName() + " Launched in "+getName());
	}
	
	private void initializePeriodicTuples(AppModule module) {
		String appId = module.getAppId();
		MyApplication app = getMyApplicationMap().get(appId);
		List<AppEdge> periodicEdges = app.getPeriodicEdges(module.getName());
		for(AppEdge edge : periodicEdges){
			send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
		}
	}

	protected void processOperatorRelease(SimEvent ev){
		this.processVmMigrate(ev, false);
	}
	
	
	protected void updateNorthTupleQueue(){
		if(!getNorthTupleQueue().isEmpty()){
			Tuple tuple = getNorthTupleQueue().poll();
			sendUpFreeLink(tuple);
		}else{
			setNorthLinkBusy(false);
		}
	}
	
	protected void sendUpFreeLink(Tuple tuple){
		double networkDelay = tuple.getCloudletFileSize()/getUplinkBandwidth();
		setNorthLinkBusy(true);
		send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
		send(parentId, networkDelay+getUplinkLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());
	}
	
	protected void sendUp(Tuple tuple){
		if(parentId > 0){
			if(!isNorthLinkBusy()){
				sendUpFreeLink(tuple);
			}else{
				northTupleQueue.add(tuple);
			}
		}
	}
	
	
	protected void updateSouthTupleQueue(){
		if(!getSouthTupleQueue().isEmpty()){
			Pair<Tuple, Integer> pair = getSouthTupleQueue().poll(); 
			sendDownFreeLink(pair.getFirst(), pair.getSecond());
		}else{
			setSouthLinkBusy(false);
		}
	}
	
	protected void sendDownFreeLink(Tuple tuple, int childId){
		double networkDelay = tuple.getCloudletFileSize()/getDownlinkBandwidth();
		//Logger.debug(getName(), "Sending tuple with tupleType = "+tuple.getTupleType()+" DOWN");
		setSouthLinkBusy(true);
		double latency = getChildToLatencyMap().get(childId);
		send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE);
		send(childId, networkDelay+latency, FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
	}
	
	protected void sendDown(Tuple tuple, int childId){
		if(getChildrenIds().contains(childId)){
			if(!isSouthLinkBusy()){
				sendDownFreeLink(tuple, childId);
			}else{
				southTupleQueue.add(new Pair<Tuple, Integer>(tuple, childId));
			}
		}
	}
	
	
	protected void sendToSelf(Tuple tuple){
		send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
	}
	public PowerHost getHost(){
		return (PowerHost) getHostList().get(0);
	}
	public int getParentId() {
		return parentId;
	}
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}
	public List<Integer> getChildrenIds() {
		return childrenIds;
	}
	public void setChildrenIds(List<Integer> childrenIds) {
		this.childrenIds = childrenIds;
	}
	public double getUplinkBandwidth() {
		return uplinkBandwidth;
	}
	public void setUplinkBandwidth(double uplinkBandwidth) {
		this.uplinkBandwidth = uplinkBandwidth;
	}
	public double getUplinkLatency() {
		return uplinkLatency;
	}
	public void setUplinkLatency(double uplinkLatency) {
		this.uplinkLatency = uplinkLatency;
	}
	public boolean isSouthLinkBusy() {
		return isSouthLinkBusy;
	}
	public boolean isNorthLinkBusy() {
		return isNorthLinkBusy;
	}
	public void setSouthLinkBusy(boolean isSouthLinkBusy) {
		this.isSouthLinkBusy = isSouthLinkBusy;
	}
	public void setNorthLinkBusy(boolean isNorthLinkBusy) {
		this.isNorthLinkBusy = isNorthLinkBusy;
	}
	public int getControllerId() {
		return controllerId;
	}
	public void setControllerId(int controllerId) {
		this.controllerId = controllerId;
	}
	public List<String> getActiveMyApplications() {
		return activeMyApplications;
	}
	public void setActiveMyApplications(List<String> activeMyApplications) {
		this.activeMyApplications = activeMyApplications;
	}
	public Map<Integer, List<String>> getChildToOperatorsMap() {
		return childToOperatorsMap;
	}
	public void setChildToOperatorsMap(Map<Integer, List<String>> childToOperatorsMap) {
		this.childToOperatorsMap = childToOperatorsMap;
	}

	public Map<String, MyApplication> getMyApplicationMap() {
		return applicationMap;
	}

	public void setMyApplicationMap(Map<String, MyApplication> applicationMap) {
		this.applicationMap = applicationMap;
	}

	public Queue<Tuple> getNorthTupleQueue() {
		return northTupleQueue;
	}

	public void setNorthTupleQueue(Queue<Tuple> northTupleQueue) {
		this.northTupleQueue = northTupleQueue;
	}

	public Queue<Pair<Tuple, Integer>> getSouthTupleQueue() {
		return southTupleQueue;
	}

	public void setSouthTupleQueue(Queue<Pair<Tuple, Integer>> southTupleQueue) {
		this.southTupleQueue = southTupleQueue;
	}

	public double getDownlinkBandwidth() {
		return downlinkBandwidth;
	}

	public void setDownlinkBandwidth(double downlinkBandwidth) {
		this.downlinkBandwidth = downlinkBandwidth;
	}

	public List<Pair<Integer, Double>> getAssociatedMyActuatorIds() {
		return associatedMyActuatorIds;
	}

	public void setAssociatedMyActuatorIds(List<Pair<Integer, Double>> associatedMyActuatorIds) {
		this.associatedMyActuatorIds = associatedMyActuatorIds;
	}
	
	public double getEnergyConsumption() {
		return energyConsumption;
	}

	public void setEnergyConsumption(double energyConsumption) {
		this.energyConsumption = energyConsumption;
	}
	public Map<Integer, Double> getChildToLatencyMap() {
		return childToLatencyMap;
	}

	public void setChildToLatencyMap(Map<Integer, Double> childToLatencyMap) {
		this.childToLatencyMap = childToLatencyMap;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public double getRatePerMips() {
		return ratePerMips;
	}

	public void setRatePerMips(double ratePerMips) {
		this.ratePerMips = ratePerMips;
	}
	public double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}

	public Map<String, Map<String, Integer>> getModuleInstanceCount() {
		return moduleInstanceCount;
	}

	public void setModuleInstanceCount(
			Map<String, Map<String, Integer>> moduleInstanceCount) {
		this.moduleInstanceCount = moduleInstanceCount;
	}
	
	//////////////////////////////////////////
	private int mips;

	public int getMips() {
		return mips;
	}

	public void setMips(int mips) {
		this.mips = mips;
	}
	
	
	public void setNumUpTuples(int num)
	{
		this.numUpTuples = num;
	}
	
	public int getNumUpTuples()
	{
		return this.numUpTuples;
	}
	
	public void setNumDownTuples(int num)
	{
		this.numDownTuples = num;
	}
	
	public int getNumDownTuples()
	{
		return this.numDownTuples;
	}
	
	public void setEmittedTuplesIdList(List<Integer> lst)
	{
		this.emittedTuplesId = lst;
	}
	
	public List<Integer> getEmittedTuplesIdList()
	{
		return this.emittedTuplesId;
	}
	
	public List<String> getAssociatedEndDevices()
	{
		return this.associatedEndDevices;
	}
	
	public void setAssociatedEndDevices(List<String> lst)
	{
		this.associatedEndDevices = lst;
	}
	
	
	//MAT 27/6/2019
		 public List<SecurityCounterMeasure> getSecurityMeasures() {
		        return securityMeasures;
		    }

		    public void setSecurityMeasures(List<String> sm) {
		        List<SecurityCounterMeasure> measures = new ArrayList<SecurityCounterMeasure>();
		        for(String measure : sm){
		            SecurityCounterMeasure newSecurityMeasure = new SecurityCounterMeasure(measure);
		            measures.add(newSecurityMeasure);
		        }
		        this.securityMeasures = measures;
		    }

		    public ArrayList<String> getSecurityMeasuresByType(String type){
		        ArrayList<String> result = new ArrayList<>();
		        for (SecurityCounterMeasure s : this.securityMeasures){
		            if (SecurityTaxonomy.getSecurityMeasuresByType(type).contains(s)){
		                result.add(s.toString());
		            }
		        }
		        return result;
		    }

		    public int computeNodeSecurityScore(){
		        int result = 0;
		        for (SecurityCounterMeasure s: this.securityMeasures){
		            result+=s.getWeight();
		        }
		        return result;
		    }
		    
		    
		    public double computePercentNodeSecurityScore()
		    {
		    	double result =0;
		    	
		    	result = 100 * this.computeNodeSecurityScore()/ SecurityConstants.allCountermeasures.size();
		    	
		    	return result;
		    }
		    
		    
		    public double computePercentNodeSecurityScoreByType(String type)
		    {
		    	double result =0;
		    	
		    	double tmp1 = this.getSecurityMeasuresByType(type).size();
		    	
		    	double tmp2 = SecurityTaxonomy.getSecurityMeasuresByType(type).size();
		    	
		    	result =100 * tmp1/tmp2;
		    	
		    	return result;
		    } 
		    
		    
		    public int computeNodeSecurityScoreByType(String type){
		        int result = 0;
		        List<String> measures = SecurityTaxonomy.getSecurityMeasuresByType(type);
		        for (SecurityCounterMeasure s: this.securityMeasures) {
		            if (measures.contains(s.getName())) {
		                result += s.getWeight();
		            }
		        }
		        return result;
		    }

		    public boolean supportsSecurityRequirements(List<String> requirements){
		        ArrayList<String> measures = new ArrayList<>();
		        if (this.securityMeasures != null) {
		            for (SecurityCounterMeasure s : this.securityMeasures) {
		                measures.add(s.toString());
		            }
		        }
		        return measures.containsAll(requirements);
		    }
		    
		    
		    public int getNumberOfSupportedSecurityRequirement(List<String> requirements)
		    {
		    
		    	int num =0;
		    	
		    	 ArrayList<String> measures = new ArrayList<>();
			        if (this.securityMeasures != null) {
			            for (SecurityCounterMeasure s : this.securityMeasures) {
			                measures.add(s.toString());
			                
			            //    System.out.println("Measure  " + s.toString());
			                
			            }
			        }
			    
			     
			   
			   for (String s: requirements)
			   {
				   
				   if (measures.contains(s))
				   {
					//   System.out.println("Measures contains:   " + s);
					   
					   num = num +1;
				   }
			   }
			       
			   
			//   System.out.println(num);
			   
		    	return num;
		    }
		    
		     
		    public int getCriticalityCieling()
		    {
		    	return this.criticalityCeling;
		    }
		    
		    public void setCrticalityCeiling(int ceil)
		    {
		    	
		    	if (ceil > this.criticalityCeling)
		    	{
		    	
		    	this.criticalityCeling = ceil;
		    	}
		    }
		    
		 public double computeSuitabilityToAppModule(AppModule md)
		 {
			 double st= 0;
			 int tmp1=0;
			 
			 
			 if (md.getNonEssnetialSecurityRequirements() !=null)
			 {
			  tmp1 = md.getNonEssnetialSecurityRequirements().size();
			 }
			 int tmp2 = this.getNumberOfSupportedSecurityRequirement(md.getNonEssnetialSecurityRequirements());
			 
			 System.out.println(this.getName() + "  supports    " + tmp2 + "     for module  " + md.getName());
			 
			 
			 if (md.getEssnetialSecurityRequirements() == null )
			 {
				 if (md.getNonEssnetialSecurityRequirements() == null )
				 {
					 
					 System.out.println("Line 1144  " +   this.getName() + "     for module  " + md.getName());
					 st =1;
				 }
				 else if (this.supportsSecurityRequirements(md.getNonEssnetialSecurityRequirements()))
				 {
					 
					 System.out.println("Line 1150  " +   this.getName() + "     for module  " + md.getName());
					 st = 1;
				 }
				 else
				 {
					 
					 System.out.println("Line 1156  " +   this.getName() +"     for module  " + md.getName());
					 st = tmp2/tmp1;
				 }
			 }
			 
			 else 
				 {
				 if (this.supportsSecurityRequirements(md.getEssnetialSecurityRequirements()))
					 st= 1;
				 else
					 st=0;
				 }
			
			 
			 return st;
		 }
		 
		 public double computeSuitabilityToAppModule(List<String> ess, List<String> nonEss)
		 {
			 double st= 0;
			 int tmp1=0;
			 int tmp2=0;
			 
			 List<String> lst = new ArrayList<>();
			 lst.addAll(ess);
			 lst.addAll(nonEss);
			 
			 if (this.supportsSecurityRequirements(lst))
			 {
				// System.out.print("Support All Requirements");
				 
				 st = (double)2;
			 }
			 else if (this.supportsSecurityRequirements(ess))
			 {
				// System.out.println("Support Esssential but Some NonEssnetial");
				 
				 tmp1 = nonEss.size();
				 tmp2 = getNumberOfSupportedSecurityRequirement(nonEss);
				 double tmp = (double)tmp2/tmp1;
				 st = (double)(1)+ tmp;
			 }
			 else 
			 {
				 //System.out.println("Doesnot Support Esssential but Some NonEssnetial");
				 tmp2 = getNumberOfSupportedSecurityRequirement(nonEss);
				// System.out.println(tmp2);
				 //System.out.println(lst.size());
				 st = (double)tmp2/lst.size();
				 
				 //System.out.println(st);
			 }
			 
			 return st;
		 }
		 
		 
		 
		 
		 
		 
		public void setCriticalityLevel (int l)
		{
			this.criticalityLevel = l;
		}
		
		public int getCriticalityLevel()
		{
			return this.criticalityLevel;
		}
		
		public void setCurrentCriticalityLevel (int l)
		{
			this.currentCriticalityLevel = l;
		}
		
		public int getCurrentCriticalityLevel()
		{
			return this.currentCriticalityLevel;
		}
		
		public void setKnapsack(Knapsack sk)

		{
			this.sack = sk;
		}
		
		public Knapsack getKnapsack()
		{
			return this.sack;
		}
		
		public void setKnapsackSolution(Solution sol)
		{
			this.solution =sol;
		}
		
		public Solution getKnapsackSolution()
		{
			return this.solution;
		}
		
		public void setRollOverItems(List<Item> lst)
		{
			this.rollOverItems = lst;
		}
		
		public List<Item> getrollOverItems()
		{
			return this.rollOverItems;
		}
		
		public double getMaxSuitabilityFactor()
		{
			return this.maxSuitabilityFactor;
		}
		
		public void setMaxSuitabilityFactor(double val)
		{
			this.maxSuitabilityFactor = val;
		}
		
		public void setChildEndDeviceIds(List<Integer> ls)
		{
			this.childEndDeviceIds = ls;
		}
		
		public List<Integer> getChildEndDeviceIds()
		{
			return this.childEndDeviceIds;
		}
		
		public Map<Integer,Double> getChildCripMap()
		{
			return this.childCritMap;
		}
		
		public Map<Integer,Double> getChildSecMap()
		{
			return this.childSecMap;
		}
		
		public Map<Integer,Double> getChildMipsMap()
		{
			return this.childMipsMap;
		}
		
		public void setChildCritMap (Map<Integer,Double> mp)
		{
			this.childCritMap = mp;
		}
		
		public void setChildSecMap (Map<Integer,Double> mp)
		{
			this.childSecMap = mp;
		}
		
		public void setChildMipsMap (Map<Integer,Double> mp)
		{
			this.childMipsMap = mp;
		}
		
		public void setAggProfitValue(double val)
		{
			this.aggProfitValue = val;
		}
		
		public double getAggProfitValue()
		{
			return this.aggProfitValue;
		}
		
		public void setPrefFogDeviceName(String nm)
		{
			this.prefFogDeviceName = nm;
		}
		
		public String getPrefFogDeviceName()
		{
			return this.prefFogDeviceName;
		}
		
		public void setEssSecReq(List<String> lst)
		{
			this.essSecReq = lst;
		}
		
		public List<String> getEssSecReq()
		{
			return this.essSecReq;
		}
		
		public void setNonEssSecReq(List<String> lst)
		{
			this.nonEssSecReq = lst;
		}
		
		public List<String> getNonEssSecReq()
		{
			return this.nonEssSecReq;
		}
		
		
		public void updateSecTypleCount(double suit)
		{
			
			String tmp;
			if (suit == (double)2)
			tmp = "allSec";
			else if (suit > (double) 1)
				tmp = "allEssSomeNonEss";
			else if (suit == (double)1)
				tmp = "onleEss";
			else if (suit > (double)0)
				tmp = "someNonEss";
			else tmp = "none";
			
			
			
			if(!TimeKeeper.getInstance().getSecTypleToExecutedTupleCount().containsKey(tmp)){
				TimeKeeper.getInstance().getSecTypleToExecutedTupleCount().put(tmp,(double)1);
				
			} else{
				
				double currentCount = TimeKeeper.getInstance().getSecTypleToExecutedTupleCount().get(tmp);
				currentCount = currentCount +1;
			    TimeKeeper.getInstance().getSecTypleToExecutedTupleCount().put(tmp,currentCount+1);
			}
			}
		
		public void setTotalPower(double pwr)
		{
			this.totalPower=pwr;
		}
		
		public double getTotalPower()
		{
			return this.totalPower;
		}
		
		
		
		public double getMinCritVal()
		{
			return this.minCritVal;
		}
		
		public void setMinCritVal(double val)
		{
			this.minCritVal = val;
		}
		
		public double getMinSecVal()
		{
			return this.minSecVal;
		}
		
		public void setMinSecVal(double val)
		{
			this.minSecVal = val;
		}
		
		
		
		public double getMaxCritVal()
		{
			return this.maxCritVal;
		}
		
		public void setMaxCritVal(double val)
		{
			this.maxCritVal = val;
		}
		
		public double getMaxSecVal()
		{
			return this.maxSecVal;
		}
		
		public void setMaxSecVal(double val)
		{
			this.maxSecVal = val;
		}

		
		
		
		
}

