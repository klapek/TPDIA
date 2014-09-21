package polsl.tpdia;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.lang.model.type.PrimitiveType;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math.stat.descriptive.summary.Sum;

import polsl.tpdia.models.AggregatedNozzleData;
import polsl.tpdia.models.RawPrimaryData;


public class StreamDataGenerator {
	public Long deviationTimeToNextClient = 60l;
	public Long meanTimeToNextClient = 60l;
	public Long deviationAmountToTank = 15l;
	public Long meanAmountToTank = 35l;
	public Double msTimeFor1l = 1800d; 	// 1l/1.8s
	public Integer interval = 5000; // 5s
	public int aggregationInterval = 900000; // 15min
	public Long minuteToMs = 60000l;
	public double oneLtoFuelHeightProportion = 0.000379552d;
	public int tankId = 9650;
	public int nozzleId = 1640;
	
	
	
	public StreamDataGenerator(){
		
	}

	public Double getRandValueToTankInL(Random randomGenerator) {
		return randomGenerator.nextGaussian() * deviationAmountToTank
				+ meanAmountToTank;
	}

	public Double nextTankInMs(Random randomGenerator) {
		return (randomGenerator.nextGaussian() * deviationTimeToNextClient
				+ meanTimeToNextClient) * minuteToMs;
	}
	
	
	public double getWaterHeight(Random randomGenerator) {
		double returnValue =  Math.max(randomGenerator.nextGaussian() -4.4 ,0)/100;
		if (returnValue>0){
			System.out.println(returnValue);
		}
		return returnValue;
	}
	
	
	public ArrayList<RawPrimaryData> Generate(Calendar dateFrom, Calendar dateTo){
		Calendar startingPoint = (Calendar) dateFrom.clone();
		Random randomGenerator = new Random();
//		Map<Date,Double> dict =  new HashMap<Date, Double>();
		ArrayList<Double> values = new ArrayList<Double>();
		ArrayList<RawPrimaryData> rawData = new ArrayList<RawPrimaryData>();
		

		Double amountOfFuelInTick= interval/msTimeFor1l;
		Double valueToTank = 0d;
		 //Diff of time of period to generate divided by interval that data are taken
		Long ticksAmount = (dateTo.getTimeInMillis() - startingPoint.getTimeInMillis())
				/ interval;
		Double nextTank = nextTankInMs(randomGenerator);
		
		for (Integer i = 0; i < ticksAmount; i++, nextTank -= interval, startingPoint.add(Calendar.MILLISECOND, interval)) {
			Date pointInTime = startingPoint.getTime();
			
			if (nextTank <= 0 && valueToTank<=0) {
				valueToTank = Math.max(0,getRandValueToTankInL(randomGenerator));
				nextTank = nextTankInMs(randomGenerator);
			}
			
			
			double tankedValue = Math.min(amountOfFuelInTick, valueToTank);
			valueToTank-= tankedValue;
			
//			System.out.println(tankedValue + " \t" + pointInTime);
//			dict.put(pointInTime, tankedValue);
			values.add(tankedValue);
			RawPrimaryData rawPoint = new RawPrimaryData(tankId,nozzleId, pointInTime,tankedValue,tankedValue*oneLtoFuelHeightProportion,getWaterHeight(randomGenerator));
			
			rawData.add(rawPoint);
		}
		
		
		for(RawPrimaryData data : rawData){
			System.out.println(data.RawFuelNozzleAmount  + " " + data.PointInTime + " " + data.RawFuelVolume +  " "  + data.RawWaterVolume);
		}
		
		
		double[] calculatedTankedValues = ArrayUtils.toPrimitive(values.toArray(new Double[values.size()]));

		
		int aggergationStep = (int) (aggregationInterval/interval);
		
		NozzleDataHandler nozzleDataHandler = new NozzleDataHandler(aggregationInterval);
		
		List<AggregatedNozzleData> aggregatedData = nozzleDataHandler.aggregateNozzleData(aggergationStep,calculatedTankedValues,(Calendar)dateFrom.clone(),nozzleId,tankId,5d);
		
		for(AggregatedNozzleData data : aggregatedData){
			data.printToConsole();
		}
		
		return rawData;
	}

	
}
