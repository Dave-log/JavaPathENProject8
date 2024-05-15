package com.openclassrooms.tourguide.dto;

import java.util.List;

public record NearbyAttractionsDTO(
        List<NearbyAttraction> nearbyAttractionList,
        double userLatitude,
        double userLongitude
        )
{}
