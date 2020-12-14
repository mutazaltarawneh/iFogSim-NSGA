package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.MyApplication;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.MyActuator;
import org.fog.entities.MyFogDevice;
import org.fog.entities.MySensor;
import org.fog.entities.Tuple;
import org.fog.placement.ModuleMapping;
import org.fog.placement.MyController;
import org.fog.placement.MyModulePlacement;
import org.fog.placement.MyModulePlacementCloudOnly;
import org.fog.placement.MyModulePlacementKnapSack;
import org.fog.placement.MyModulePlacementKnapSackTwoObjs;
import org.fog.placement.MyModulePlacementNSGA;
import org.fog.placement.MyModulePlacementNSGA2;
import org.fog.placement.MyModulePlacementNSGA2Affin;
import org.fog.placement.MyModulePlacementNSGASec1;
import org.fog.placement.MyModulePlacementNSGASec2;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.security.SecurityConstants;
import org.fog.utils.FogLinearPowerModel;

import org.fog.utils.PowerModelSpecPowerIbmX3550XeonX5675;

import org.fog.utils.PowerModelSpecPowerHuaweiXH310XeonX3470;
import org.fog.utils.PowerModelSpecPowerIbmX3550XeonX5670;
import org.fog.utils.PowerModelSpecPowerIbmX3250XeonX3480;

import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.UniformDistribution;


public class TestApplication {
	static List<MyFogDevice> fogDevices = new ArrayList<MyFogDevice>();
	static Map<Integer,MyFogDevice> deviceById = new HashMap<Integer,MyFogDevice>();
	static List<MySensor> sensors = new ArrayList<MySensor>();
	static List<MyActuator> actuators = new ArrayList<MyActuator>();
	static List<Integer> idOfEndDevices = new ArrayList<Integer>();
	static Map<Integer, Map<String, Double>> deadlineInfo = new HashMap<Integer, Map<String, Double>>();
	static Map<Integer, Map<String, Integer>> additionalMipsInfo = new HashMap<Integer, Map<String, Integer>>();
	static Map<Integer, Map<String,List<String>>> moduleEssSecReqInfo = new HashMap<Integer, Map<String,List<String>>>();
	static Map<Integer, Map<String,List<String>>> moduleNonEssSecReqInfo = new HashMap<Integer, Map<String,List<String>>>();
	
	static Map<Integer, Map<String, Integer>> criticalityInfo = new HashMap<Integer, Map<String, Integer>>();
	
	static Map<Integer, Map<String, Double>> sensingFreqInfo = new HashMap<Integer, Map<String, Double>>();
	
	static Map<Integer, Map<String, Double>> inputDataInfo = new HashMap<Integer, Map<String, Double>>();
	
	static Map<Integer, Map<String, Integer>> privacyInfo = new HashMap<Integer, Map<String, Integer>>();
	static Map<Integer, Map<String, Integer>> privacyCountInfo = new HashMap<Integer, Map<String, Integer>>();
	
	

	
	static boolean CLOUD = simConstants.CLOUD;
	
	static int numOfGateways = simConstants.numOfGateways;
	static int numOfEndDevPerGateway = simConstants.numOfEndDevPerGateway;
	static double sensingInterval = simConstants.sensingInterval; 
	static double minInterval =simConstants.minInterval;
	static double maxInterval =simConstants.maxInterval;
	
	static double secWeight = simConstants.secWeight;
	static double critWeight = simConstants.critWeight;
	
	static double posCons =simConstants.posCons;
	static double negCons = simConstants.negCons;
	
	static int secUnionLimit = simConstants.secUnionLimit;
	
	static protected int critUsagePercentage[] = {20,30,50};
	
	static protected double critPercent =simConstants.critPercent;
	
