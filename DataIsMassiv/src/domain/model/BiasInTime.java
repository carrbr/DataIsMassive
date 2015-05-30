package domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import domain.AVGPair;
import domain.Rating;

public abstract class BiasInTime implements DelatAccess, Serializable {
	private static final long serialVersionUID = 1L;

	private final HashMap<Integer, AVGPair> baseBias;
	private final HashMap<Integer, HashMap<Integer, Double>> timeOffset;
	private final HashMap<Integer, Integer> maxDay;
	private final HashMap<Integer, Integer> minDay;
	private final HashMap<Integer, Double> overAllTimeOffset;
	private int oMaxDay = Integer.MIN_VALUE, oMinDay = Integer.MAX_VALUE;
	private final int daysPerBucket = 7 * 8;

	public BiasInTime() {
		baseBias = new HashMap<>();
		timeOffset = new HashMap<>();
		maxDay = new HashMap<>();
		minDay = new HashMap<>();
		overAllTimeOffset = new HashMap<>();
	}

	public void train(List<Rating> toTrain, BaseLearner base) {
		calculateBaseBias(toTrain, base);
		calculateTimeDependentBias(toTrain, base);
		// check out examples
		Random r = new Random();

		for (int j = 0; j < 5; j++) {
			int i = r.nextInt(toTrain.size());
			int itemId = getItemId(toTrain.get(i));
			int maxDayItem = maxDay.get(itemId);
			int minDayItem = minDay.get(itemId);
			Double y0 = timeOffset.get(itemId).get(minDayItem / daysPerBucket);
			Double y1 = timeOffset.get(itemId).get(maxDayItem / daysPerBucket);
			double m = (maxDayItem - minDayItem) == 0 ? 0 : (y1 - y0)
					/ (maxDayItem - minDayItem);
			System.out.println("Item " + itemId + " has m=" + m + " and y0="
					+ y0);
		}
	}

	private void calculateBaseBias(List<Rating> toTrain, BaseLearner base) {
		for (Rating r : toTrain) {
			checkSetMinMax(r);

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
		HashMap<Integer, HashMap<Integer, AVGPair>> timeOffsetTemp = new HashMap<>();
		HashMap<Integer, AVGPair> overAllTimeOffsetTemp = new HashMap<>();

		calculateAVGInTime(toTrain, base, timeOffsetTemp, overAllTimeOffsetTemp);
		copyAVGInTimeToOffsetTime(timeOffsetTemp, overAllTimeOffsetTemp);
	}

	private void calculateAVGInTime(List<Rating> toTrain, BaseLearner base,
			HashMap<Integer, HashMap<Integer, AVGPair>> timeOffsetTemp,
			HashMap<Integer, AVGPair> overAllTimeOffsetTemp) {

		for (Rating r : toTrain) {
			double offset = r.getRating() - base.getDelta(r)
					- baseBias.get(getItemId(r)).getAVG();

			HashMap<Integer, AVGPair> itemSpecificTimeBiasMap = timeOffsetTemp
					.get(getItemId(r));
			if (itemSpecificTimeBiasMap == null) {
				itemSpecificTimeBiasMap = new HashMap<>();
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
			HashMap<Integer, HashMap<Integer, AVGPair>> timeOffsetTemp,
			HashMap<Integer, AVGPair> overAllTimeOffsetTemp) {

		for (Map.Entry<Integer, HashMap<Integer, AVGPair>> itemBuckets : timeOffsetTemp
				.entrySet()) {

			HashMap<Integer, Double> finalBucket = new HashMap<>();
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

	private void checkSetMinMax(Rating r) {
		checkSetMax(r);
		checkSetMin(r);
	}

	private void checkSetMin(Rating r) {
		if (r.getDateId() < oMinDay)
			oMinDay = r.getDateId();

		if (!minDay.containsKey(getItemId(r))) {
			minDay.put(getItemId(r), r.getDateId());
		} else {
			int currentMax = minDay.get(getItemId(r));
			if (r.getDateId() < currentMax) {
				minDay.put(getItemId(r), r.getDateId());
			}
		}
	}

	private void checkSetMax(Rating r) {
		if (r.getDateId() > oMaxDay)
			oMaxDay = r.getDateId();

		if (!maxDay.containsKey(getItemId(r))) {
			maxDay.put(getItemId(r), r.getDateId());
		} else {
			int currentMax = maxDay.get(getItemId(r));
			if (r.getDateId() > currentMax) {
				maxDay.put(getItemId(r), r.getDateId());
			}
		}
	}

	private int getMin(Rating r) {
		if (!minDay.containsKey(getItemId(r))) {
			minDay.put(getItemId(r), r.getDateId());
			return r.getDateId();
		} else
			return minDay.get(r.getDateId());
	}

	private int getMax(Rating r) {
		if (!maxDay.containsKey(getItemId(r))) {
			maxDay.put(getItemId(r), r.getDateId());
			return r.getDateId();
		} else
			return maxDay.get(r.getDateId());
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

	private double getAvgOfTimeBuket(int movieId, int day) {
		HashMap<Integer, Double> hm = timeOffset.get(movieId);
		if (hm == null) {
			if (oMinDay <= day && day <= oMaxDay)
				return overAllTimeOffset.get(day / daysPerBucket);
			if (day < oMinDay)
				return overAllTimeOffset.get(oMinDay / daysPerBucket);
			if (day > oMaxDay)
				return overAllTimeOffset.get(oMaxDay / daysPerBucket);
		}
		Double d = hm.get(day / daysPerBucket);
		if (d == null)
			return hm.get(maxDay.get(movieId) / daysPerBucket);
		return d;
	}

	private double calculateTimeOffset(Rating rating) {
		int currentDay = rating.getDateId();
		int max = getMax(rating);
		int min = getMin(rating);
		if (min <= currentDay && currentDay <= max)
			return calculateMiddleOffset(rating);
		if (min > currentDay)
			return getAvgOfTimeBuket(getItemId(rating), min);

		return calculateFutureOffset(rating);
	}

	private double calculateFutureOffset(Rating rating) {
		int max = getMax(rating);
		// TODO triangulate linear
		return getAvgOfTimeBuket(getItemId(rating), max);
	}

	private double calculateMiddleOffset(Rating rating) {
		int day = rating.getDateId();
		int xCurrent = (day / daysPerBucket) * daysPerBucket + daysPerBucket
				/ 2;
		double yCurrent = getAvgOfTimeBuket(getItemId(rating), day);

		int xNext = xCurrent + daysPerBucket;
		double yNext = getAvgOfTimeBuket(getItemId(rating), xNext);

		return (yNext - yCurrent) / daysPerBucket * (day - xCurrent) + yCurrent;
	}

}
