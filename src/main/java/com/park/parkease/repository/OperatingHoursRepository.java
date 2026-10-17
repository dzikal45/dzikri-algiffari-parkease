package com.park.parkease.repository;

import com.park.parkease.domain.entities.OperatingHours;

import java.util.Collection;

public interface OperatingHoursRepository {
    Collection<OperatingHours> findAll();
}
