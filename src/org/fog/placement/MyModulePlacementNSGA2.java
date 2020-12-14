package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.MyApplication;
import org.fog.entities.MyActuator;
import org.fog.entities.MyFogDevice;
import org.fog.entities.MySensor;
import org.fog.security.SecurityConstants;
import org.fog.security.SecurityParametersTypes;
import org.fog.security.SecurityTaxonomy;
import org.fog.utils.TimeKeeper;
import org.knapsack.Item;
import org.knapsack.Knapsack;
//import org.knapsack.Solution;
import org.moeaframework.Executor;
import org.moeaframework.Instrumenter;
import org.moeaframework.analysis.collector.Accumulator;
import org.moeaframework.analysis.plot.Plot;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.PopulationIO;
import org.moeaframework.util.Vector;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class MyModulePlacementNSGA2 extends MyPlacement{
	
	protected ModuleMapping moduleMapping;
	protected List<MySensor> sensors;
	protected List<MyActuator> actuators;
	protected String moduleToPlace;
	protected Map<Integer, Integer> deviceMipsInfo;
	
	
	static boolean Crit = true;
	
	
	public MyModulePlacementNSGA2(List<MyFogDevice> fogDevices, List<MySensor> sensors, List<MyActuator> actuators, 
			MyApplication application, ModuleMapping moduleMapping, String moduleToPlace){
		this.setMyFogDevices(fogDevices);
		this.setMyApplication(application);
		this.setModuleMapping(moduleMapping);
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		setMySensors(sensors);
		setMyActuators(actuators);
		this.moduleToPlace = moduleToPlace;
		this.deviceMipsInfo = new HashMap<Integer, Integer>();
		mapModules();
	}

	@Override
	protected void mapModules() {
		
		
		//this.showDetailedSecurityScoresofFogDevices();
		
		for(String deviceName : getModuleMapping().getModuleMapping().keySet()){
			for(String moduleName : getModuleMapping().getModuleMapping().get(deviceName)){
				int deviceId = CloudSim.getEntityId(deviceName);
				AppModule appModule = getMyApplication().getModuleByName(moduleName); 
				if(!getDeviceToModuleMap().containsKey(deviceId))
				{
					List<AppModule>placedModules = new ArrayList<AppModule>();
					placedModules.add(appModule);
					getDeviceToModuleMap().put(deviceId, placedModules);
					
				}
				else
				{
					List<AppModule>placedModules = getDeviceToModuleMap().get(deviceId);
					placedModules.add(appModule);
					getDeviceToModuleMap().put(deviceId, placedModules);
				}
			}
		}
		
		if (!Crit)
		{
			
			
			for(MyFogDevice device:getMyFogDevices())
			{
				int deviceParent = -1;
				List<Integer>children = new ArrayList<Integer>();
				
				if(device.getLevel()==2)
				{
					
					List<Item> itms = new ArrayList<Item>();
					
					if(!deviceMipsInfo.containsKey(device.getId()))
						deviceMipsInfo.put(device.getId(), 0);
					deviceParent = device.getParentId();
					
					if (!deviceMipsInfo.containsKey(deviceParent))
						deviceMipsInfo.put(deviceParent, 0);
					
					for(MyFogDevice deviceChild:getMyFogDevices())
					{
						if(deviceChild.getParentId()==device.getId())
						{
							children.add(deviceChild.getId());
						}
					}
					
					
					Map<Integer, Double>highCritDeadline = new HashMap<Integer, Double>();
					Map<Integer, Double>medCritDeadline = new HashMap<Integer, Double>();
					Map<Integer, Double>lowCritDeadline = new HashMap<Integer, Double>();
					
					
					Map<Integer, Double>childDeadline = new HashMap<Integer, Double>();
					
					for(int childId:children)
					{
						childDeadline.put(childId,getMyApplication().getDeadlineInfo().get(childId).get(moduleToPlace));
						
						MyFogDevice dv = (MyFogDevice)CloudSim.getEntity(childId);
						
						int crit = dv.getCriticalityLevel();
						
					//	System.out.println(crit);
						
						
						if (crit == 2)
						{
							highCritDeadline.put(childId, childDeadline.get(childId));
						}
						else if (crit==1)
						{
							medCritDeadline.put(childId, childDeadline.get(childId));
						}
						else
						{
							lowCritDeadline.put(childId, childDeadline.get(childId));
						}
						
					}
					
				
					List<Integer> keys = new ArrayList<Integer>(childDeadline.keySet());
					
					List<Integer> hkeys = new ArrayList<Integer>(highCritDeadline.keySet());
					List<Integer> mkeys = new ArrayList<Integer>(medCritDeadline.keySet());
					List<Integer> lkeys = new ArrayList<Integer>(lowCritDeadline.keySet());
					
					
				
					//System.out.println("HKeys --- " + hkeys);
					//System.out.println("MKeys --- " + mkeys);
					//System.out.println("LKeys --- " + lkeys);
					
					
					
					  for(int i = 0; i<keys.size()-1; i++) 
					  { 
						  for(int j=0;j<keys.size()-i-1;j++)
						  {
					     if(childDeadline.get(keys.get(j))>childDeadline.get(keys.get(j+1))) { 
						  int tempJ = keys.get(j); 
						  int tempJn = keys.get(j+1); 
						  keys.set(j, tempJn);
					      keys.set(j+1, tempJ); 
					      }
					  } 
					  }
					 
					
					for(int i = 0; i<hkeys.size()-1; i++)
					{
						for(int j=0;j<hkeys.size()-i-1;j++)
						{
							if(highCritDeadline.get(hkeys.get(j))>highCritDeadline.get(hkeys.get(j+1)))
							{
								int tempJ = hkeys.get(j);
								int tempJn = hkeys.get(j+1);
								hkeys.set(j, tempJn);
								hkeys.set(j+1, tempJ);
							}
						}
					}
					
					for(int i = 0; i<mkeys.size()-1; i++)
					{
						for(int j=0;j<mkeys.size()-i-1;j++)
						{
							if(medCritDeadline.get(mkeys.get(j))>medCritDeadline.get(mkeys.get(j+1)))
							{
								int tempJ = mkeys.get(j);
								int tempJn = mkeys.get(j+1);
								mkeys.set(j, tempJn);
								mkeys.set(j+1, tempJ);
							}
						}
					}
					
					for(int i = 0; i<lkeys.size()-1; i++)
					{
						for(int j=0;j<lkeys.size()-i-1;j++)
						{
							if(lowCritDeadline.get(lkeys.get(j))>lowCritDeadline.get(lkeys.get(j+1)))
							{
								int tempJ = lkeys.get(j);
								int tempJn = lkeys.get(j+1);
								lkeys.set(j, tempJn);
								lkeys.set(j+1, tempJ);
							}
						}
					}
						
					int baseMipsOfPlacingModule = (int)getMyApplication().getModuleByName(moduleToPlace).getMips();
					
					for(int key:keys)
					{
						
						  int currentMips = deviceMipsInfo.get(device.getId()); 
						  AppModule  appModule = getMyApplication().getModuleByName(moduleToPlace); 
						  int additionalMips = getMyApplication().getAdditionalMipsInfo().get(key).get(moduleToPlace);
						  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
						  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
						  
						  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
						  
						//  System.out.println(device.getName() + " Suitability To  " + CloudSim.getEntityName(key) +  " is " + suit/device.getMaxSuitabilityFactor() );
							  
						  
						  // System.out.println(device.getName() + "    "+ device.getMips());
						  
						  String name = CloudSim.getEntityName(key);
						  
						  if(currentMips+baseMipsOfPlacingModule+additionalMips<device.getMips()) {
						  currentMips = currentMips+baseMipsOfPlacingModule+additionalMips;
						  deviceMipsInfo.put(device.getId(), currentMips);
						  if(!getDeviceToModuleMap().containsKey(device.getId())) {
						  List<AppModule>placedModules = new ArrayList<AppModule>();
						  placedModules.add(appModule); 
						  getDeviceToModuleMap().put(device.getId(),placedModules); 
						  device.getAssociatedEndDevices().add(name);
						  TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(name, device.getName());
						  
						  
						  } 
						  else 
						  {
							  List<AppModule>placedModules = getDeviceToModuleMap().get(device.getId()); 
						  placedModules.add(appModule);
						  getDeviceToModuleMap().put(device.getId(), placedModules);
						  device.getAssociatedEndDevices().add(name);
						  TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(name,
						  device.getName()); } } 
						  
						  
						  else { 
							  
						  MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(deviceParent);
						  
						  int currentMips_1 = deviceMipsInfo.get(dv.getId()); 
						  int additionalMips_1 =getMyApplication().getAdditionalMipsInfo().get(key).get(moduleToPlace);
						  
						  if(currentMips_1+baseMipsOfPlacingModule+additionalMips_1<dv.getMips()) {
						  
						  currentMips_1 = currentMips_1+baseMipsOfPlacingModule+additionalMips;
						  deviceMipsInfo.put(dv.getId(), currentMips_1);
						  
						  if(!getDeviceToModuleMap().containsKey(dv.getId())) {
						  List<AppModule>placedModules = new ArrayList<AppModule>();
						  placedModules.add(appModule); getDeviceToModuleMap().put(dv.getId(),
						  placedModules); dv.getAssociatedEndDevices().add(name);
						  TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(name, dv.getName());
						  
						  
						  } else { List<AppModule>placedModules =
						  getDeviceToModuleMap().get(dv.getId()); placedModules.add(appModule);
						  getDeviceToModuleMap().put(dv.getId(), placedModules);
						  dv.getAssociatedEndDevices().add(name);
						  TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(name, dv.getName()); }
						  
						  } else { 
						  int id = dv.getParentId(); MyFogDevice dv_1 =  (MyFogDevice)CloudSim.getEntity(id);
						  
						  List<AppModule>placedModules = getDeviceToModuleMap().get(id);
						  placedModules.add(appModule); getDeviceToModuleMap().put(id, placedModules);
						  
						  dv_1.getAssociatedEndDevices().add(name);
						  TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(name, dv_1.getName());
						  
						  }
						  
						  }
						 }			
		}
		}
		}
		
		
		else
		{
			
			
			for(MyFogDevice device:getMyFogDevices())
			{
				int deviceParent = -1;
			//	List<Integer>children = new ArrayList<Integer>();
			//	System.out.println(device.getLevel());
				
				if(device.getLevel()==2)
				{
					
					
					device.setChildCritMap(new LinkedHashMap<Integer,Double>());
					device.setChildSecMap(new LinkedHashMap<Integer,Double>());
					device.setChildMipsMap(new LinkedHashMap<Integer,Double>());
					
					Map<Integer,Double> childCritMap = new LinkedHashMap<Integer,Double>();
					Map<Integer,Double> childSecMap = new LinkedHashMap<Integer,Double>();
				    Map<Integer,Double> childMipsMap = new LinkedHashMap<Integer,Double>();
				    
					List<Integer>children = new ArrayList<Integer>();
					
					for(MyFogDevice deviceChild:getMyFogDevices())
					{
						if(deviceChild.getParentId()==device.getId())
						{
							
							if(!deviceMipsInfo.containsKey(device.getId()))
								deviceMipsInfo.put(device.getId(), 0);
							deviceParent = device.getParentId();
							
							if (!deviceMipsInfo.containsKey(deviceParent))
								deviceMipsInfo.put(deviceParent, 0);
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(deviceChild.getId());
							children.add(deviceChild.getId());
							double cont =  getMyApplication().getDeadlineInfo().get(deviceChild.getId()).get(moduleToPlace);
							int val = dv.getCriticalityLevel();
							if (val==0)
								val = val+1;
								//val=val;
							else val = val+4;
							
							int noHops =  dv.getLevel() - device.getLevel() ;
							
							double nVal = (double) val/noHops;
							
							nVal = ((nVal) *(1/cont));
							
							double critValue = (double) nVal;
							
							
							int key = deviceChild.getId();
							
							  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
							  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
							  
							  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
							  
							 // double secValue = suit/maxSecVal;
							  double secValue = suit/device.getMaxSuitabilityFactor();
							  
							  MyFogDevice dvKey = (MyFogDevice) CloudSim.getEntity(key);
							  
							  if (critValue > dvKey.getMaxCritVal())
								  dvKey.setMaxCritVal(critValue*100);
							  
							  if (secValue * 100 > dvKey.getMaxSecVal())
								  dvKey.setMaxSecVal(secValue*100);
							  
							  if (secValue * 100 < dvKey.getMinSecVal())
								  dvKey.setMinSecVal(secValue*100);
							  
							  if (critValue < dvKey.getMinCritVal())
								  dvKey.setMinCritVal(critValue*100);
							  
							  System.out.println(dvKey.getName() + "  Key:  " + key + " Crit on:  " + device.getName() + "   is:   " + critValue);
							  
							  
							  double mips = getMyApplication().getModuleByName(moduleToPlace).getMips();
								mips = mips + getMyApplication().getAdditionalMipsInfo().get(deviceChild.getId()).get(moduleToPlace);
							//	Item itm = new Item(dv.getName(),(int) (val *( 1-1/cont)*10000),(int)mips);
							//	Item itm = new Item(dv.getName(),(int) ((nVal) *(1/cont)*10000),(int)mips);
							//	Item itm = new Item(dv.getName(),itemValue,(int)mips);
								
								 childMipsMap.put(key, mips);
								 childCritMap.put(key, critValue*100);
								 childSecMap.put(key, secValue*100);
							  
							  
							     
						}
					}
					
					
				    device.setChildEndDeviceIds(children);
					device.setChildCritMap(childCritMap);
					device.setChildMipsMap(childMipsMap);
					device.setChildSecMap(childSecMap);
						
					//System.out.println(device.getChildEndDeviceIds());	
					
				}
				
			}
			
			
				
			for(MyFogDevice device:getMyFogDevices())
			{
				
				if(device.getLevel()==1)
				{
					
					device.setChildCritMap(new LinkedHashMap<Integer,Double>());
					device.setChildSecMap(new LinkedHashMap<Integer,Double>());
					device.setChildMipsMap(new LinkedHashMap<Integer,Double>());
					
					for(MyFogDevice deviceChild:getMyFogDevices())
					{
						if(deviceChild.getParentId()==device.getId())
						{
							
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(deviceChild.getId());
							//children.add(deviceChild.getId());
							device.getChildEndDeviceIds().addAll(dv.getChildEndDeviceIds());
							device.getChildMipsMap().putAll(dv.getChildMipsMap());
							device.getChildCripMap().putAll(dv.getChildCripMap());
						}
					}
					
					
						for (int key: device.getChildEndDeviceIds())
					{
					
					 MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
					 
					 int nHops = dv.getLevel() - device.getLevel();
					 
					 double critValue=0;
					 
					 if (dv.getCriticalityLevel()==2)
					 
					 critValue = (double) (device.getChildCripMap().get(key)/nHops);
					 
					 else
						 critValue = (double) (device.getChildCripMap().get(key));
					 
					 
					 device.getChildCripMap().put(key, critValue);
						
					  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
					  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
					  
					  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
					  
					  double secValue = suit/device.getMaxSuitabilityFactor();
					  
					  device.getChildSecMap().put(key, secValue*100);
					  
					//  System.out.println(device.getName() + " Suitability To  " + CloudSim.getEntityName(key) +  " is " + suit/device.getMaxSuitabilityFactor() );
					
							  
							  MyFogDevice dvKey = (MyFogDevice) CloudSim.getEntity(key);
							  
							  if (critValue > dvKey.getMaxCritVal())
								  dvKey.setMaxCritVal(critValue );
							  
							  if (secValue*100 > dvKey.getMaxSecVal())
								  dvKey.setMaxSecVal(secValue *100 );
							  
							  if (secValue *100 < dvKey.getMinSecVal())
								  dvKey.setMinSecVal(secValue*100);
							  
							  if (critValue < dvKey.getMinCritVal())
								  dvKey.setMinCritVal(critValue);
							  
							  System.out.println(dvKey.getName() + "  Key:  " + key + " Crit on:  " + device.getName() + "   is:   " + critValue);

						}
					
				
				}
			}
				
			for(MyFogDevice device:getMyFogDevices())
			{
				if(device.getLevel()==0)
				{
					
					device.setChildCritMap(new LinkedHashMap<Integer,Double>());
					device.setChildSecMap(new LinkedHashMap<Integer,Double>());
					device.setChildMipsMap(new LinkedHashMap<Integer,Double>());
					
					
					List<Integer>children = new ArrayList<Integer>();
					
					for(MyFogDevice deviceChild:getMyFogDevices())
					{
						if(deviceChild.getParentId()==device.getId())
						{
							
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(deviceChild.getId());
							children.add(deviceChild.getId());
							device.getChildEndDeviceIds().addAll(dv.getChildEndDeviceIds());
							device.getChildMipsMap().putAll(dv.getChildMipsMap());
							device.getChildCripMap().putAll(dv.getChildCripMap());
						}
					}
					
					
					for (int key: device.getChildEndDeviceIds())
					{
					
					  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
					  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
					  
					  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
					  
					  MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
					  MyFogDevice dvParent = (MyFogDevice) CloudSim.getEntity(dv.getParentId());
					  
					  int nHops = dv.getLevel() - device.getLevel();
					  double critValue =0;
					  
					  if (dv.getCriticalityLevel() ==2)
					   critValue = (double)(dvParent.getChildCripMap().get(key)/nHops);
					  else
					   critValue = (double)(dvParent.getChildCripMap().get(key));
						  
					  
						
						 
						  device.getChildCripMap().put(key, critValue);
						
						  double secValue = suit/device.getMaxSuitabilityFactor();
						  
						  device.getChildSecMap().put(key, secValue*100);
						  
				//	  System.out.println(device.getName() + " Suitability To  " + CloudSim.getEntityName(key) +  " is " + suit/device.getMaxSuitabilityFactor() );
					    
							  MyFogDevice dvKey = (MyFogDevice) CloudSim.getEntity(key);
							  
							  if (critValue > dvKey.getMaxCritVal())
								  dvKey.setMaxCritVal(critValue);
							  
							  if (secValue * 100 > dvKey.getMaxSecVal())
								  dvKey.setMaxSecVal(secValue * 100);
							  
							  if (secValue *100 < dvKey.getMinSecVal())
								  dvKey.setMinSecVal(secValue * 100);
							  
							  if (critValue < dvKey.getMinCritVal())
								  dvKey.setMinCritVal(critValue);
							  System.out.println(dvKey.getName() + "  Key:  " + key + " Crit on:  " + device.getName() + "   is:   " + critValue);

						}
					}
				}
			
			
			
			for(MyFogDevice device:getMyFogDevices())
			{
				int deviceParent = -1;
				
				
				if(device.getLevel()==2)
				{
					
				/*	double maxCritVal = Double.MIN_VALUE;
					double maxSecVal = Double.MIN_VALUE;
					
					Map<Integer,Double> childCritMap = new LinkedHashMap<Integer,Double>();
					Map<Integer,Double> childSecMap = new LinkedHashMap<Integer,Double>();
				    Map<Integer,Double> childMipsMap = new LinkedHashMap<Integer,Double>();
					
					List<Item> itms = new ArrayList<Item>();
					List<Integer>children = new ArrayList<Integer>();
					
					if(!deviceMipsInfo.containsKey(device.getId()))
						deviceMipsInfo.put(device.getId(), 0);
					deviceParent = device.getParentId();
					
					if (!deviceMipsInfo.containsKey(deviceParent))
						deviceMipsInfo.put(deviceParent, 0);
					
					for(MyFogDevice deviceChild:getMyFogDevices())
					{
						if(deviceChild.getParentId()==device.getId())
						{	
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(deviceChild.getId());
							children.add(deviceChild.getId());
							
							double cont =  getMyApplication().getDeadlineInfo().get(deviceChild.getId()).get(moduleToPlace);
							int val = dv.getCriticalityLevel();
							if (val==0)
								val = val+1;
							else val = val+4;
							
							int noHops =  dv.getLevel() - device.getLevel() ;
							
							double nVal = (double) val/noHops;
							
							nVal = ((nVal) *(1/cont));
							
							if (nVal > maxCritVal)
								maxCritVal = nVal;
							
							int key = deviceChild.getId();
							
							  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
							  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
							  
							  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
							  
							  if (suit > maxSecVal)
								  maxSecVal = suit;
							
						}
								
					}
					
					
					List<Item> tabuItems = new ArrayList<>();
					
					
					
					for(MyFogDevice deviceChild:getMyFogDevices())
					{
						if(deviceChild.getParentId()==device.getId())
						{
							
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(deviceChild.getId());
							
							double cont =  getMyApplication().getDeadlineInfo().get(deviceChild.getId()).get(moduleToPlace);
							int val = dv.getCriticalityLevel();
							if (val==0)
								val = val+1;
								//val=val;
							else val = val+4;
							
							int noHops =  dv.getLevel() - device.getLevel() ;
							
							double nVal = (double) val/noHops;
							
							nVal = ((nVal) *(1/cont));
							
							double critValue = (double) nVal/maxCritVal;
							
							
							int key = deviceChild.getId();
							
							  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
							  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
							  
							  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
							  
							 // double secValue = suit/maxSecVal;
							  double secValue = suit/device.getMaxSuitabilityFactor();
							  
							  double secWeight = getMyApplication().getSecWeight();
							  double critWeight = getMyApplication().getCritWeight();
							  
							  double finalVal = secWeight * secValue + critWeight * critValue;
							  
							  childCritMap.put(key, critValue*100);
							  childSecMap.put(key, secValue*100);
							  
							  MyFogDevice dvKey = (MyFogDevice) CloudSim.getEntity(key);
							  
							  if (critValue > dvKey.getMaxCritVal())
								  dvKey.setMaxCritVal(critValue);
							  
							  if (secValue > dvKey.getMaxSecVal())
								  dvKey.setMaxSecVal(secValue);
							  
							  if (secValue < dvKey.getMinSecVal())
								  dvKey.setMinSecVal(secValue);
							  
							  if (critValue < dvKey.getMinCritVal())
								  dvKey.setMinCritVal(critValue);
							  
							  
							  int itemValue = (int) (finalVal * 100);
							  
							  
						//	System.out.println(dv.getName() + "  final value  " + itemValue);
							
							double mips = getMyApplication().getModuleByName(moduleToPlace).getMips();
							mips = mips + getMyApplication().getAdditionalMipsInfo().get(deviceChild.getId()).get(moduleToPlace);
						//	Item itm = new Item(dv.getName(),(int) (val *( 1-1/cont)*10000),(int)mips);
						//	Item itm = new Item(dv.getName(),(int) ((nVal) *(1/cont)*10000),(int)mips);
							Item itm = new Item(dv.getName(),itemValue,(int)mips);
							
							 childMipsMap.put(key, mips);
							//System.out.println(dv.getName() + "   " + cont + "   " + 1/cont   + "    " + (1-1/cont));
							
						//	System.out.println(dv.getName() + "  Crit Value  " + critValue * 100  +  "  Sec Value  " + secValue * 100 + " MIPS " + mips);
							//Item itm = new Item(dv.getName(),val,(int)mips);
							
							if (secValue > 0.5)
							{
							//	System.out.println(itm.getItemName() + " has been removed from knapsack of  " + device.getName() );
								
								tabuItems.add(itm);
								//itms.remove(itm);
							}
							
							
							itms.add(itm);
							//children.add(deviceChild.getId());
						 
							 // System.out.println(device.getName() + " Suitability To  " + CloudSim.getEntityName(key) +  " is " + suit/device.getMaxSuitabilityFactor() );
						}
					}
					
					device.setChildEndDeviceIds(children);
					device.setChildCritMap(childCritMap);
					device.setChildMipsMap(childMipsMap);
					device.setChildSecMap(childSecMap);
				//	device.setKnapsack(new Knapsack(itms,(int)device.getMips()));
					device.setKnapsack(new Knapsack(tabuItems,(int)device.getMips()));
				//	device.getKnapsack().display();
					
			
					
					
					System.out.println(device.getName() + " Child Ids  " + device.getChildEndDeviceIds());*/
					
					
					int nsacks =2;
					//int nitems = children.size();
					int nitems = device.getChildEndDeviceIds().size();
					
					List<Integer>children = new ArrayList<Integer>();
					children = device.getChildEndDeviceIds();
					
					Map<Integer,Double> childCritMap = new LinkedHashMap<Integer,Double>();
					Map<Integer,Double> childSecMap = new LinkedHashMap<Integer,Double>();
				    Map<Integer,Double> childMipsMap = new LinkedHashMap<Integer,Double>();
				    
				    childCritMap = device.getChildCripMap();
				    childSecMap = device.getChildSecMap();
				    childMipsMap = device.getChildMipsMap();
					
					double [][]profit = new double [nsacks][nitems];
					double [][]weight = new double [nsacks][nitems];
					int capacity = device.getMips();
					
					for (int i=0; i<nitems-1;i++)
					{
						int key = children.get(i);
						profit[0][i] = childCritMap.get(children.get(i));
						profit[1][i] = childSecMap.get(children.get(i));
						weight[0][i] = childMipsMap.get(children.get(i));
						weight[1][i] = childMipsMap.get(children.get(i));
					}
					Instant start = Instant.now();
					
					  Instrumenter instrumenter = new Instrumenter()
					  .withProblemClass(KnapsackP.class, profit,weight,nitems,capacity)
					  .withFrequency(1000)
					  .attachElapsedTimeCollector();
					 
					
					NondominatedPopulation result = new Executor()
							.withProblemClass(KnapsackP.class, profit,weight,nitems,capacity)
							.withAlgorithm("NSGAII")
							.withMaxEvaluations(10000)
							.distributeOnAllCores()
							.withInstrumenter(instrumenter)
							.run();
					 Accumulator accumulator = instrumenter.getLastAccumulator();
					 
					 System.out.println(accumulator.toString());
					 try {
						accumulator.saveCSV(new File("output.csv"));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					
					  for (int i=0; i<accumulator.size("NFE"); i++) {
					  System.out.format("%5d    %-8.4f  ", accumulator.get("NFE", i),
					  accumulator.get("Elapsed Time", i)); 
					  System.out.println();
					  }
					 
					 
					
					
					Instant finish = Instant.now();
					 
				    long timeElapsed = Duration.between(start, finish).toMillis();
					System.out.println("Elapsed time to solve MOEA for  " + device.getName() + "  is:   " + timeElapsed);
					//new Plot() .add("NSGAII", result).show();
					new Plot() .add(accumulator).showDialog();
					 
					double secWeight = getMyApplication().getSecWeight();
					double critWeight = getMyApplication().getCritWeight();
					
					double maxSolVal = Double.MIN_VALUE;
					Solution bestSol = null;
					
					Solution minDistSol = null;
					double minDist = Double.MAX_VALUE;
					
					double maxObj0 = Double.MIN_VALUE;
					double maxObj1 = Double.MIN_VALUE;
					
					
					double secMax=Double.MIN_VALUE;
					double secMin = Double.MAX_VALUE;
					double critMax = Double.MIN_VALUE;
					double critMin = Double.MAX_VALUE;
					
					
					for (int j = 0; j < result.size(); j++) {
						
						Solution solu = result.get(j);
						double [] objs = solu.getObjectives();
						
						objs = Vector.negate(objs);
						
						if (objs[0] > maxObj0)
							maxObj0 = objs[0];
						if (objs[1] > maxObj1)
							maxObj1 = objs[1];
							
					}
					
					System.out.println("Max objs0: " + maxObj0 + "   " + "Max Obj1:   " + maxObj1);
					
					for (int i = 0; i < result.size(); i++) {
						Solution sol = result.get(i);
						double[] objectives = sol.getObjectives();
								
						// negate objectives to return them to their maximized form
						objectives = Vector.negate(objectives);
						
						//double tmpVal = (double)(critWeight * objectives[0] + secWeight * objectives[1]);
						double tmpVal = (double)(critWeight * (objectives[0]) + secWeight * (objectives[1]));
						
						if (tmpVal > maxSolVal)
						{
							maxSolVal = tmpVal;
							bestSol = sol;
						}
						
						 double dist = Math.sqrt(Math.pow(objectives[0], 2) + Math.pow(objectives[1], 2));
	                       // System.out.println("Solution " + (i+1) + "   Dist: " + dist );
	                        
	                        
	                        if (dist < minDist)
	                        {
	                        	minDist = dist;
	                        	minDistSol = sol;
	                        }
	                        
						System.out.println("Solution " + (i+1) + ":");
						System.out.println("    Sack 1 Profit: " + objectives[0]);
						System.out.println("    Sack 2 Profit: " + objectives[1]);
						System.out.println("    Total Proft:  " + (double)(critWeight * objectives[0] + secWeight * objectives[1]));
						System.out.println("    Distance  : "   + dist);
						System.out.println("    Binary String: " + sol.getVariable(0));
						
						
						
						
                    /* boolean[] d = EncodingUtils.getBinary(sol.getVariable(0));
						
						for (int j=0; j<d.length;j++)
						{
							if (d[j])
							{
								int key = device.getChildEndDeviceIds().get(j);
								double secValue = device.getChildSecMap().get(key);
								double critValue = device.getChildCripMap().get(key);
							//	System.out.println(device.getChildEndDeviceIds().get(j) + "  " + d[j]  +"  Sec Value " + secValue + "  Crit Value " + critValue);
							   // device.getChildEndDeviceIds().remove(device.getChildEndDeviceIds().get(j));
                               // double aggValue = secWeight * secValue + critWeight * critValue;
								
								MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
								
								secMax = dv.getMaxSecVal();
								secMin = dv.getMinSecVal();
								
								critMax = dv.getMaxCritVal();
								critMin = dv.getMinCritVal();
								
								//System.out.println(dv.getName() + "     id:  "+device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue);
							
	                         //   double aggValue = secWeight * secValue/100 + critWeight * critValue/100;
								
								double aggValue = secWeight * (secValue-secMin)/(secMax-secMin) + critWeight * (critValue-critMin)/(critMax-critMin);
								
								
								//double aggValue = secWeight * (secValue/100) + critWeight * (critValue/100);
								
							//	MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
								
								if (aggValue > dv.getAggProfitValue())
								{
									dv.setAggProfitValue(aggValue);
									dv.setPrefFogDeviceName(device.getName());
								}
							
								System.out.println(dv.getName() + "    id:  " + device.getChildEndDeviceIds().get(j) + "  Sec Value " + secValue + "  Crit Value " + critValue + "  -->  " + aggValue);

							}
						}*/
						
						
					}
					
					double [] bestObjectives = bestSol.getObjectives();
					bestObjectives = Vector.negate(bestObjectives);
					System.out.println("Best Solution " + ":");
					System.out.println("    Sack 1 Profit: " + bestObjectives[0]);
					System.out.println("    Sack 2 Profit: " + bestObjectives[1]);
					System.out.println("    Total Proft:  " + (double)(critWeight * bestObjectives[0] + secWeight * bestObjectives[1]));
					System.out.println("    Binary String: " + bestSol.getVariable(0));
					
					double [] minDistObjectives = minDistSol.getObjectives();
					minDistObjectives = Vector.negate(minDistObjectives);
					System.out.println("Minimun Distance Solution " + ":");
					System.out.println("    Sack 1 Profit: " + minDistObjectives[0]);
					System.out.println("    Sack 2 Profit: " + minDistObjectives[1]);
					System.out.println("    Total Proft:  " + (double)(critWeight * minDistObjectives[0] + secWeight * minDistObjectives[1]));
					System.out.println("    Binary String: " + minDistSol.getVariable(0));
					
					
                   boolean [] d = null;
					
					if (critWeight == secWeight)
					d = EncodingUtils.getBinary(minDistSol.getVariable(0));
					else
				    d = EncodingUtils.getBinary(bestSol.getVariable(0));
					
				/*	double secMax=Double.MIN_VALUE;
					double secMin = Double.MAX_VALUE;
					double critMax = Double.MIN_VALUE;
					double critMin = Double.MAX_VALUE;
					
					for (int j2=0; j2<d.length;j2++)
					{
						if (d[j2])
						{
							int key = device.getChildEndDeviceIds().get(j2);
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
							double secValue = device.getChildSecMap().get(key);
							double critValue = device.getChildCripMap().get(key);
							
						//	System.out.println(dv.getName() + "    id:  " + device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue);
						
                            if (secValue > secMax)
                            	secMax = secValue;
                            if (secValue < secMin)
                            	secMin = secValue;
                            
                            if (critValue > critMax)
                            	critMax = critValue;
                            if (critValue < critMin)
                            	critMin = critValue;

						}
					}*/
					
					for (int j1=0; j1<d.length;j1++)
					{
						if (d[j1])
						{
							int key = device.getChildEndDeviceIds().get(j1);
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
							double secValue = device.getChildSecMap().get(key);
							double critValue = device.getChildCripMap().get(key);
							
							secMax = dv.getMaxSecVal();
							secMin = dv.getMinSecVal();
							
							critMax = dv.getMaxCritVal();
							critMin = dv.getMinCritVal();
							
							//System.out.println(dv.getName() + "     id:  "+device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue);
						
                         //   double aggValue = secWeight * secValue/100 + critWeight * critValue/100;
							
						//	double aggValue = secWeight * (secValue-secMin)/(secMax-secMin) + critWeight * (critValue-critMin)/(critMax-critMin);

							double aggValue = secWeight * (secValue-secMin)/(secMax) + critWeight * (critValue-critMin)/(critMax);
	
							
							
							if (aggValue > dv.getAggProfitValue())
							{
								dv.setAggProfitValue(aggValue);
								dv.setPrefFogDeviceName(device.getName());
							}
							
							System.out.println(dv.getName() + "    id:  " + device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue + "  -->  " + aggValue);

					
						}
					} 
					

				}
				
				
			}
			
			
			for(MyFogDevice device:getMyFogDevices())
			{
				int deviceParent = -1;
				List<Integer>children = new ArrayList<Integer>();
				
				if(device.getLevel()==1)
				{
					
					
					List<Item> itms = new ArrayList<Item>();	
					
					
			/*		device.setChildCritMap(new LinkedHashMap<Integer,Double>());
					device.setChildSecMap(new LinkedHashMap<Integer,Double>());
					device.setChildMipsMap(new LinkedHashMap<Integer,Double>());*/
					
					
					if(!deviceMipsInfo.containsKey(device.getId()))
						deviceMipsInfo.put(device.getId(), 0);
					deviceParent = device.getParentId();
					
					if (!deviceMipsInfo.containsKey(deviceParent))
						deviceMipsInfo.put(deviceParent, 0);
					
				/*	for(MyFogDevice deviceChild:getMyFogDevices())
					{
						if(deviceChild.getParentId()==device.getId())
						{
							
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(deviceChild.getId());
							itms.addAll(dv.getrollOverItems());
							children.add(deviceChild.getId());
							device.getChildEndDeviceIds().addAll(dv.getChildEndDeviceIds());
							device.getChildMipsMap().putAll(dv.getChildMipsMap());
							device.getChildCripMap().putAll(dv.getChildCripMap());
						}
					}*/
					
				/*	for (int key: device.getChildEndDeviceIds())
					{
					
					 MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
					 
					 int nHops = dv.getLevel() - device.getLevel();
					 
					 double critValue = (double) (device.getChildCripMap().get(key)/nHops);
					 
					 device.getChildCripMap().put(key, critValue);
						
					  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
					  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
					  
					  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
					  
					  double secValue = suit/device.getMaxSuitabilityFactor();
					  
					  device.getChildSecMap().put(key, secValue*100);
					  
					//  System.out.println(device.getName() + " Suitability To  " + CloudSim.getEntityName(key) +  " is " + suit/device.getMaxSuitabilityFactor() );
					}*/
				//	System.out.println(device.getChildEndDeviceIds());
					
				//	device.setRollOverItems(itms);
					
					
					
					int nsacks =2;
					int nitems = device.getChildEndDeviceIds().size();
					
					
					
					System.out.println(device.getName() + " Child Ids  " + device.getChildEndDeviceIds());
					
					double [][]profit = new double [nsacks][nitems];
					double [][]weight = new double [nsacks][nitems];
					int capacity = device.getMips();
					
					for (int i=0; i<device.getChildEndDeviceIds().size()-1;i++)
					{
						int key = device.getChildEndDeviceIds().get(i);
						profit[0][i] = device.getChildCripMap().get(key);
						profit[1][i] = device.getChildSecMap().get(key);
						weight[0][i] = device.getChildMipsMap().get(key);
						weight[1][i] = device.getChildMipsMap().get(key);
						
					}
					
					Instant start = Instant.now();
					NondominatedPopulation result = new Executor()
							.withProblemClass(KnapsackP.class, profit,weight,nitems,capacity)
							.withAlgorithm("NSGAII")
							.withMaxEvaluations(10000)
							.distributeOnAllCores()
							.run();
					Instant finish = Instant.now();
					 
				    long timeElapsed = Duration.between(start, finish).toMillis();
					System.out.println("Elapsed time to solve MOEA for  " + device.getName() + "  is:   " + timeElapsed);
				    new Plot() .add("NSGAII", result) .show();
					 
					
					double secWeight = getMyApplication().getSecWeight();
					double critWeight = getMyApplication().getCritWeight();
					
					double maxSolVal = Double.MIN_VALUE;
					Solution bestSol = null;
					
					Solution minDistSol = null;
					double minDist = Double.MAX_VALUE;
					
					double maxObj0 = 0;
					double maxObj1 = 0;
					
					for (int k = 0; k < result.size(); k++) {
						
						Solution solu = result.get(k);
						double [] objs = solu.getObjectives();
						
						objs = Vector.negate(objs);
						
						if (objs[0] > maxObj0)
							maxObj0 = objs[0];
						if (objs[1] > maxObj1)
							maxObj1 = objs[1];
							
					}
					
					
					System.out.println("==============Proxy NSGA Results==================");
					
					System.out.println("Max objs0: " + maxObj0 + "   " + "Max Obj1:   " + maxObj1);
					
					
					double secMax=Double.MIN_VALUE;
					double secMin = Double.MAX_VALUE;
					double critMax = Double.MIN_VALUE;
					double critMin = Double.MAX_VALUE;
					
					
					for (int i = 0; i < result.size(); i++) {
						Solution sol = result.get(i);
						double[] objectives = sol.getObjectives();
								
						// negate objectives to return them to their maximized form
						objectives = Vector.negate(objectives);
						
                      //  double tmpVal = (double)(critWeight * objectives[0] + secWeight * objectives[1]);
						
						double tmpVal = (double)(critWeight * (objectives[0]) + secWeight * (objectives[1]));
						
						if (tmpVal > maxSolVal)
						{
							maxSolVal = tmpVal;
							bestSol = sol;
						}
						
						  double dist = Math.sqrt(Math.pow(objectives[0], 2) + Math.pow(objectives[1], 2));
	                       // System.out.println("Solution " + (i+1) + "   Dist: " + dist );
	                        
	                        
	                        if (dist < minDist)
	                        {
	                        	minDist = dist;
	                        	minDistSol = sol;
	                        }
								
						System.out.println("Solution " + (i+1) + ":");
						System.out.println("    Sack 1 Profit: " + objectives[0]);
						System.out.println("    Sack 2 Profit: " + objectives[1]);
						System.out.println("    Total Proft:  " + (double)(critWeight * (objectives[0]) + secWeight * (objectives[1])));
						System.out.println("    Distance  : "   + dist);
						System.out.println("    Binary String: " + sol.getVariable(0));
						
						/*boolean[] d = EncodingUtils.getBinary(sol.getVariable(0));
						for (int j1=0; j1<d.length;j1++)
						{
							if (d[j1])
							{
								int key = device.getChildEndDeviceIds().get(j1);
								double secValue = device.getChildSecMap().get(key);
								double critValue = device.getChildCripMap().get(key);
								
                                MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
								
								secMax = dv.getMaxSecVal();
								secMin = dv.getMinSecVal();
								
								critMax = dv.getMaxCritVal();
								critMin = dv.getMinCritVal();
								
								//System.out.println(dv.getName() + "     id:  "+device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue);
							
	                         //   double aggValue = secWeight * secValue/100 + critWeight * critValue/100;
								
								double aggValue = secWeight * (secValue-secMin)/(secMax-secMin) + critWeight * (critValue-critMin)/(critMax-critMin);
							
                               // double aggValue = secWeight * secValue + critWeight * critValue;
								
							//	double aggValue = secWeight * (secValue/100) + critWeight * (critValue/100);
								
						//		System.out.println(device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue + "  -->  " + aggValue);
								

							//	MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
								
								if (aggValue > dv.getAggProfitValue())
								{
									dv.setAggProfitValue(aggValue);
									dv.setPrefFogDeviceName(device.getName());
								}
								
								System.out.println(dv.getName() + "    id:  " + device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue + "  -->  " + aggValue);

						
							}
						} */
						
					}
					
					double [] bestObjectives = bestSol.getObjectives();
					bestObjectives = Vector.negate(bestObjectives);
					System.out.println("Best Solution " + ":");
					System.out.println("    Sack 1 Profit: " + bestObjectives[0]);
					System.out.println("    Sack 2 Profit: " + bestObjectives[1]);
					System.out.println("    Total Proft:  " + (double)(critWeight * bestObjectives[0] + secWeight * bestObjectives[1]));
					System.out.println("    Binary String: " + bestSol.getVariable(0));
					
					double [] minDistObjectives = minDistSol.getObjectives();
					minDistObjectives = Vector.negate(minDistObjectives);
					System.out.println("Minimun Distance Solution " + ":");
					System.out.println("    Sack 1 Profit: " + minDistObjectives[0]);
					System.out.println("    Sack 2 Profit: " + minDistObjectives[1]);
					System.out.println("    Total Proft:  " + (double)(critWeight * minDistObjectives[0] + secWeight * minDistObjectives[1]));
					System.out.println("    Binary String: " + minDistSol.getVariable(0));
					
					
                    boolean [] d = null;
					
					if (critWeight == secWeight)
					d = EncodingUtils.getBinary(minDistSol.getVariable(0));
					else
				    d = EncodingUtils.getBinary(bestSol.getVariable(0));
					
				/*	double secMax=Double.MIN_VALUE;
					double secMin = Double.MAX_VALUE;
					double critMax = Double.MIN_VALUE;
					double critMin = Double.MAX_VALUE;
					
					for (int j2=0; j2<d.length;j2++)
					{
						if (d[j2])
						{
							int key = device.getChildEndDeviceIds().get(j2);
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
							double secValue = device.getChildSecMap().get(key);
							double critValue = device.getChildCripMap().get(key);
							
						//	System.out.println(dv.getName() + "    id:  " + device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue);
						
                            if (secValue > secMax)
                            	secMax = secValue;
                            if (secValue < secMin)
                            	secMin = secValue;
                            
                            if (critValue > critMax)
                            	critMax = critValue;
                            if (critValue < critMin)
                            	critMin = critValue;

						}
					}*/
					
					
					for (int j1=0; j1<d.length;j1++)
					{
						if (d[j1])
						{
							int key = device.getChildEndDeviceIds().get(j1);
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
							double secValue = device.getChildSecMap().get(key);
							double critValue = device.getChildCripMap().get(key);
							
					//		System.out.println(dv.getName() + "    id:  " + device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue);
						
                      //      double aggValue = secWeight * secValue/100 + critWeight * critValue/100;
							
							secMax = dv.getMaxSecVal();
							secMin = dv.getMinSecVal();
							
							critMax = dv.getMaxCritVal();
							critMin = dv.getMinCritVal();
							
							
							
						//	double aggValue = secWeight * (secValue-secMin)/(secMax-secMin) + critWeight * (critValue-critMin)/(critMax-critMin);

							double aggValue = secWeight * (secValue-secMin)/(secMax) + critWeight * (critValue-critMin)/(critMax);
	
							
							
							if (aggValue > dv.getAggProfitValue())
							{
								dv.setAggProfitValue(aggValue);
								dv.setPrefFogDeviceName(device.getName());
							}
							
							System.out.println(dv.getName() + "    id:  " + device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue + "  -->  " + aggValue);

					
						}
					} 
					
					
				
			    }
			}
			
			for(MyFogDevice device:getMyFogDevices())
			{
				int deviceParent = -1;
				List<Integer>children = new ArrayList<Integer>();
				
				if(device.getLevel()==0)
				{
					
					List<Item> itms = new ArrayList<Item>();
					
					/*device.setChildCritMap(new LinkedHashMap<Integer,Double>());
					device.setChildSecMap(new LinkedHashMap<Integer,Double>());
					device.setChildMipsMap(new LinkedHashMap<Integer,Double>());*/
					
				
					
					if(!deviceMipsInfo.containsKey(device.getId()))
						deviceMipsInfo.put(device.getId(), 0);
					deviceParent = device.getParentId();
					
					if (!deviceMipsInfo.containsKey(deviceParent))
						deviceMipsInfo.put(deviceParent, 0);
					
				/*	for(MyFogDevice deviceChild:getMyFogDevices())
					{
						if(deviceChild.getParentId()==device.getId())
						{
							
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(deviceChild.getId());
							itms.addAll(dv.getrollOverItems());
							children.add(deviceChild.getId());
							device.getChildEndDeviceIds().addAll(dv.getChildEndDeviceIds());
							device.getChildMipsMap().putAll(dv.getChildMipsMap());
							device.getChildCripMap().putAll(dv.getChildCripMap());
						}
					}*/
					
				/*	for (int key: device.getChildEndDeviceIds())
					{
					
					  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
					  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
					  
					  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
					  
					  MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
					  MyFogDevice dvParent = (MyFogDevice) CloudSim.getEntity(dv.getParentId());
					  
					  int nHops = dv.getLevel() - device.getLevel();
					  double critValue = (double)(dvParent.getChildCripMap().get(key)/nHops);
					  
						
						 
						  device.getChildCripMap().put(key, critValue);
						
						  double secValue = suit/device.getMaxSuitabilityFactor();
						  
						  device.getChildSecMap().put(key, secValue*100);
						  
				//	  System.out.println(device.getName() + " Suitability To  " + CloudSim.getEntityName(key) +  " is " + suit/device.getMaxSuitabilityFactor() );
					  
					  
					}*/
					
					
					int nsacks =2;
					int nitems = device.getChildEndDeviceIds().size();
					
					
					double maxSolVal = Double.MIN_VALUE;
					Solution bestSol = null;
					Solution minDistSol = null;
					double minDist = Double.MAX_VALUE;
					//System.out.println(device.getName() + "   " + device.getChildEndDeviceIds());
					
					double [][]profit = new double [nsacks][nitems];
					double [][]weight = new double [nsacks][nitems];
					int capacity = device.getMips();
					
					
					System.out.println(device.getName() + " Child Id  " + device.getChildEndDeviceIds());
					
					for (int i=0; i<device.getChildEndDeviceIds().size()-1;i++)
					{
						int key = device.getChildEndDeviceIds().get(i);
						profit[0][i] = device.getChildCripMap().get(key);
						profit[1][i] = device.getChildSecMap().get(key);
						weight[0][i] = device.getChildMipsMap().get(key);
						weight[1][i] = device.getChildMipsMap().get(key);
						
					}
					
					Instant start = Instant.now();
					NondominatedPopulation result = new Executor()
							.withProblemClass(KnapsackP.class, profit,weight,nitems,capacity)
							.withAlgorithm("NSGAII")
							.withMaxEvaluations(10000)
							.distributeOnAllCores()
							.run();
					
					
					Instant finish = Instant.now();
					 
				    long timeElapsed = Duration.between(start, finish).toMillis();
					System.out.println("Elapsed time to solve MOEA for  " + device.getName() + "  is:   " + timeElapsed);
					
					  new Plot() .add("NSGAII", result) .show();
					 
					
					double secWeight = getMyApplication().getSecWeight();
					double critWeight = getMyApplication().getCritWeight();
					
					double maxObj0 = 0;
					double maxObj1 = 0;
					
					for (int k = 0; k < result.size(); k++) {
						
						Solution solu = result.get(k);
						double [] objs = solu.getObjectives();
						
						objs = Vector.negate(objs);
						
						if (objs[0] > maxObj0)
							maxObj0 = objs[0];
						if (objs[1] > maxObj1)
							maxObj1 = objs[1];
							
					}
					
					System.out.println("==============Cloud NSGA Results==================");
					
					System.out.println("Max objs0: " + maxObj0 + "   " + "Max Obj1:   " + maxObj1);
					
					double secMax=0;
					double secMin = 0;
					double critMax = 0;
					double critMin = 0;
				
					for (int i = 0; i < result.size(); i++) {
						Solution sol = result.get(i);
						double[] objectives = sol.getObjectives();
								
						// negate objectives to return them to their maximized form
						objectives = Vector.negate(objectives);
						
                    //    double tmpVal = (double)(critWeight * objectives[0] + secWeight * objectives[1]);
						
						double tmpVal = (double)(critWeight * (objectives[0]) + secWeight * (objectives[1]));
                        
                        double dist = Math.sqrt(Math.pow(objectives[0], 2) + Math.pow(objectives[1], 2));
                       // System.out.println("Solution " + (i+1) + "   Dist: " + dist );
                        
                        
                        if (dist < minDist)
                        {
                        	minDist = dist;
                        	minDistSol = sol;
                        }
						
						if (tmpVal > maxSolVal)
						{
							maxSolVal = tmpVal;
							bestSol = sol;
						}
								
								
						System.out.println("Solution " + (i+1) + ":");
						System.out.println("    Sack 1 Profit: " + objectives[0] + "-->" + objectives[0]/maxObj0);
						System.out.println("    Sack 2 Profit: " + objectives[1] + "-->" + objectives[1]/maxObj1);
						System.out.println("    Total Proft:  " + (double)(critWeight * (objectives[0]) + secWeight * (objectives[1])));
						System.out.println("    Binary String: " + sol.getVariable(0));
						System.out.println("    Distance  : "   + dist);
						
						
					/*	boolean[] d = EncodingUtils.getBinary(sol.getVariable(0));
						
						for (int j=0; j<d.length;j++)
						{
							if (d[j])
							{
								double aggValue =0.0;
								int key = device.getChildEndDeviceIds().get(j);
								double secValue = device.getChildSecMap().get(key);
								double critValue = device.getChildCripMap().get(key);
								
								//double aggValue = secWeight * secValue + critWeight * critValue;
								//double aggValue = secWeight * (secValue/100) + critWeight * (critValue/100);
								
                                MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
								
								secMax = dv.getMaxSecVal();
								secMin = dv.getMinSecVal();
								
								critMax = dv.getMaxCritVal();
								critMin = dv.getMinCritVal();
								
								//System.out.println(dv.getName() + "     id:  "+device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue);
							
	                         //   double aggValue = secWeight * secValue/100 + critWeight * critValue/100;
							
								
								if (secMax == secMin)
								{
									 aggValue = (secWeight * (secValue)/(secMax)) + (critWeight * (critValue-critMin)/(critMax-critMin));

								}
								
								else
								{
								 aggValue = (secWeight * (secValue-secMin)/(secMax-secMin)) + (critWeight * (critValue-critMin)/(critMax-critMin));
								}
								// aggValue = (secWeight * (secValue-secMin)/(secMax-secMin)) + (critWeight * (critValue-critMin)/(critMax-critMin));

								
								if (aggValue > dv.getAggProfitValue())
								{
									dv.setAggProfitValue(aggValue);
									dv.setPrefFogDeviceName(device.getName());
								}
								
								System.out.println(dv.getName() + "    id:  " + device.getChildEndDeviceIds().get(j) + "  Sec Value " + secValue + "  Crit Value " + critValue + "  -->  " + aggValue);
							}
						}*/
						
						
					}
					
					
					double [] bestObjectives = bestSol.getObjectives();
					bestObjectives = Vector.negate(bestObjectives);
					System.out.println("Best Solution " + ":");
					System.out.println("    Sack 1 Profit: " + bestObjectives[0]);
					System.out.println("    Sack 2 Profit: " + bestObjectives[1]);
					System.out.println("    Total Proft:  " + (double)(critWeight * bestObjectives[0] + secWeight * bestObjectives[1]));
					System.out.println("    Binary String: " + bestSol.getVariable(0));
					
					double [] minDistObjectives = minDistSol.getObjectives();
					minDistObjectives = Vector.negate(minDistObjectives);
					System.out.println("Minimun Distance Solution " + ":");
					System.out.println("    Sack 1 Profit: " + minDistObjectives[0]);
					System.out.println("    Sack 2 Profit: " + minDistObjectives[1]);
					System.out.println("    Total Proft:  " + (double)(critWeight * minDistObjectives[0] + secWeight * minDistObjectives[1]));
					System.out.println("    Binary String: " + minDistSol.getVariable(0));
					
					/*double secMax=Double.MIN_VALUE;
					double secMin = Double.MAX_VALUE;
					double critMax = Double.MIN_VALUE;
					double critMin = Double.MAX_VALUE;*/
					
					boolean [] d = null;
					
					if (critWeight == secWeight)
					d = EncodingUtils.getBinary(minDistSol.getVariable(0));
					else
				    d = EncodingUtils.getBinary(bestSol.getVariable(0));
					
					
				/*	for (int j1=0; j1<d.length;j1++)
					{
						if (d[j1])
						{
							int key = device.getChildEndDeviceIds().get(j1);
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
							double secValue = device.getChildSecMap().get(key);
							double critValue = device.getChildCripMap().get(key);
							
						//	System.out.println(dv.getName() + "    id:  " + device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue);
						
                            if (secValue > secMax)
                            	secMax = secValue;
                            if (secValue < secMin)
                            	secMin = secValue;
                            
                            if (critValue > critMax)
                            	critMax = critValue;
                            if (critValue < critMin)
                            	critMin = critValue;

						}
					}*/
					
					for (int j1=0; j1<d.length;j1++)
					{
						if (d[j1])
						{
							int key = device.getChildEndDeviceIds().get(j1);
							MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
							double secValue = device.getChildSecMap().get(key);
							double critValue = device.getChildCripMap().get(key);
							
						//	System.out.println(dv.getName() + "    id:  " + device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue);
						
                         //   double aggValue = secWeight * secValue/100 + critWeight * critValue/100;
							
							secMax = dv.getMaxSecVal();
							secMin = dv.getMinSecVal();
							
							critMax = dv.getMaxCritVal();
							critMin = dv.getMinCritVal();
							
							
							
						//	double aggValue = secWeight * (secValue-secMin)/(secMax-secMin) + critWeight * (critValue-critMin)/(critMax-critMin);
							
							double aggValue = secWeight * (secValue-secMin)/(secMax) + critWeight * (critValue-critMin)/(critMax);

							
							if (aggValue > dv.getAggProfitValue())
							{
								dv.setAggProfitValue(aggValue);
								dv.setPrefFogDeviceName(device.getName());
							}
					
							System.out.println(dv.getName() + "    id:  " + device.getChildEndDeviceIds().get(j1) + "  Sec Value " + secValue + "  Crit Value " + critValue + "  -->  " + aggValue);

						}
					}
					
					
			    }
			}
			
			
			MyFogDevice cloud = (MyFogDevice) CloudSim.getEntity("cloud");
			MyFogDevice proxy = (MyFogDevice) CloudSim.getEntity("proxy-server");
			
			
			
		//	System.out.println(cloud.getChildEndDeviceIds());
			double secWeight = getMyApplication().getSecWeight();
			double critWeight = getMyApplication().getCritWeight();
			
			Instant start = Instant.now();

			
			for (Integer key: cloud.getChildEndDeviceIds())
			{
				MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(key);
				
				
				String prefDevice =null;
				
				if (dv.getPrefFogDeviceName() == null)
				{
					
					
					if (secWeight > critWeight)
					{
						if (cloud.computeNodeSecurityScore() > proxy.computeNodeSecurityScore())
							prefDevice = "cloud";
						else
							prefDevice = "proxy-server";
					}
					
					else 
						{prefDevice = "proxy-server";
					}
				
					dv.setPrefFogDeviceName(prefDevice);
				}
				
					
				
				System.out.println(dv.getName() + " Key:   " + key + "  Pref Fog Device  " + dv.getPrefFogDeviceName()   + "  Agg Value  " + dv.getAggProfitValue());
				System.out.println(dv.getName() + " Key:   " + key + "  MaxCrit  " + dv.getMaxCritVal()   + "  MinCrit  " + dv.getMinCritVal());
				System.out.println(dv.getName() + " Key:   " + key + "  MaxSec  " + dv.getMaxSecVal()   + "  MinSec  " + dv.getMinSecVal());

				System.out.println("+++++++++++++++++++++++++++++++++++++++++++++");

				AppModule appModule =getMyApplication().getModuleByName(moduleToPlace);
				
				MyFogDevice device = (MyFogDevice) CloudSim.getEntity(dv.getPrefFogDeviceName());
				
           //    System.out.println(device.getId());
				
				if(!getDeviceToModuleMap().containsKey(device.getId())) {
					  List<AppModule>placedModules = new ArrayList<AppModule>();
					  placedModules.add(appModule); 
					  getDeviceToModuleMap().put(device.getId(), placedModules); 
					  device.getAssociatedEndDevices().add(dv.getName());
					  TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(dv.getName(),device.getName());
				}  else { List<AppModule>placedModules =
						  getDeviceToModuleMap().get(device.getId()); 
				          placedModules.add(appModule);
						  getDeviceToModuleMap().put(device.getId(), placedModules);
						  device.getAssociatedEndDevices().add(dv.getName());
						  TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(dv.getName(),device.getName()); 
						  }
			
			
			}
			Instant finish = Instant.now();
			 
		    long timeElapsed = Duration.between(start, finish).toMillis();
			System.out.println("Elapsed time to Map AppModules is:   " + timeElapsed);
			
			
			
			
			/*
			 * for(MyFogDevice device:getMyFogDevices()) { if (device.getLevel() ==2 ||
			 * device.getLevel() ==1 || device.getLevel() ==0 ) { List<Item> itms = new
			 * ArrayList<Item>(device.getKnapsackSolution().items);
			 * 
			 * for (Item itm: itms) {
			 * 
			 * if (itm.getItemName()!=null) { AppModule appModule
			 * =getMyApplication().getModuleByName(moduleToPlace);
			 * 
			 * String name = itm.getItemName();
			 * 
			 * if(!getDeviceToModuleMap().containsKey(device.getId())) {
			 * List<AppModule>placedModules = new ArrayList<AppModule>();
			 * placedModules.add(appModule); getDeviceToModuleMap().put(device.getId(),
			 * placedModules); device.getAssociatedEndDevices().add(name);
			 * TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(name,device.getName())
			 * ; } else { List<AppModule>placedModules =
			 * getDeviceToModuleMap().get(device.getId()); placedModules.add(appModule);
			 * getDeviceToModuleMap().put(device.getId(), placedModules);
			 * device.getAssociatedEndDevices().add(name);
			 * TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(name,device.getName())
			 * ; }
			 * 
			 * } }
			 * 
			 * } }
			 */
		}	
	}
	
	public ModuleMapping getModuleMapping() {
		return moduleMapping;
	}

	public void setModuleMapping(ModuleMapping moduleMapping) {
		this.moduleMapping = moduleMapping;
	}


	public List<MySensor> getMySensors() {
		return sensors;
	}

	public void setMySensors(List<MySensor> sensors) {
		this.sensors = sensors;
	}

	public List<MyActuator> getMyActuators() {
		return actuators;
	}

	public void setMyActuators(List<MyActuator> actuators) {
		this.actuators = actuators;
	}
	
	private static double getvalue(double min, double max)
	{
		Random r = new Random();
		double randomValue = min + (max - min) * r.nextDouble();
		return randomValue;
	}
	
	
	 public  double computeFogSecurityScore(){
	        double score = 0;
	        
	        for(MyFogDevice device : this.getMyFogDevices()){
				score+=device.computeNodeSecurityScore();
			}
	        return score;
	    }
	 
	 //Percent security score
	 public  double computePercentFogSecurityScore()
	 {
		 double tmp;
		 
		 double size = this.getMyFogDevices().size() * SecurityConstants.allCountermeasures.size() ;
		 tmp =  computeFogSecurityScore()/size*100;
		 return tmp;
	 }
	 
	 
	 public  double computeFogSecurityScoreByType(String type){
	        double score = 0;
	        
	        for(MyFogDevice device : this.getMyFogDevices()){
				score+=device.computeNodeSecurityScoreByType(type);
			}
	        
	        return score;
	    }
	 
	 
	 public  double computePercentFogSecurityScoreByType(String type){
	        double size = (SecurityTaxonomy.getSecurityMeasuresByType(type).size()* this.getMyFogDevices().size());
			 double result = 100*computeFogSecurityScoreByType(type)/size;
			 return result;
	    }
	
	
	 public  MyFogDevice getMinSecurityScoreFogDevice()
	    {
	    	MyFogDevice dc = null;
	    	double min = Double.MAX_VALUE;
	    	double tmp;
	    	for (MyFogDevice device : this.getMyFogDevices()){
	    		
	    		tmp = device.computeNodeSecurityScore();
	    		
	           if (tmp < min)
	           {
	        	   min = tmp;
	        	   dc = device;
	           }
	        	   
	        }
	    	return dc;
	    }
	 
	 
	 
	    
	    public MyFogDevice getMaxSecurityScoreFogDevice()
	    {
	    	MyFogDevice dc = null;
	    	double max = Double.MIN_VALUE;
	    	double tmp = 0;
	
	    	
	    	for (MyFogDevice device : this.getMyFogDevices()){
	    		
	    		tmp = device.computeNodeSecurityScore();
	    		
	           if (tmp > max)
	           {
	        	   max = tmp;
	        	   dc = device;
	           }
	        	   
	        }
	    	return dc;
	    		
	    }
	    
	    
	    public MyFogDevice getMinSecurityScoreFogDeviceByType(String type)
	    {
	    	MyFogDevice dc = null;
	    	double min = Double.MAX_VALUE;
	    	double tmp =0;
	    	for (MyFogDevice device : this.getMyFogDevices()){
	           tmp = device.computeNodeSecurityScoreByType(type);
	           if (tmp < min)
	           {
	        	   min = tmp;
	        	   dc = device;
	           }
	        	   
	        }
	    	return dc;
	    }
	    
	   
	    public MyFogDevice getMaxSecurityScoreFogDeviceByType(String type)
	    {
	    	MyFogDevice dc = null;
	    	double max = Double.MIN_VALUE;
	    	double tmp;
	    	for (MyFogDevice device : this.getMyFogDevices()){
	           tmp = device.computeNodeSecurityScoreByType(type);
	           if (tmp > max)
	           {
	        	   max = tmp;
	        	   dc = device;
	           }
	        	   
	        }
	    	return dc;
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
	    		showDetailedSecurityScoreofFogDevice(device);
	    	}
	    }
	    
	 
	    public List<MyFogDevice> getFogDevicesSupportSecReq(List<String> reqs)
	    {
	    	List<MyFogDevice> fogs = new ArrayList<MyFogDevice>();
	    	
	    	for (MyFogDevice device: this.getMyFogDevices())
	    	{
	    		if (device.supportsSecurityRequirements(reqs))
	    		{
	    			
	    		//	SimLogger.print(dcName + "  "+ Double.toString(this.computeEdgeSecurityScoreByDC(dcName)) + "     \n");
	    			fogs.add(device);
	    		}
	    	}
	    	
	    	
	    	return fogs;
	    }
	    
	    public  MyFogDevice getFogDeviceWithMaxScoreInList(List<MyFogDevice> dcs)
	    {
	    	MyFogDevice dv = null;
	    	double max = Double.MIN_VALUE;
	    	double tmp=0;
	    	
	    	if (!dcs.isEmpty())
	    	{
	    		
	    		for (MyFogDevice device: dcs)
	    		{
	    			tmp = device.computePercentNodeSecurityScore();
	    			
	    			if (tmp > max)
	    			{
	    				max = tmp;
	    				dv = device;
	    			}	
	    			
	    		}
	    		
	    	}
	    	return dv;
	    	
	    }

}

