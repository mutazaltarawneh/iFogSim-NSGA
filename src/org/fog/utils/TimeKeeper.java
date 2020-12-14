package org.fog.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.Tuple;

public class TimeKeeper {

	private static TimeKeeper instance;
	
	private long simulationStartTime;
	private int count; 
	private Map<Integer, Double> emitTimes;
	private Map<Integer, Double> endTimes;
	private Map<Integer, List<Integer>> loopIdToTupleIds;
	private Map<Integer, Double> tupleIdToCpuStartTime;
	private Map<String, Double> tupleTypeToAverageCpuTime;
	private Map<String, Integer> tupleTypeToExecutedTupleCount;
	
	private Map<Integer, Double> loopIdToCurrentAverage;
	private Map<Integer, Integer> loopIdToCurrentNum;
	
	private Map<Integer,String> tupleIdToDeviceMap;
	
	private Map<String,Double> endDeviceToDelayMap;
	private Map<String,Integer> endDeviceRetiredTuplesCount;
	private Map<String,String> endDeviceToDeviceMap;
	
	
	private Map<String,Double> coreDeviceviceToDelayMap;
	private Map<String,Integer> coreDeviceRetiredTuplesCount;
	
	
	private Map<String, Double> critTypeToAverageResponseTime;
	private Map<String, Integer> critTypeToExecutedTupleCount;
	
	private Map<Integer,Integer> tupleIdToCriticalityMap;
	
	private Map<String,Double> secTypeToExecuteTupleCount;
	
	
	Map<String,Integer> currentDeviceCriticality;
	
	public static TimeKeeper getInstance(){
		if(instance == null)
			instance = new TimeKeeper();
		return instance;
	}
	
	public int getUniqueId(){
		return count++;
	}
	
	public void tupleStartedExecution(Tuple tuple){
		tupleIdToCpuStartTime.put(tuple.getCloudletId(), CloudSim.clock());
	}
	
	public void tupleEndedExecution(Tuple tuple){
		if(!tupleIdToCpuStartTime.containsKey(tuple.getCloudletId()))
			return;
		double executionTime = CloudSim.clock() - tupleIdToCpuStartTime.get(tuple.getCloudletId());
		if(!tupleTypeToAverageCpuTime.containsKey(tuple.getTupleType())){
			tupleTypeToAverageCpuTime.put(tuple.getTupleType(), executionTime);
			tupleTypeToExecutedTupleCount.put(tuple.getTupleType(), 1);
		} else{
			double currentAverage = tupleTypeToAverageCpuTime.get(tuple.getTupleType());
			int currentCount = tupleTypeToExecutedTupleCount.get(tuple.getTupleType());
			tupleTypeToAverageCpuTime.put(tuple.getTupleType(), (currentAverage*currentCount+executionTime)/(currentCount+1));
		}
	}
	
	public Map<Integer, List<Integer>> loopIdToTupleIds(){
		return getInstance().getLoopIdToTupleIds();
	}
	
	private TimeKeeper(){
		count = 1;
		setEmitTimes(new HashMap<Integer, Double>());
		setEndTimes(new HashMap<Integer, Double>());
		setLoopIdToTupleIds(new HashMap<Integer, List<Integer>>());
		setTupleTypeToAverageCpuTime(new HashMap<String, Double>());
		setTupleTypeToExecutedTupleCount(new HashMap<String, Integer>());
		setTupleIdToCpuStartTime(new HashMap<Integer, Double>());
		setLoopIdToCurrentAverage(new HashMap<Integer, Double>());
		setLoopIdToCurrentNum(new HashMap<Integer, Integer>());
		setTupleIdToDeviceMap(new HashMap<Integer,String>());
		
		setEndDeviceToDelayMap(new HashMap<String,Double>());
		setCoreDeviceToDelayMap(new HashMap<String,Double>());
		setCoreDeviceNumRetiredTuples(new HashMap<String,Integer>());
		
		
		setEndDeviceNumRetiredTuples(new HashMap<String,Integer>());
		setEndDeviceToDeviceMap(new HashMap<String,String>());
		
		setCritTypleToTupleCount(new HashMap<String,Integer>());
		setCritTypleToAverageResponseTime(new HashMap<String,Double>());
		
		setTupleIdToCriticalityMap(new HashMap<Integer,Integer>());
		
		setCurrentDeviceCriticality(new HashMap<String,Integer>());
		setSecTypeToExecutedTupleCount(new HashMap<String,Double>());
		
	}
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Map<Integer, Double> getEmitTimes() {
		return emitTimes;
	}

	public void setEmitTimes(Map<Integer, Double> emitTimes) {
		this.emitTimes = emitTimes;
	}

	public Map<Integer, Double> getEndTimes() {
		return endTimes;
	}

	public void setEndTimes(Map<Integer, Double> endTimes) {
		this.endTimes = endTimes;
	}

	public Map<Integer, List<Integer>> getLoopIdToTupleIds() {
		return loopIdToTupleIds;
	}

	public void setLoopIdToTupleIds(Map<Integer, List<Integer>> loopIdToTupleIds) {
		this.loopIdToTupleIds = loopIdToTupleIds;
	}

