package org.fog.placement;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.MyApplication;
import org.fog.entities.MyActuator;
import org.fog.entities.MyFogDevice;
import org.fog.entities.MySensor;
import org.fog.security.SecurityParametersTypes;
import org.fog.security.SecurityTaxonomy;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;

public class MyController extends SimEntity{
	
	public static boolean ONLY_CLOUD = false;
		
	private List<MyFogDevice> fogDevices;
	private List<MySensor> sensors;
	private List<MyActuator> actuators;
	
	private Map<String, MyApplication> applications;
	private Map<String, Integer> appLaunchDelays;

	private Map<String, MyPlacement> appModulePlacementPolicy;
	
	public MyController(String name, List<MyFogDevice> fogDevices, List<MySensor> sensors, List<MyActuator> actuators) {
		super(name);
		this.applications = new HashMap<String, MyApplication>();
		setAppLaunchDelays(new HashMap<String, Integer>());
		setAppModulePlacementPolicy(new HashMap<String, MyPlacement>());
		for(MyFogDevice fogDevice : fogDevices){
			fogDevice.setControllerId(getId());
		}
		setMyFogDevices(fogDevices);
		setMyActuators(actuators);
		setMySensors(sensors);
		connectWithLatencies();
	}

	private MyFogDevice getMyFogDeviceById(int id){
		for(MyFogDevice fogDevice : getMyFogDevices()){
			if(id==fogDevice.getId())
				return fogDevice;
		}
		return null;
	}
	
	private void connectWithLatencies(){
		for(MyFogDevice fogDevice : getMyFogDevices()){
			MyFogDevice parent = getMyFogDeviceById(fogDevice.getParentId());
			if(parent == null)
				continue;
			double latency = fogDevice.getUplinkLatency();
			parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
			parent.getChildrenIds().add(fogDevice.getId());
		}
	}
	
	@Override
	public void startEntity() {
		for(String appId : applications.keySet()){
			if(getAppLaunchDelays().get(appId)==0)
				processAppSubmit(applications.get(appId));
			else
				send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
		}

		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
		
		send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);
		
