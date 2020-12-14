package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.MyApplication;
import org.fog.entities.FogDevice;
import org.fog.entities.MyActuator;
import org.fog.entities.MyFogDevice;
import org.fog.entities.MySensor;
import org.fog.security.SecurityConstants;
import org.fog.security.SecurityParametersTypes;
import org.fog.security.SecurityTaxonomy;
import org.fog.utils.TimeKeeper;

import org.knapsack.Knapsack;
import org.knapsack.Item;
import org.knapsack.Solution;


public class MyModulePlacementKnapSack extends MyPlacement{
	
	protected ModuleMapping moduleMapping;
	protected List<MySensor> sensors;
	protected List<MyActuator> actuators;
	protected String moduleToPlace;
	protected Map<Integer, Integer> deviceMipsInfo;
	
	
	static boolean Crit = true;
	
	
	public MyModulePlacementKnapSack(List<MyFogDevice> fogDevices, List<MySensor> sensors, List<MyActuator> actuators, 
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
			System.out.println("I am here !");
			
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
					
					
					
					  for(int i = 0; i<keys.size()-1; i++) { for(int j=0;j<keys.size()-i-1;j++) {
					  if(childDeadline.get(keys.get(j))>childDeadline.get(keys.get(j+1))) { int
					  tempJ = keys.get(j); int tempJn = keys.get(j+1); keys.set(j, tempJn);
					  keys.set(j+1, tempJ); } } }
					 
					
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
						  
						  System.out.println(device.getName() + " Suitability To  " + CloudSim.getEntityName(key) +  " is " + suit/device.getMaxSuitabilityFactor() );
							  
						  
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
						  TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(name,
						  device.getName());
						  
						  
						  } 
						  else 
						  { List<AppModule>placedModules = getDeviceToModuleMap().get(device.getId()); 
						  placedModules.add(appModule);
						  getDeviceToModuleMap().put(device.getId(), placedModules);
						  device.getAssociatedEndDevices().add(name);
						  TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(name,
						  device.getName()); } } else { MyFogDevice dv = (MyFogDevice)
						  CloudSim.getEntity(deviceParent);
						  
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
						  
						  } else { int id = dv.getParentId(); MyFogDevice dv_1 = (MyFogDevice)
						  CloudSim.getEntity(id);
						  
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
				List<Integer>children = new ArrayList<Integer>();
				
				if(device.getLevel()==2)
				{
					
					double maxCritVal = Double.MIN_VALUE;
					double maxSecVal = Double.MIN_VALUE;
					
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
							
							double critValue = (double) nVal;
							
							
							int key = deviceChild.getId();
							
							  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
							  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
							  
							  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
							  
							 // double secValue = suit/maxSecVal;
							  double secValue = suit/device.getMaxSuitabilityFactor();
							  
							  double secWeight = 0;
							  double critWeight = 1;
							  
							  double finalVal = secWeight * secValue + critWeight * critValue;
							  
							  System.out.println(dv.getName() + "  Crit Value  " + critValue   +  "  Sec Value  " + secValue);
							  
							  
							  int itemValue = (int) (finalVal * 100);
							  
							  
							System.out.println(dv.getName() + "  final value  " + itemValue);
							
							double mips = getMyApplication().getModuleByName(moduleToPlace).getMips();
							mips = mips + getMyApplication().getAdditionalMipsInfo().get(deviceChild.getId()).get(moduleToPlace);
						//	Item itm = new Item(dv.getName(),(int) (val *( 1-1/cont)*10000),(int)mips);
						//	Item itm = new Item(dv.getName(),(int) ((nVal) *(1/cont)*10000),(int)mips);
							Item itm = new Item(dv.getName(),itemValue,(int)mips);
							
							//System.out.println(dv.getName() + "   " + cont + "   " + 1/cont   + "    " + (1-1/cont));
							
							
							//Item itm = new Item(dv.getName(),val,(int)mips);
						
							
							itms.add(itm);
							children.add(deviceChild.getId());
						 
							 // System.out.println(device.getName() + " Suitability To  " + CloudSim.getEntityName(key) +  " is " + suit/device.getMaxSuitabilityFactor() );
						}
					}
					
					device.setChildEndDeviceIds(children);
				//	device.setKnapsack(new Knapsack(itms,(int)device.getMips()));
					device.setKnapsack(new Knapsack(itms,(int)device.getMips()));
				//	device.getKnapsack().display();
					
					Solution solution = device.getKnapsack().solve();
					device.setKnapsackSolution(solution);
					
					//System.out.println("Items are:   " + solution.getSolutionItemNames());
					
					List<String> solNames = solution.getSolutionItemNames();
					
					/*
					 * for (int i=0;i<device.getKnapsack().getKnapsackItems().size();i++) { if
					 * (solNames.contains(device.getKnapsack().getKnapsackItems().get(i).getItemName
					 * ())) device.getKnapsack().getKnapsackItems().remove(i); }
					 */
					
					List<Item> newItms= new ArrayList<>();
					
					int size = itms.size();
					
					if (size !=0)
					{
						for (int i=0;i<size ;i++) { 
							String name = device.getKnapsack().getKnapsackItems().get(i).getItemName();
						//	String name = itms.get(i).getItemName();
							 if (!device.getKnapsack().getKnapSackMap().containsKey(name))
							 {
								 newItms.add(device.getKnapsack().getKnapsackItems().get(i));
								// newItms.add(itms.get(i));
							 }
						}
						device.setRollOverItems(newItms);
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
					
					
					double maxSecVal = Double.MIN_VALUE;
					double maxCritVal = Double.MIN_VALUE;
					
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
							itms.addAll(dv.getrollOverItems());
							children.add(deviceChild.getId());
							device.getChildEndDeviceIds().addAll(dv.getChildEndDeviceIds());
						}
					}
					
					for (int key: device.getChildEndDeviceIds())
					{
					
					  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
					  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
					  
					  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
					  
					  System.out.println(device.getName() + " Suitability To  " + CloudSim.getEntityName(key) +  " is " + suit/device.getMaxSuitabilityFactor() );
					}
				//	System.out.println(device.getChildEndDeviceIds());
					
				//	device.setRollOverItems(itms);
					
					
					
					for (Item itm: itms)
					{
						
						//System.out.println("I am here !");
				      //  Item itm = itms.get(i);		
						String name = itm.getItemName();
						MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(name);
						
						double cont =  getMyApplication().getDeadlineInfo().get(dv.getId()).get(moduleToPlace);
						int val = dv.getCriticalityLevel();
						if (val==0)
							val = val+1;
						else val = val+4;
						
						int noHops =  dv.getLevel() - device.getLevel() ;
						
						double nVal = (double) val/noHops;
						
						nVal = ((nVal) *(1/cont));
						
						if (nVal > maxCritVal)
							maxCritVal = nVal;
						
						int key = dv.getId();
						
						  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
						  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
						  
						  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
						  
						  if (suit > maxSecVal)
							  maxSecVal = suit;
						
					}
					
				List<Item> spareItms = new ArrayList<>();
				
				spareItms = itms;
				int j=0;
				//	for (int i=0;i<spareItms.size();i++)
				
				List<Item> tabuItems = new ArrayList<>();
				
				
					for (Item itm: itms)
					{
						
					   // Item itm = itms.get(i);
						
						//Item itm = spareItms.get(0);
						String name = itm.getItemName();
						MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(name);
						
						double cont =  getMyApplication().getDeadlineInfo().get(dv.getId()).get(moduleToPlace);
						int val = dv.getCriticalityLevel();
						if (val==0)
							val = val+1;
						else val = val+4;
						
						int noHops =  dv.getLevel() - device.getLevel() ;
						
						double nVal = (double) val/noHops;
						
						nVal = ((nVal) *(1/cont));
						
						double critValue = (double) nVal;
						
						
						int key = dv.getId();
						
						  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
						  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
						  
						  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
						  
						//  double secValue = suit/maxSecVal;
						  double secValue = suit/device.getMaxSuitabilityFactor();
						  
						  double secWeight = 0;
						  double critWeight = 1;
						  
						  double finalVal = secWeight * secValue + critWeight * critValue;
						  
						  int itemValue = (int) (finalVal * 100);
						
					//	System.out.println(val);
						
												  
						itm.setItemValue(itemValue);
					
					//	System.out.println(itm.getItemName() + " new value  " + itm.getItemValue());
						
					}
					
					//device.setKnapsack(new Knapsack(itms, device.getMips()));
					
					device.setKnapsack(new Knapsack(itms, device.getMips()));
					
					
					Solution solution = device.getKnapsack().solve();
					device.setKnapsackSolution(solution);
					
	                List<Item> newItms= new ArrayList<>();
					
					int size = itms.size();
					
					if (size !=0)
					{
						for (int i=0;i<size ;i++) { 
							//String name = device.getKnapsack().getKnapsackItems().get(i).getItemName();
							String name = itms.get(i).getItemName();
							 if (!device.getKnapsack().getKnapSackMap().containsKey(name))
							 {
								// newItms.add(device.getKnapsack().getKnapsackItems().get(i));
								 newItms.add(itms.get(i));
								 
							 }
						}
						device.setRollOverItems(newItms);
					}
					
					System.out.println(device.getName() + "  Rollover Size:   " + device.getrollOverItems().size());
			    }
			}
			
			
			
			for(MyFogDevice device:getMyFogDevices())
			{
				int deviceParent = -1;
				List<Integer>children = new ArrayList<Integer>();
				
				if(device.getLevel()==0)
				{
					
					List<Item> itms = new ArrayList<Item>();
					
					
					
					double maxSecVal = Double.MIN_VALUE;
					double maxCritVal = Double.MIN_VALUE;
					
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
							itms.addAll(dv.getrollOverItems());
							children.add(deviceChild.getId());
							device.getChildEndDeviceIds().addAll(dv.getChildEndDeviceIds());
						}
					}
					
					for (int key: device.getChildEndDeviceIds())
					{
					
					  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
					  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
					  
					  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
					  
					  System.out.println(device.getName() + " Suitability To  " + CloudSim.getEntityName(key) +  " is " + suit/device.getMaxSuitabilityFactor() );
					}
					
					for (Item itm: itms)
					{
						String name = itm.getItemName();
						MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(name);
						
						double cont =  getMyApplication().getDeadlineInfo().get(dv.getId()).get(moduleToPlace);
						int val = dv.getCriticalityLevel();
						if (val==0)
							val = val+1;
						else val = val+4;
						
						int noHops =  dv.getLevel() - device.getLevel() ;
						
						double nVal = (double) val/noHops;
						
						nVal = ((nVal) *(1/cont));
						
						if (nVal > maxCritVal)
							maxCritVal = nVal;
						
						int key = dv.getId();
						
						  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
						  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
						  
						  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
						  
						  if (suit > maxSecVal)
							  maxSecVal = suit;
						
					}
					
			
					List<Item> tabuItems = new ArrayList<>();
					
					for (Item itm: itms)
					{
						String name = itm.getItemName();
						MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(name);
						
						double cont =  getMyApplication().getDeadlineInfo().get(dv.getId()).get(moduleToPlace);
						int val = dv.getCriticalityLevel();
						if (val==0)
							val = val+1;
						else val = val+4;
						
						int noHops =  dv.getLevel() - device.getLevel() ;
						
						double nVal = (double) val/noHops;
						
						nVal = ((nVal) *(1/cont));
						
						double critValue = (double) nVal;
						
						
						int key = dv.getId();
						
						  List<String> essSecReq = getMyApplication().getModuleEssSecReqInfo().get(key).get(moduleToPlace);
						  List<String> nonEssSecReq = getMyApplication().getModuleNonEssSecReqInfo().get(key).get(moduleToPlace);
						  
						  double suit = device.computeSuitabilityToAppModule(essSecReq, nonEssSecReq);
						  
						//  double secValue = suit/maxSecVal;
						  double secValue = suit/device.getMaxSuitabilityFactor();
						  
						  double secWeight = 0;
						  double critWeight = 1;
						  
						  double finalVal = secWeight * secValue + critWeight * critValue;
						  
						  int itemValue = (int) (finalVal * 100);
						
					//	System.out.println(val);
						
						itm.setItemValue(itemValue);
						
						if (secValue < 0.5)
						{
							System.out.println(itm.getItemName() + " has been removed from knapsack of  " + device.getName() );
							
							tabuItems.add(itm);
							//itms.remove(itm);
						}
						
					//	System.out.println(itm.getItemName() + " new value  " + itm.getItemValue());
						
					}
					
				//	device.setRollOverItems(itms);
					
					device.setKnapsack(new Knapsack(itms, device.getMips()));
					
					//device.setKnapsack(new Knapsack(tabuItems, device.getMips()));
					
					Solution solution = device.getKnapsack().solve();
					device.setKnapsackSolution(solution);
					
	                List<Item> newItms= new ArrayList<>();
					
					int size = itms.size();
					
					if (size !=0)
					{
						for (int i=0;i<size ;i++) { 
							String name = device.getKnapsack().getKnapsackItems().get(i).getItemName();
							 if (!device.getKnapsack().getKnapSackMap().containsKey(name))
							 {
								 newItms.add(device.getKnapsack().getKnapsackItems().get(i));
							 }
						}
						device.setRollOverItems(newItms);
					}
				
					System.out.println(device.getName() + "  Rollover Size:   " + device.getrollOverItems().size());
			    }
			}
			
			
			for(MyFogDevice device:getMyFogDevices())
			{
				if (device.getLevel() ==2 || device.getLevel() ==1 || device.getLevel() ==0 )
				{
					List<Item> itms = new ArrayList<Item>(device.getKnapsackSolution().items);
					
					for (Item itm: itms)
					{
						
						if (itm.getItemName()!=null)
						{
						AppModule appModule =getMyApplication().getModuleByName(moduleToPlace);
						
						String name = itm.getItemName();
						
						if(!getDeviceToModuleMap().containsKey(device.getId())) {
							  List<AppModule>placedModules = new ArrayList<AppModule>();
							  placedModules.add(appModule); 
							  getDeviceToModuleMap().put(device.getId(), placedModules); 
							  device.getAssociatedEndDevices().add(name);
							  TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(name,device.getName());
						}  else { List<AppModule>placedModules =
								  getDeviceToModuleMap().get(device.getId()); 
						          placedModules.add(appModule);
								  getDeviceToModuleMap().put(device.getId(), placedModules);
								  device.getAssociatedEndDevices().add(name);
								  TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(name,device.getName()); 
								  }
						
					}
					}
							  	 
				}
			}
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