	public Map<String, Double> getTupleTypeToAverageCpuTime() {
		return tupleTypeToAverageCpuTime;
	}

	public void setTupleTypeToAverageCpuTime(
			Map<String, Double> tupleTypeToAverageCpuTime) {
		this.tupleTypeToAverageCpuTime = tupleTypeToAverageCpuTime;
	}

	public Map<String, Integer> getTupleTypeToExecutedTupleCount() {
		return tupleTypeToExecutedTupleCount;
	}

	public void setTupleTypeToExecutedTupleCount(
			Map<String, Integer> tupleTypeToExecutedTupleCount) {
		this.tupleTypeToExecutedTupleCount = tupleTypeToExecutedTupleCount;
	}

	public Map<Integer, Double> getTupleIdToCpuStartTime() {
		return tupleIdToCpuStartTime;
	}

	public void setTupleIdToCpuStartTime(Map<Integer, Double> tupleIdToCpuStartTime) {
		this.tupleIdToCpuStartTime = tupleIdToCpuStartTime;
	}

	public long getSimulationStartTime() {
		return simulationStartTime;
	}

	public void setSimulationStartTime(long simulationStartTime) {
		this.simulationStartTime = simulationStartTime;
	}

	public Map<Integer, Double> getLoopIdToCurrentAverage() {
		return loopIdToCurrentAverage;
	}

	public void setLoopIdToCurrentAverage(Map<Integer, Double> loopIdToCurrentAverage) {
		this.loopIdToCurrentAverage = loopIdToCurrentAverage;
	}

	public Map<Integer, Integer> getLoopIdToCurrentNum() {
		return loopIdToCurrentNum;
	}

	public void setLoopIdToCurrentNum(Map<Integer, Integer> loopIdToCurrentNum) {
		this.loopIdToCurrentNum = loopIdToCurrentNum;
	}
	
	public void setTupleIdToDeviceMap(Map<Integer,String> mp)
	{
		this.tupleIdToDeviceMap = mp;
	}
	
	public Map<Integer,String> getTupleIdToDeviceMap()
	{
		return this.tupleIdToDeviceMap;
	}
	
	
	
	public void setEndDeviceToDelayMap(Map<String,Double> mp)
	{
		this.endDeviceToDelayMap = mp;
	}
	
	public Map<String,Double> getEndDeviceToDelayMap()
	{
		return this.endDeviceToDelayMap;
	}
	
	
	public void setCoreDeviceToDelayMap(Map<String,Double> mp)
	{
		this.coreDeviceviceToDelayMap = mp;
	}
	
	public Map<String,Double> getCoreDeviceToDelayMap()
	{
		return this.coreDeviceviceToDelayMap;
	}
	
	public void setCoreDeviceNumRetiredTuples(Map<String,Integer> mp)
	{
		this.coreDeviceRetiredTuplesCount = mp;
	}
	
	public Map<String,Integer> getCoreDeviceNumRetiredTuples()
	{
		return this.coreDeviceRetiredTuplesCount;
	}
	
	
	
	public void setEndDeviceNumRetiredTuples(Map<String,Integer> mp)
	{
		this.endDeviceRetiredTuplesCount = mp;
	}
	
	public Map<String,Integer> getEndDeviceNumRetiredTuples()
	{
		return this.endDeviceRetiredTuplesCount;
	}
	
	public void setEndDeviceToDeviceMap(Map<String,String> mp)
	{
		this.endDeviceToDeviceMap = mp;
	}
	
	public Map<String,String> getEndDeviceToDeviceMap()
	{
		return this.endDeviceToDeviceMap;
	}
	
	public void setCritTypleToAverageResponseTime(Map<String,Double> mp)
	{
		this.critTypeToAverageResponseTime = mp;
	}
	
	public void setCritTypleToTupleCount(Map<String,Integer> mp)
	{
		this.critTypeToExecutedTupleCount = mp;
	}
	
	public Map<String,Double> getCritTypleToAverageResponseTime()
	{
		return this.critTypeToAverageResponseTime ;
	}
	
	public Map<String,Integer> getCritTypleToTupleCount()
	{
		return this.critTypeToExecutedTupleCount;
	}
	
	public void setTupleIdToCriticalityMap(Map<Integer,Integer> mp)
	{
		this.tupleIdToCriticalityMap = mp;
	}
	
	public Map<Integer,Integer> getTupleIdToCriticalityMap()
	{
		return this.tupleIdToCriticalityMap;
	}
	
	
	public void setCurrentDeviceCriticality(Map<String,Integer> mp)
	{
		this.currentDeviceCriticality = mp;
	}
	
	public Map<String,Integer> getCurrentDeviceCriticality()
	{
		return this.currentDeviceCriticality;
	}
	
	public void setSecTypeToExecutedTupleCount(Map<String,Double> mp)
	{
		this.secTypeToExecuteTupleCount = mp;
	}
	
	public Map<String,Double> getSecTypleToExecutedTupleCount()
	{
		return this.secTypeToExecuteTupleCount;
	}
	
	
	
	
}