		for(MyFogDevice dev : getMyFogDevices())
			sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);

	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.TUPLE_FINISHED:
			processTupleFinished(ev);
			break;
		case FogEvents.CONTROLLER_RESOURCE_MANAGE:
			manageResources();
			break;
		case FogEvents.STOP_SIMULATION:
			CloudSim.stopSimulation();
			printTimeDetails();
			printPowerDetails();
			printCostDetails();
			printNetworkUsageDetails();
			printNumSensorEmittedTuples();
			printNumActuatorRecievedTuples();
			printEndDeviceDetails();
			printEndDeviceDownDetails();
			printEndDeviceDelayDetails();
			printCoreDeviceDelayDetails();
			printTupleTypeDelayDetails();
			printSecTypeCountDetails();
			showDetailedSecurityScoresofFogDevices();
			showEndDevicesDetailedSecurityReuirements();
			printDevicePowerDetails();
			System.exit(0);
			break;
			
		}
	}
	
	private void printNetworkUsageDetails() {
		System.out.println("Total network usage = "+NetworkUsageMonitor.getNetworkUsage()/Config.MAX_SIMULATION_TIME);		
	}

	private MyFogDevice getCloud(){
		for(MyFogDevice dev : getMyFogDevices())
			if(dev.getName().equals("cloud"))
				return dev;
		return null;
	}
	
	private void printCostDetails(){
		System.out.println("Cost of execution in cloud = "+getCloud().getTotalCost());
	}
	
	
	private void printNumSensorEmittedTuples() {
		
		
         System.out.println();
		
		System.out.println("======== Sensor Details==========");
		for(MySensor sensor : this.getMySensors()){
			System.out.println(sensor.getName() + " : Emitted Tuples = "+ sensor.getNumEmittedTuples());
		}
	}
	
	
	private void printNumActuatorRecievedTuples() {
		
		
        System.out.println();
		
		System.out.println("======== Actuator Details==========");
		for(MyActuator act : this.getMyActuators()){
			System.out.println(act.getName() + " : Recieved Tuples = "+ act.getNumRecievedTuples());
		}
	}
	
     private void printEndDeviceDetails() {
		
    	 System.out.println("======== Up Tuples Details==========");
		for(MyFogDevice fogDevice : getMyFogDevices()){
			
			if (fogDevice.getLevel()==2 || fogDevice.getLevel()==1)
			System.out.println(fogDevice.getName() + " : Up Tuples = "+fogDevice.getNumUpTuples());
		}
	}
     
     private void printEndDeviceDownDetails() {
 		
    	 System.out.println("======== Down Tuples Details==========");
		for(MyFogDevice fogDevice : getMyFogDevices()){
			
			if (fogDevice.getLevel()==2 )
			System.out.println(fogDevice.getName() + " : Down Tuples = "+fogDevice.getNumDownTuples());
		}
	}
     
     
     private void printEndDeviceDelayDetails() {
    	 
    	 double lowCritCntr =0;
    	 double highCritCntr =0;
    	 double cntr =0;
  		
    	 System.out.println("======== End Device Average Delay Details==========");
		for(String name: TimeKeeper.getInstance().getEndDeviceToDelayMap().keySet() ){
			
			String end = TimeKeeper.getInstance().getEndDeviceToDeviceMap().get(name);
			MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(name);
			
			if (dv.getCriticalityLevel() ==0)
				lowCritCntr++;
			else
				highCritCntr++;
			cntr++;
			System.out.println(name + "  Crit:   " + dv.getCriticalityLevel() +"  : Average tuple Delay = "+ TimeKeeper.getInstance().getEndDeviceToDelayMap().get(name) + " Linked to:  " + end );
		}
		
		System.out.println("Low Crit Count: " + lowCritCntr + " <--> Percentage:  " + (double)(lowCritCntr/cntr));
		System.out.println(" High Crit Count: " + highCritCntr + " <--> Percentage:  " + (double)(highCritCntr/cntr));
	}
	
	
     private void printCoreDeviceDelayDetails() {
   		System.out.println();
    	 System.out.println("======== End Device Average Delay Details==========");
    	 int hCount =0;
    	 int lCount=0;
		for(String name: TimeKeeper.getInstance().getCoreDeviceToDelayMap().keySet() ){
			
			hCount=0;
			lCount=0;
			MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(name);
			//String end = TimeKeeper.getInstance().getCoreDeviceToDelayMap().get(name);
			List<String> lst = dv.getAssociatedEndDevices();
			
			for (String child: lst)
			{
				MyFogDevice dv_1 = (MyFogDevice) CloudSim.getEntity(child);
				if (dv_1.getCriticalityLevel() ==0)
				lCount++;
				else
				hCount++;
			}
			
		//	System.out.println(name + " : Average tuple Delay = "+ TimeKeeper.getInstance().getCoreDeviceToDelayMap().get(name) + "  Associated Devices:  " + dv.getAssociatedEndDevices().size() + "   " + dv.getAssociatedEndDevices() + " Knapsack solution:  " + dv.getKnapsackSolution().getSolutionItemNames());
			System.out.println(name + " : Average tuple Delay = "+ TimeKeeper.getInstance().getCoreDeviceToDelayMap().get(name) + "  Associated Devices:  " + dv.getAssociatedEndDevices().size() + "  ( " + hCount + "," + lCount +" )"+ "  "+dv.getAssociatedEndDevices());
		//System.out.println(name + " : Average tuple Delay = "+ TimeKeeper.getInstance().getCoreDeviceToDelayMap().get(name));

			//if (dv.getLevel()==2)
		    //{
		   // 	dv.getKnapsack().display();
		    //	dv.getKnapsackSolution().display();
		    //}
		
		
		}
	}
     
     
     private void printTupleTypeDelayDetails() {
    		System.out.println();
     	 System.out.println("======== Criticality Typle Delay Details==========");
 		for(String name: TimeKeeper.getInstance().getCritTypleToAverageResponseTime().keySet() ){
 			
 			MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(name);
 			//String end = TimeKeeper.getInstance().getCoreDeviceToDelayMap().get(name);
 			
 			System.out.println(name + " : Average tuple Delay = "+ TimeKeeper.getInstance().getCritTypleToAverageResponseTime().get(name) + " No.:   " + TimeKeeper.getInstance().getCritTypleToTupleCount().get(name));
 		}
 	} 
     
     private void printSecTypeCountDetails() {
 		System.out.println();
  	 System.out.println("======== Security Typle Count Details==========");
		for(String name: TimeKeeper.getInstance().getSecTypleToExecutedTupleCount().keySet() ){
			
			MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(name);
			//String end = TimeKeeper.getInstance().getCoreDeviceToDelayMap().get(name);
			
			System.out.println(name + " : Number of Tuples = "+ TimeKeeper.getInstance().getSecTypleToExecutedTupleCount().get(name));
		}
	} 
     
	private void printPowerDetails() {
		
		
		
		for(MyFogDevice fogDevice : getMyFogDevices()){
			System.out.println(fogDevice.getName() + " : Energy Consumed = "+fogDevice.getEnergyConsumption());
		}
	}
	
   private void printDevicePowerDetails() {
		
		System.out.println();
		System.out.println("======================= Device Power Details =================");
		
		for(MyFogDevice fogDevice : getMyFogDevices()){
			System.out.println(fogDevice.getName() + " : Power Consumed = "+fogDevice.getTotalPower());
		}
	}

	private String getStringForLoopId(int loopId){
		for(String appId : getMyApplications().keySet()){
			MyApplication app = getMyApplications().get(appId);
			for(AppLoop loop : app.getLoops()){
				if(loop.getLoopId() == loopId)
					return loop.getModules().toString();
			}
		}
		return null;
	}
	private void printTimeDetails() {
		System.out.println("=========================================");
		System.out.println("============== RESULTS ==================");
		System.out.println("=========================================");
		System.out.println("EXECUTION TIME : "+ (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()));
		System.out.println("=========================================");
		System.out.println("APPLICATION LOOP DELAYS");
		System.out.println("=========================================");
		for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()){
		
			System.out.println(getStringForLoopId(loopId) + " ---> "+TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId));
		}
		System.out.println("=========================================");
		System.out.println("TUPLE CPU EXECUTION DELAY");
		System.out.println("=========================================");
		
		for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()){
			System.out.println(tupleType + " ---> "+TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
		}
		
		System.out.println("=========================================");
	}

	protected void manageResources(){
		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
	}
	
	private void processTupleFinished(SimEvent ev) {
	}
	
	@Override
	public void shutdownEntity() {	
	}
	
	public void submitApplication(MyApplication application, int delay, MyModulePlacement modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(MySensor sensor : sensors){
			sensor.setApp(getMyApplications().get(sensor.getAppId()));
		}
		for(MyActuator ac : actuators){
			ac.setApp(getMyApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(MyActuator actuator : getMyActuators()){
					if(actuator.getMyActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	
	public void submitApplication(MyApplication application, int delay, MyModulePlacementNSGA modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(MySensor sensor : sensors){
			sensor.setApp(getMyApplications().get(sensor.getAppId()));
		}
		for(MyActuator ac : actuators){
			ac.setApp(getMyApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(MyActuator actuator : getMyActuators()){
					if(actuator.getMyActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	public void submitApplication(MyApplication application, int delay, MyModulePlacementNSGA2 modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(MySensor sensor : sensors){
			sensor.setApp(getMyApplications().get(sensor.getAppId()));
		}
		for(MyActuator ac : actuators){
			ac.setApp(getMyApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(MyActuator actuator : getMyActuators()){
					if(actuator.getMyActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	public void submitApplication(MyApplication application, int delay, MyModulePlacementNSGA2Affin modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(MySensor sensor : sensors){
			sensor.setApp(getMyApplications().get(sensor.getAppId()));
		}
		for(MyActuator ac : actuators){
			ac.setApp(getMyApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(MyActuator actuator : getMyActuators()){
					if(actuator.getMyActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	
	public void submitApplication(MyApplication application, int delay, MyModulePlacementNSGASec1 modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(MySensor sensor : sensors){
			sensor.setApp(getMyApplications().get(sensor.getAppId()));
		}
		for(MyActuator ac : actuators){
			ac.setApp(getMyApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(MyActuator actuator : getMyActuators()){
					if(actuator.getMyActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	
	public void submitApplication(MyApplication application, int delay, MyModulePlacementNSGASec2 modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(MySensor sensor : sensors){
			sensor.setApp(getMyApplications().get(sensor.getAppId()));
		}
		for(MyActuator ac : actuators){
			ac.setApp(getMyApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(MyActuator actuator : getMyActuators()){
					if(actuator.getMyActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	
	
	
	
	public void submitApplication(MyApplication application, int delay, MyModulePlacementKnapSack modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(MySensor sensor : sensors){
			sensor.setApp(getMyApplications().get(sensor.getAppId()));
		}
		for(MyActuator ac : actuators){
			ac.setApp(getMyApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(MyActuator actuator : getMyActuators()){
					if(actuator.getMyActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	
	public void submitApplication(MyApplication application, int delay, MyModulePlacementKnapSackTwoObjs modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(MySensor sensor : sensors){
			sensor.setApp(getMyApplications().get(sensor.getAppId()));
		}
		for(MyActuator ac : actuators){
			ac.setApp(getMyApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(MyActuator actuator : getMyActuators()){
					if(actuator.getMyActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	public void submitApplication(MyApplication application, int delay, MyModulePlacementCloudOnly modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(MySensor sensor : sensors){
			sensor.setApp(getMyApplications().get(sensor.getAppId()));
		}
		for(MyActuator ac : actuators){
			ac.setApp(getMyApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(MyActuator actuator : getMyActuators()){
					if(actuator.getMyActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	public void submitApplication(MyApplication application, MyModulePlacement modulePlacement){
		submitApplication(application, 0, modulePlacement);
	}
	
	
	private void processAppSubmit(SimEvent ev){
		MyApplication app = (MyApplication) ev.getData();
		processAppSubmit(app);
	}
	
	private void processAppSubmit(MyApplication application){
		System.out.println(CloudSim.clock()+" Submitted application "+ application.getAppId());
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getMyApplications().put(application.getAppId(), application);
		
		MyPlacement modulePlacement = getAppModulePlacementPolicy().get(application.getAppId());
		for(MyFogDevice fogDevice : fogDevices){
			sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
		}
		
		Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
		
		
		for(Integer deviceId : deviceToModuleMap.keySet()){
			for(AppModule module : deviceToModuleMap.get(deviceId)){
				
				sendNow(deviceId, FogEvents.APP_SUBMIT, application);
				System.out.println(CloudSim.clock()+" Trying to Launch "+ module.getName() + " in "+getMyFogDeviceById(deviceId).getName());
				sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
			}
		}
		
	}

	public List<MyFogDevice> getMyFogDevices() {
		return fogDevices;
	}

	public void setMyFogDevices(List<MyFogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Map<String, Integer> getAppLaunchDelays() {
		return appLaunchDelays;
	}

	public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
		this.appLaunchDelays = appLaunchDelays;
	}

	public Map<String, MyApplication> getMyApplications() {
		return applications;
	}

	public void setMyApplications(Map<String, MyApplication> applications) {
		this.applications = applications;
	}

	public List<MySensor> getMySensors() {
		return sensors;
	}

	public void setMySensors(List<MySensor> sensors) {
		for(MySensor sensor : sensors)
			sensor.setControllerId(getId());
		this.sensors = sensors;
	}

	public List<MyActuator> getMyActuators() {
		return actuators;
	}

	public void setMyActuators(List<MyActuator> actuators) {
		this.actuators = actuators;
	}

	public Map<String, MyPlacement> getAppModulePlacementPolicy() {
		return appModulePlacementPolicy;
	}

	public void setAppModulePlacementPolicy(Map<String, MyPlacement> appModulePlacementPolicy) {
		this.appModulePlacementPolicy = appModulePlacementPolicy;
	}
	
	
	public void showDetailedSecurityScoreofFogDevice(MyFogDevice device)
    {
    
    //	Datacenter dc = this.getEdgeDCbyName(dcName);
        
        
        double val = device.computeNodeSecurityScore();
        
        double val1 = device.computePercentNodeSecurityScore();
        
        String name = device.getName();
        
        int id = device.getId();
        
        System.out.println("**********************************************************************\n");
       
    	 System.out.println(("\t\t " + name + "  - Overall Security Score: " + Double.toString(val) +"    Percent    " +  val1+ "  \n"));
    	
      for (String type : (SecurityParametersTypes.getParametersTypes())) {
     	 
     	 ArrayList<String> active_measures = device.getSecurityMeasuresByType(type);
          List<String> allMeasures = SecurityTaxonomy.getSecurityMeasuresByType(type);
          double size = allMeasures.size();
          allMeasures.removeAll(active_measures);
        System.out.println("\t\t\t - " + type + ": " + Double.toString(device.computeNodeSecurityScoreByType(type)) +  "  "+ Double.toString(device.computePercentNodeSecurityScoreByType(type)) + "     missing " + allMeasures + "\n");
      }
    	
    }
    
   
    public  void showDetailedSecurityScoresofFogDevices()
    {
    	for (MyFogDevice device: this.getMyFogDevices())
    	{
    		
    		if (device.getLevel() < 3)
    		showDetailedSecurityScoreofFogDevice(device);
    	}
    }
    
    
    public  void showEndDevicesDetailedSecurityReuirements()
    {
    	for (MyFogDevice device: this.getMyFogDevices())
    	{
    		
    		if (device.getLevel() == 3)
    		{
    			System.out.println("************ End Device Security Requirements===================");
    			System.out.println(device.getName() + "  Essential---->  " + device.getEssSecReq()  +  "    NonEssential ---- >   " + device.getNonEssSecReq() );
    		}
    	}
    }
}