	public static void main(String[] args) {
		
		Log.printLine("Starting TestApplication...");

		try {
			Log.disable();
			//Log.enable();
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; 
			CloudSim.init(num_user, calendar, trace_flag);
			String appId = "test_app"; 
			FogBroker broker = new FogBroker("broker");
			
			createFogDevices(broker.getId(), appId);
			
			Random rnd = new Random();
			
			MyApplication application = createApplication(appId, broker.getId());
			//MyApplication application = createApplication(appId, broker.getId(),secWeight,critWeight);
			application.setUserId(broker.getId());
			application.setCritWeight(critWeight);
            application.setSecWeight(secWeight);			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); 
			
			moduleMapping.addModuleToDevice("storageModule", "cloud"); 
		//	moduleMapping.addModuleToDevice("mainModule", "cloud");
			for(int i=0;i<idOfEndDevices.size();i++)
			{
				MyFogDevice fogDevice = deviceById.get(idOfEndDevices.get(i));
				moduleMapping.addModuleToDevice("clientModule", fogDevice.getName()); 
			}
			
			
			MyController controller = new MyController("master-controller", fogDevices, sensors, actuators);
			
			if (CLOUD)
			{
			controller.submitApplication(application, 0, new MyModulePlacementCloudOnly(fogDevices, sensors, actuators, application, moduleMapping,"mainModule"));
			}
			else
				controller.submitApplication(application, 0, new MyModulePlacementNSGA2(fogDevices, sensors, actuators, application, moduleMapping,"mainModule"));

				
			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			
			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.printLine("TestApplication finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
		
		
		}
		
	
	
	private static double getvalue(double min, double max)
	{
		Random r = new Random();
		double randomValue = min + (max - min) * r.nextDouble();
		return randomValue;
	}
	
	static List<String> getUnionSecurityMeasures(int limit)
	{
		List<String> lst = new ArrayList<String>();
		for (int i=0; i< limit;i++)
		{
			int indx = getvalue(0,SecurityConstants.secSets.size()-1);
			if (indx < 0)
				indx = -1 * indx;
			lst.addAll(SecurityConstants.secSets.get(indx));
		}
		return lst;
	}
	
	private static int getvalue(int min, int max)
	{
		Random r = new Random();
		int randomValue = min + r.nextInt()%(max - min);
		
		if (randomValue < 0)
			randomValue = -1 * randomValue;
			
		
		return randomValue;
	}

	private static void createFogDevices(int userId, String appId) {
		MyFogDevice cloud = createFogDevice("cloud", 89600, 40000, 10000, 10000, 0, 0.01, 16*103, 16*83.25);
		cloud.setParentId(-1);
		
		
		int listSize = SecurityConstants.secTypes.size();
		int indx = getvalue(0,listSize-1);
		
		
	//	if (indx < 0)
	//		indx = indx * -1;
	//	cloud.setSecurityMeasures(SecurityConstants.secTypes.get(indx));
		
    
		
		List<String> lst1 = getUnionSecurityMeasures(simConstants.secUnionLimitCloud);
	    cloud.setSecurityMeasures(lst1);
		
	//    cloud.setSecurityMeasures(SecurityConstants.allCountermeasures);
		
    // 	List<String> lst1 = getUnionSecurityMeasures(secUnionLimit);
	//	cloud.setSecurityMeasures(lst1);
		//cloud.setSecurityMeasures(SecurityConstants.secType2);
		fogDevices.add(cloud);
		deviceById.put(cloud.getId(), cloud);
		
		MyFogDevice proxy = createFogDevice("proxy-server", 9600, 4000, 20000, 20000, 1, 0.0, 107.339, 83.4333);
		proxy.setParentId(cloud.getId());
		proxy.setUplinkLatency(150);
		
		
        int indx1 = getvalue(0,listSize-1);
		
		
		if (indx1 < 0)
			indx1 = indx1 * -1;
		
	//	proxy.setSecurityMeasures(SecurityConstants.secTypes.get(indx));
		
	//	proxy.setSecurityMeasures(SecurityConstants.allCountermeasures);
		
	//	List<String> lst = getUnionSecurityMeasures(simConstants.secUnionLimitProxy);
	//	proxy.setSecurityMeasures(lst);
		
		proxy.setSecurityMeasures(SecurityConstants.allCountermeasures);
		
		//proxy.setSecurityMeasures(SecurityConstants.secTypes.get(0));
		
	//	proxy.setSecurityMeasures(SecurityConstants.list1);
	   // proxy.setSecurityMeasures(SecurityConstants.secType3);
		
		//.setSecurityMeasures(SecurityConstants.secType4);
		
		fogDevices.add(proxy);
		deviceById.put(proxy.getId(), proxy);
		
		
		for(int i=0;i<numOfGateways;i++){
			addGw(i+"", userId, appId, proxy.getId()); 
		}
		
	}

	private static void addGw(String gwPartialName, int userId, String appId, int parentId){
		MyFogDevice gw = createFogDevice("g-"+gwPartialName, 4800, 4000, 80000, 80000, 2, 0.0, 107.339, 83.4333);
		fogDevices.add(gw);
		deviceById.put(gw.getId(), gw);
		gw.setParentId(parentId);
		gw.setUplinkLatency(5); 
		
		List<String> lst = new ArrayList<>();
		lst.addAll(SecurityConstants.list2);
		lst.addAll(SecurityConstants.list3);
		
		int listSize = SecurityConstants.secTypes.size();
		int indx = getvalue(0,listSize-1);
		
		
		/*
		 * if (indx < 0) indx = indx * -1;
		 * gw.setSecurityMeasures(SecurityConstants.secTypes.get(indx));
		 */
		
   	List<String> lst1 = getUnionSecurityMeasures(simConstants.secUnionLimitGW);
	gw.setSecurityMeasures(lst1);
		
//	   gw.setSecurityMeasures(SecurityConstants.allCountermeasures);
		
		
		//gw.setSecurityMeasures(SecurityConstants.secType1);
	//gw.setSecurityMeasures(SecurityConstants.list1);
		//gw.setSecurityMeasures(lst);
		//for(int i=numOfEndDevPerGateway;i>0;i--)
		for(int i=0;i<numOfEndDevPerGateway;i++)
			{
			String endPartialName = gwPartialName+"-"+i;
			MyFogDevice end  = addEnd(endPartialName, userId, appId, gw.getId()); 
			end.setUplinkLatency(1); 
			
			end.setSecurityMeasures(SecurityConstants.allCountermeasures);
			
			fogDevices.add(end);
			deviceById.put(end.getId(), end);
		}
		
	}
	
	private static MyFogDevice addEnd(String endPartialName, int userId, String appId, int parentId){
		MyFogDevice end = createFogDevice("e-"+endPartialName, 3200, 1000, 10000, 270, 3, 0, 87.53, 82.44);
		end.setParentId(parentId);
		idOfEndDevices.add(end.getId());
//	MySensor sensor = new MySensor("s-"+endPartialName, "IoTSensor", userId, appId, new DeterministicDistribution(sensingInterval)); // inter-transmission time of EEG sensor follows a deterministic distribution
		
	MySensor sensor = new MySensor("s-"+endPartialName, "IoTSensor", userId, appId, new UniformDistribution(minInterval,maxInterval)); // inter-transmission time of EEG sensor follows a deterministic distribution
        sensor.setPosConstant(posCons);
        sensor.setNegConstant(negCons);
		sensors.add(sensor);
		MyActuator actuator = new MyActuator("a-"+endPartialName, userId, appId, "IoTActuator");
		actuators.add(actuator);
		sensor.setGatewayDeviceId(end.getId());
		sensor.setLatency(6.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms
		actuator.setGatewayDeviceId(end.getId());
		actuator.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms
		
		System.out.println(end.getName() + "   Sensor   " + sensor.getName() + "   Actuator   " + actuator.getName());
		
		return end;
	}
	
	private static MyFogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); 
		int hostId = FogUtils.generateEntityId();
		long storage = 1000000;
		int bw = 10000;

		PowerModel pm;
		if (nodeName.equals("cloud"))
		{
			pm = new PowerModelSpecPowerHuaweiXH310XeonX3470();
		}else if (nodeName.startsWith("p"))
		{
			pm = new PowerModelSpecPowerIbmX3550XeonX5670();
		}
		else if (nodeName.startsWith("g"))
		{
			pm = new PowerModelSpecPowerIbmX3550XeonX5675();
		}
		else pm = new PowerModelSpecPowerIbmX3250XeonX3480();
		
		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
				//pm
			);
		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);
		String arch = "x86"; 
		String os = "Linux"; 
		String vmm = "Xen";
		double time_zone = 10.0; 
		double cost = 3.0; 
		double costPerMem = 0.05; 
		double costPerStorage = 0.001; 
		double costPerBw = 0.0; 
		LinkedList<Storage> storageList = new LinkedList<Storage>(); 
		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		MyFogDevice fogdevice = null;
		try {
			fogdevice = new MyFogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 1, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		fogdevice.setMips((int) mips);
		return fogdevice;
	}

	@SuppressWarnings({"serial" })
	private static MyApplication createApplication(String appId, int userId, double wt1,double wt2){
		
		MyApplication application = MyApplication.createApplication(appId, userId,wt1,wt2); 
		application.addAppModule("clientModule",10, 1000, 1000, 100); 
		application.addAppModule("mainModule", 50, 1500, 10000, 800); 
		application.addAppModule("storageModule", 10, 50, 12000, 100); 
		
		application.addAppEdge("IoTSensor", "clientModule", 100, 200, "IoTSensor", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("clientModule", "mainModule", 6000, 600  , "RawData", Tuple.UP, AppEdge.MODULE); 
		application.addAppEdge("mainModule", "storageModule", 1000, 300, "StoreData", Tuple.UP, AppEdge.MODULE); 
		application.addAppEdge("mainModule", "clientModule", 100, 50, "ResultData", Tuple.DOWN, AppEdge.MODULE); 
		application.addAppEdge("clientModule", "IoTActuator", 100, 50, "Response", Tuple.DOWN, AppEdge.ACTUATOR);  
		
		application.addTupleMapping("clientModule", "IoTSensor", "RawData", new FractionalSelectivity(1.0)); 
		application.addTupleMapping("mainModule", "RawData", "ResultData", new FractionalSelectivity(1.0)); 
		application.addTupleMapping("mainModule", "RawData", "StoreData", new FractionalSelectivity(1.0)); 
		application.addTupleMapping("clientModule", "ResultData", "Response", new FractionalSelectivity(1.0)); 
	
		Random rnd = new Random();
		
		
		for(int id:idOfEndDevices)
		{
			Map<String,Double> moduleDeadline = new HashMap<String,Double>();
			
			Map<String,Integer> modulePrivacy = new HashMap<String,Integer>();
			
			Map<String,Integer> modulePrivacyCount = new HashMap<String,Integer>();
			
			
			
			
			
			Map<String,Double>moduleSensingInfo = new HashMap<String,Double>();
			
			Map<String,Double>moduleInputDataInfo = new HashMap<String,Double>();
			
			
		//	moduleDeadline.put("mainModule", getvalue(3.00, 5.00));
			Map<String,Integer>moduleAddMips = new HashMap<String,Integer>();
			
			Map<String,List<String>> essModuleSecReq = new HashMap<String,List<String>>();
			Map<String,List<String>> nonEssModuleSecReq = new HashMap<String,List<String>>();
			
			moduleAddMips.put("mainModule", getvalue(0, 500));
			//moduleAddMips.put("mainModule", 100);
			
			
			int indx = rnd.nextInt(SecurityConstants.secSets.size());
			int indx1 = rnd.nextInt(SecurityConstants.secSets.size());
			
			if (indx == indx1)
			{
				if ((indx-1 ) > 0)
					indx1 = indx1-1;
				else if ((indx+1) < SecurityConstants.secSets.size())
					indx1 = indx1+1;
			}
			
		//	essModuleSecReq.put("mainModule", SecurityConstants.list3);
		//	nonEssModuleSecReq.put("mainModule", SecurityConstants.list4);
			
			essModuleSecReq.put("mainModule", SecurityConstants.secSets.get(indx));
			nonEssModuleSecReq.put("mainModule", SecurityConstants.secSets.get(indx1));
			
			int count = essModuleSecReq.get("mainModule").size() + nonEssModuleSecReq.get("mainModule").size();
			
			modulePrivacyCount.put("mainModule", count);
			
			System.out.println(id + " Security Count " + count);
			
			Map<String,Integer> moduleCriticality = new HashMap<String,Integer>();
          //  int crit = getvalue(0,3);
			
			
			
            
			/*
			 * int randomChildCrit = -1; //double childCritSelector = getvalue(0, 100);
			 * double childCritPercentage = 0; for (int j=0; j<critUsagePercentage.length;
			 * j++) { childCritPercentage += critUsagePercentage[j]; if(childCritSelector <=
			 * childCritPercentage){ randomChildCrit = j; break; } } if(randomChildCrit ==
			 * -1){ System.out.println("Impossible is occured! no random task type!");
			 * continue; }
			 */
			
			double childCritSelector = rnd.nextDouble();
			int randomChildCrit = -1;
			if (childCritSelector < critPercent)
				randomChildCrit =2;
			else
				randomChildCrit =0;
			
			//System.out.println("randomChildCrit:  "+randomChildCrit);
			
			MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(id);
			//dv.setCriticalityLevel(randomChildCrit);
			
			if (randomChildCrit == 0) {
				dv.setCriticalityLevel(0);
				dv.setCurrentCriticalityLevel(0);
			}
			else
			{
				dv.setCriticalityLevel(2);
				dv.setCurrentCriticalityLevel(2);
			}
			
			//System.out.println("randomChildCrit:  "+randomChildCrit +" Device:   "+ dv.getCriticalityLevel());
			
		//	System.out.println( dv.getName() + "  Criticality:   " + dv.getCurrentCriticalityLevel());
			
			
            moduleCriticality.put("mainModule", randomChildCrit);
			
			if (randomChildCrit ==2)
			{
				moduleDeadline.put("mainModule", getvalue(7.00, 9.00));
				moduleSensingInfo.put("mainModule", getvalue(1.625,3.0));
				moduleInputDataInfo.put("mainModule", getvalue(0.5,1.10));
				
			}
			
			else
			moduleDeadline.put("mainModule", getvalue(3.00, 5.00));
			moduleSensingInfo.put("mainModule", getvalue(0.25,1.80));
			moduleInputDataInfo.put("mainModule", getvalue(0.25,0.65));
			
			
			
			
			modulePrivacy.put("mainModule", getvalue(0,3));
			
			
			sensingFreqInfo.put(id,moduleSensingInfo );
			inputDataInfo.put(id,moduleInputDataInfo );
			
			
			criticalityInfo.put(id, moduleCriticality);
			
			
			privacyInfo.put(id, modulePrivacy);
			privacyCountInfo.put(id, modulePrivacyCount);
			deadlineInfo.put(id, moduleDeadline);
			additionalMipsInfo.put(id,moduleAddMips);
			moduleEssSecReqInfo.put(id, essModuleSecReq);
			moduleNonEssSecReqInfo.put(id, nonEssModuleSecReq);
			
			dv.setNonEssSecReq(moduleNonEssSecReqInfo.get(id).get("mainModule"));
			dv.setEssSecReq(moduleEssSecReqInfo.get(id).get("mainModule"));
			
		}
		
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("IoTSensor");add("clientModule");add("mainModule");add("clientModule");add("IoTActuator");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		application.setDeadlineInfo(deadlineInfo);
		application.setInputDataInfo(inputDataInfo);
		application.setSensingInfo(sensingFreqInfo);
		application.setInputPrivacyInfo(privacyInfo);
		application.setInputPrivacyCountInfo(privacyCountInfo);
		
		
		
		application.setAdditionalMipsInfo(additionalMipsInfo);
		application.setModuleEssSecReqInfo(moduleEssSecReqInfo);
		application.setModuleNonEssSecReqInfo(moduleNonEssSecReqInfo);
		
		return application;
	}
	
	
	
	@SuppressWarnings({"serial" })
	private static MyApplication createApplication(String appId, int userId){
		
		MyApplication application = MyApplication.createApplication(appId, userId); 
		application.addAppModule("clientModule",10, 1000, 1000, 100); 
		application.addAppModule("mainModule", 50, 1500, 10000, 800); 
		application.addAppModule("storageModule", 10, 50, 12000, 100); 
		
		application.addAppEdge("IoTSensor", "clientModule", 100, 200, "IoTSensor", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("clientModule", "mainModule", 6000, 600  , "RawData", Tuple.UP, AppEdge.MODULE); 
		application.addAppEdge("mainModule", "storageModule", 1000, 300, "StoreData", Tuple.UP, AppEdge.MODULE); 
		application.addAppEdge("mainModule", "clientModule", 100, 50, "ResultData", Tuple.DOWN, AppEdge.MODULE); 
		application.addAppEdge("clientModule", "IoTActuator", 100, 50, "Response", Tuple.DOWN, AppEdge.ACTUATOR);  
		
		application.addTupleMapping("clientModule", "IoTSensor", "RawData", new FractionalSelectivity(1.0)); 
		application.addTupleMapping("mainModule", "RawData", "ResultData", new FractionalSelectivity(1.0)); 
		application.addTupleMapping("mainModule", "RawData", "StoreData", new FractionalSelectivity(1.0)); 
		application.addTupleMapping("clientModule", "ResultData", "Response", new FractionalSelectivity(1.0)); 
	
		Random rnd = new Random();
		
		
		for(int id:idOfEndDevices)
		{
			Map<String,Double>moduleDeadline = new HashMap<String,Double>();
            Map<String,Double>moduleSensingInfo = new HashMap<String,Double>();
			
			Map<String,Double>moduleInputDataInfo = new HashMap<String,Double>();
		//	moduleDeadline.put("mainModule", getvalue(3.00, 5.00));
			Map<String,Integer>moduleAddMips = new HashMap<String,Integer>();
			
			Map<String,List<String>> essModuleSecReq = new HashMap<String,List<String>>();
			Map<String,List<String>> nonEssModuleSecReq = new HashMap<String,List<String>>();
			
			Map<String,Integer> modulePrivacyCount = new HashMap<String,Integer>();	
			
			moduleAddMips.put("mainModule", getvalue(0, 500));
			//moduleAddMips.put("mainModule", 100);
			Map<String,Integer> modulePrivacy = new HashMap<String,Integer>();
			
			int indx = rnd.nextInt(SecurityConstants.secSets.size());
			int indx1 = rnd.nextInt(SecurityConstants.secSets.size());
			
			if (indx == indx1)
			{
				if ((indx-1 ) > 0)
					indx1 = indx1-1;
				else if ((indx+1) < SecurityConstants.secSets.size())
					indx1 = indx1+1;
			}
			
		//	essModuleSecReq.put("mainModule", SecurityConstants.list3);
		//	nonEssModuleSecReq.put("mainModule", SecurityConstants.list4);
			
			essModuleSecReq.put("mainModule", SecurityConstants.secSets.get(indx));
			nonEssModuleSecReq.put("mainModule", SecurityConstants.secSets.get(indx1));
			
			Map<String,Integer>moduleCriticality = new HashMap<String,Integer>();
          //  int crit = getvalue(0,3);
			
			
           int count = essModuleSecReq.get("mainModule").size() + nonEssModuleSecReq.get("mainModule").size();
			
			modulePrivacyCount.put("mainModule", count);
			
			System.out.println(id + " Security Count " + count);
			
            
			/*
			 * int randomChildCrit = -1; //double childCritSelector = getvalue(0, 100);
			 * double childCritPercentage = 0; for (int j=0; j<critUsagePercentage.length;
			 * j++) { childCritPercentage += critUsagePercentage[j]; if(childCritSelector <=
			 * childCritPercentage){ randomChildCrit = j; break; } } if(randomChildCrit ==
			 * -1){ System.out.println("Impossible is occured! no random task type!");
			 * continue; }
			 */
			
			double childCritSelector = rnd.nextDouble();
			int randomChildCrit = -1;
			if (childCritSelector < critPercent)
				randomChildCrit =2;
			else
				randomChildCrit =0;
			
			//System.out.println("randomChildCrit:  "+randomChildCrit);
			
			MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(id);
			//dv.setCriticalityLevel(randomChildCrit);
			
			if (randomChildCrit == 0) {
				dv.setCriticalityLevel(0);
				dv.setCurrentCriticalityLevel(0);
			}
			else
			{
				dv.setCriticalityLevel(2);
				dv.setCurrentCriticalityLevel(2);
			}
			
			//System.out.println("randomChildCrit:  "+randomChildCrit +" Device:   "+ dv.getCriticalityLevel());
			
		//	System.out.println( dv.getName() + "  Criticality:   " + dv.getCurrentCriticalityLevel());
			
			
            moduleCriticality.put("mainModule", randomChildCrit);
			
            if (randomChildCrit ==2)
			{
				moduleDeadline.put("mainModule", getvalue(7.00, 9.00));
				moduleSensingInfo.put("mainModule", getvalue(1.625,3.0));
				moduleInputDataInfo.put("mainModule", getvalue(0.5,1.10));
				
			}
			
			else
			moduleDeadline.put("mainModule", getvalue(3.00, 5.00));
			moduleSensingInfo.put("mainModule", getvalue(0.25,1.80));
			moduleInputDataInfo.put("mainModule", getvalue(0.25,0.65));
			modulePrivacy.put("mainModule", getvalue(0,3));
			
			sensingFreqInfo.put(id,moduleSensingInfo );
			inputDataInfo.put(id,moduleInputDataInfo );
			privacyInfo.put(id, modulePrivacy);
			privacyCountInfo.put(id, modulePrivacyCount);
			
			criticalityInfo.put(id, moduleCriticality);
			deadlineInfo.put(id, moduleDeadline);
			additionalMipsInfo.put(id,moduleAddMips);
			moduleEssSecReqInfo.put(id, essModuleSecReq);
			moduleNonEssSecReqInfo.put(id, nonEssModuleSecReq);
			
			dv.setNonEssSecReq(moduleNonEssSecReqInfo.get(id).get("mainModule"));
			dv.setEssSecReq(moduleEssSecReqInfo.get(id).get("mainModule"));
			
		}
		
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("IoTSensor");add("clientModule");add("mainModule");add("clientModule");add("IoTActuator");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		application.setDeadlineInfo(deadlineInfo);
		application.setAdditionalMipsInfo(additionalMipsInfo);
		application.setModuleEssSecReqInfo(moduleEssSecReqInfo);
		application.setModuleNonEssSecReqInfo(moduleNonEssSecReqInfo);
		application.setInputDataInfo(inputDataInfo);
		application.setSensingInfo(sensingFreqInfo);
		application.setInputPrivacyInfo(privacyInfo);
		application.setInputPrivacyCountInfo(privacyCountInfo);
		
		return application;
	}
}