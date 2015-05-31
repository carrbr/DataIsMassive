package domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import domain.AVGPair;
import domain.Rating;

public abstract class BiasInTime implements DelatAccess, Serializable {
	private static final long serialVersionUID = 1L;

	private final HashMap<Integer, AVGPair> baseBias;
	private final HashMap<Integer, TreeMap<Integer, Double>> timeOffset;
	private final TreeMap<Integer, Double> overAllTimeOffset;
	private final int daysPerBucket = 7 * 8;

	public BiasInTime() {
		baseBias = new HashMap<>();
		timeOffset = new HashMap<>();
		overAllTimeOffset = new TreeMap<>();
	}

	public void train(List<Rating> toTrain, BaseLearner base) {

		calculateBaseBias(toTrain, base);
		calculateTimeDependentBias(toTrain, base);

		// check out examples
		Random r = new Random();
		for (int j = 0; j < 5; j++) {
			int i = r.nextInt(toTrain.size());
			int itemId = getItemId(toTrain.get(i));
			int maxDayItem = timeOffset.get(itemId).lastKey();
			int minDayItem = timeOffset.get(itemId).firstKey();
			Double y0 = timeOffset.get(itemId).get(minDayItem);
			Double y1 = timeOffset.get(itemId).get(maxDayItem);
			double m = (maxDayItem - minDayItem) == 0 ? 0 : (y1 - y0)
					/ ((maxDayItem - minDayItem)*daysPerBucket);
			System.out.println("Item " + itemId + " has m=" + m + " and y0="
					+ y0);
		}
	}

	private void calculateBaseBias(List<Rating> toTrain, BaseLearner base) {
		for (Rating r : toTrain) {

			AVGPair avg = baseBias.get(getItemId(r));
			if (avg == null) {
				avg = new AVGPair();
				baseBias.put(getItemId(r), avg);
			}
			avg.add(base.getDelta(r) - r.getRating());
		}
	}

	private void calculateTimeDependentBias(List<Rating> toTrain,
			BaseLearner base) {
		HashMap<Integer, TreeMap<Integer, AVGPair>> timeOffsetTemp = new HashMap<>();
		HashMap<Integer, AVGPair> overAllTimeOffsetTemp = new HashMap<>();

		calculateAVGInTime(toTrain, base, timeOffsetTemp, overAllTimeOffsetTemp);
		copyAVGInTimeToOffsetTime(timeOffsetTemp, overAllTimeOffsetTemp);
	}

	private void calculateAVGInTime(List<Rating> toTrain, BaseLearner base,
			HashMap<Integer, TreeMap<Integer, AVGPair>> timeOffsetTemp,
			HashMap<Integer, AVGPair> overAllTimeOffsetTemp) {

		for (Rating r : toTrain) {
			double offset = r.getRating() - base.getDelta(r)
					- baseBias.get(getItemId(r)).getAVG();

			TreeMap<Integer, AVGPair> itemSpecificTimeBiasMap = timeOffsetTemp
					.get(getItemId(r));
			if (itemSpecificTimeBiasMap == null) {
				itemSpecificTimeBiasMap = new TreeMap<>();
				timeOffsetTemp.put(getItemId(r), itemSpecificTimeBiasMap);
			}
			int bucketNo = r.getDateId() / daysPerBucket;
			AVGPair bucketBias = itemSpecificTimeBiasMap.get(bucketNo);
			if (bucketBias == null) {
				bucketBias = new AVGPair();
				itemSpecificTimeBiasMap.put(bucketNo, bucketBias);
			}

			bucketBias.add(offset);

			AVGPair bucketBiasOverall = overAllTimeOffsetTemp.get(bucketNo);
			if (bucketBiasOverall == null) {
				bucketBiasOverall = new AVGPair();
				overAllTimeOffsetTemp.put(bucketNo, bucketBias);
			}
			bucketBiasOverall.add(offset);
		}
	}

	private void copyAVGInTimeToOffsetTime(
			HashMap<Integer, TreeMap<Integer, AVGPair>> timeOffsetTemp,
			HashMap<Integer, AVGPair> overAllTimeOffsetTemp) {

		for (Map.Entry<Integer, TreeMap<Integer, AVGPair>> itemBuckets : timeOffsetTemp
				.entrySet()) {

			TreeMap<Integer, Double> finalBucket = new TreeMap<>();
			timeOffset.put(itemBuckets.getKey(), finalBucket);

			for (Map.Entry<Integer, AVGPair> bucket : itemBuckets.getValue()
					.entrySet()) {
				finalBucket.put(bucket.getKey(), bucket.getValue().getAVG());
			}
		}

		for (Map.Entry<Integer, AVGPair> bucket : overAllTimeOffsetTemp
				.entrySet()) {
			overAllTimeOffset.put(bucket.getKey(), bucket.getValue().getAVG());
		}

	}

	protected abstract int getItemId(Rating rating);

	@Override
	public double getDelta(Rating rating) {
		AVGPair avg = baseBias.get(getItemId(rating));
		if (avg == null) {
			avg = new AVGPair();
			baseBias.put(getItemId(rating), avg);
		}
		return avg.getAVG() + calculateTimeOffset(rating);
	}

	private double calculateTimeOffset(Rating rating) {
		TreeMap<Integer, Double> timedependance = timeOffset
				.get(getItemId(rating));
		if (timedependance == null)
			return findNearestIn(overAllTimeOffset, rating);
		return findNearestIn(timedependance, rating);
	}

	private double findNearestIn(TreeMap<Integer, Double> dayBucketToAVG,
			Rating rating) {
		int bucket = rating.getDateId() / daysPerBucket;
				
		if (dayBucketToAVG.containsKey(bucket)
				&& dayBucketToAVG.containsKey(bucket + 1)) {
			
			double fractionWithinBucket = (rating.getDateId() % daysPerBucket)
							/ (double) daysPerBucket;
			return triangulate(dayBucketToAVG.get(bucket),
					dayBucketToAVG.get(bucket + 1),
					fractionWithinBucket);
		}
		
		Integer floor = dayBucketToAVG.floorKey(bucket);
		Integer ceiling = dayBucketToAVG.ceilingKey(bucket);
		if(floor == null && ceiling == null)
			return 0;
		if(floor == null && ceiling != null)
			return dayBucketToAVG.get(ceiling);
		if(floor != null && ceiling == null)
			return dayBucketToAVG.get(floor);
		if(floor == ceiling)
			return dayBucketToAVG.get(floor);
		
		double currentBucket = dayBucketToAVG.get(floor);
		double nextBucket = dayBucketToAVG.get(ceiling);
		double fractionWithinBucket =  ((double)bucket-floor)/(ceiling-floor);
		return triangulate(currentBucket, nextBucket, fractionWithinBucket);
	}

	private double triangulate(double currentBucket, double nextBucket,
			double fractionWithinNextBucket) {
		return (currentBucket * (1 - fractionWithinNextBucket) + nextBucket
				* fractionWithinNextBucket);
	}

}
