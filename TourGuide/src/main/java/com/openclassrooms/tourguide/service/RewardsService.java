package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	private final ExecutorService es = Executors.newCachedThreadPool();
	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public void calculateRewardsForAllUsers(List<User> users) throws ExecutionException, InterruptedException {
		List<CompletableFuture<Void>> futures = users.stream()
				.map(user -> CompletableFuture.runAsync(() -> calculateRewards(user), es))
				.toList();
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
	}

	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();

		List<VisitedLocation> userLocationsCopy = new CopyOnWriteArrayList<>(userLocations);

		for (VisitedLocation visitedLocation : userLocationsCopy) {
			for (Attraction attraction : attractions) {
				if (isUserRewardExist(user, attraction) && nearAttraction(visitedLocation, attraction)) {
						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					}
			}
		}
//		---------------------------------------------------------------------

//		ConcurrentHashMap<String, Boolean> rewardsCache = new ConcurrentHashMap<>();
//
//		userLocationsCopy.parallelStream().forEach(visitedLocation -> {
//			attractions.parallelStream().forEach(attraction -> {
//				String rewardKey = user.getUserId() + "_" + attraction.attractionId;
//				if (!rewardsCache.containsKey(rewardKey) && nearAttraction(visitedLocation, attraction)) {
//					user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//					rewardsCache.put(rewardKey, true);
//				}
//			});
//		});

//		---------------------------------------------------------------------

//		for (VisitedLocation visitedLocation : userLocationsCopy) {
//			futures.add(CompletableFuture.runAsync(() -> {
//				for (Attraction attraction : attractions) {
//					if (user.getUserRewards().stream()
//							.noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))
//							&& nearAttraction(visitedLocation, attraction)) {
//						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//					}
//				}
//			}));
//		}
//		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

//		---------------------------------------------------------------------

//		userLocationsCopy.parallelStream().forEach(visitedLocation -> {
//			attractions.parallelStream().forEach(attraction -> {
//				if (user.getUserRewards().stream().noneMatch(r ->
//						r.attraction.attractionName.equals(attraction.attractionName)) && nearAttraction(visitedLocation, attraction)) {
//					user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//				}
//			});
//		});

//		---------------------------------------------------------------------

//		for(VisitedLocation visitedLocation : userLocations) {
//			for(Attraction attraction : attractions) {
//				if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
//					if(nearAttraction(visitedLocation, attraction)) {
//						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//					}
//				}
//			}
//		}
	}
	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	private int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	private boolean isUserRewardExist(User user, Attraction attraction) {
		return user.getUserRewards()
				.stream()
				.noneMatch(r ->
						r.attraction.attractionName.equals(attraction.attractionName));
	}

	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}
}
