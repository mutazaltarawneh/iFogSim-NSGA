package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.MyApplication;
import org.fog.entities.FogDevice;
import org.fog.entities.MyActuator;
import org.fog.entities.MyFogDevice;
import org.fog.entities.MySensor;
import org.fog.utils.TimeKeeper;

public class MyModulePlacementCloudOnly extends MyPlacement {
	
	protected ModuleMapping moduleMapping;
	protected List<MySensor> sensors;
	protected List<MyActuator> actuators;
	protected String moduleToPlace;
	protected Map<Integer, Integer> deviceMipsInfo;
	
	
	
	public MyModulePlacementCloudOnly(List<MyFogDevice> fogDevices, List<MySensor> sensors, List<MyActuator> actuators, 
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
		
		for(MyFogDevice device:getMyFogDevices())
		{
			int deviceParent = -1;
			int cloudDv=-1;
			List<Integer>children = new ArrayList<Integer>();
			
			if(device.getLevel()==2)
			{
				if(!deviceMipsInfo.containsKey(device.getId()))
					deviceMipsInfo.put(device.getId(), 0);
				deviceParent = device.getParentId();
				
				cloudDv = CloudSim.getEntityId("cloud");
				
				MyFogDevice dv = (MyFogDevice) CloudSim.getEntity(cloudDv);
				
				for(MyFogDevice deviceChild:getMyFogDevices())
				{
					if(deviceChild.getParentId()==device.getId())
					{
						children.add(deviceChild.getId());
					}
				}
				
				Map<Integer, Double>childDeadline = new HashMap<Integer, Double>();
				for(int childId:children)
					childDeadline.put(childId,getMyApplication().getDeadlineInfo().get(childId).get(moduleToPlace));

				List<Integer> keys = new ArrayList<Integer>(childDeadline.keySet());
				
				//System.out.println(keys);
				
				for(int i=0;i<keys.size();i++)
				{
					
					    AppModule appModule = getMyApplication().getModuleByName(moduleToPlace); 
					
					
					//System.out.println(device.getName() + "    "+ device.getMips());
					
						List<AppModule>placedModules = getDeviceToModuleMap().get(cloudDv);
						placedModules.add(appModule);
						getDeviceToModuleMap().put(cloudDv, placedModules);
						dv.getAssociatedEndDevices().add(CloudSim.getEntityName(keys.get(i)));
						TimeKeeper.getInstance().getEndDeviceToDeviceMap().put(CloudSim.getEntityName(keys.get(i)), dv.getName());
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
}
