package com.park.parkease.repository;

import com.park.parkease.domain.entities.OperatingHours;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OperatingHoursRepositoryService implements OperatingHoursRepository {
    private final Map<String, OperatingHours> operatingHours = new ConcurrentHashMap<>();

    @Override
    public Collection<OperatingHours> findAll() {
        return operatingHours.values();
    }
}
