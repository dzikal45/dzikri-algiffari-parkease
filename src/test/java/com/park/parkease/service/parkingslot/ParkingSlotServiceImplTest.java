package com.park.parkease.service.parkingslot;

import com.park.parkease.domain.entities.ParkingLot;
import com.park.parkease.domain.entities.ParkingSlot;
import com.park.parkease.domain.entities.Reservation;
import com.park.parkease.domain.enums.ReservationStatus;
import com.park.parkease.domain.enums.SlotStatus;
import com.park.parkease.domain.enums.VehicleType;
import com.park.parkease.exception.ValidationException;
import com.park.parkease.repository.ParkingLotRepository;
import com.park.parkease.repository.ReservationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParkingSlotServiceImplTest {
    private final ParkingLotRepository parkingLotRepository = mock(ParkingLotRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final ParkingSlotService service = new ParkingSlotServiceImpl(parkingLotRepository, reservationRepository);

    @Test
    void findAvailableSlots_sortsAvailableSlotsByFloorThenSlotId() {
        ParkingSlot floorTwo = slot("CAR-F2-001", 2);
        ParkingSlot floorOneLaterId = slot("CAR-F1-002", 1);
        ParkingSlot floorOneFirstId = slot("CAR-F1-001", 1);
        when(parkingLotRepository.findById("LOT-JKT01")).thenReturn(Optional.of(lot(floorTwo, floorOneLaterId, floorOneFirstId)));
        when(reservationRepository.findAll()).thenReturn(List.of());

        List<ParkingSlot> availableSlots = service.findAvailableSlots(
                "LOT-JKT01", VehicleType.CAR,
                futureStartTime(),
                futureStartTime().plusHours(1));

        assertThat(availableSlots).extracting(ParkingSlot::getSlotId)
                .containsExactly("CAR-F1-001", "CAR-F1-002", "CAR-F2-001");
    }

    @Test
    void findAvailableSlots_excludesSlotWithOverlappingActiveReservation() {
        ParkingSlot slot = slot("CAR-F1-001", 1);
        when(parkingLotRepository.findById("LOT-JKT01")).thenReturn(Optional.of(lot(slot)));
        when(reservationRepository.findAll()).thenReturn(List.of(Reservation.builder()
                .reservationId("reservation-1")
                .lotId("LOT-JKT01")
                .slotId("CAR-F1-001")
                .status(ReservationStatus.ACTIVE)
                .plannedStartTime(futureStartTime())
                .plannedEndTime(futureStartTime().plusHours(1))
                .build()));

        List<ParkingSlot> availableSlots = service.findAvailableSlots(
                "LOT-JKT01", VehicleType.CAR,
                futureStartTime().plusMinutes(30),
                futureStartTime().plusHours(1).plusMinutes(30));

        assertThat(availableSlots).isEmpty();
    }

    @Test
    void findAvailableSlots_withPastStartTime_rejectsRequest() {
        assertThatThrownBy(() -> service.findAvailableSlots(
                "LOT-JKT01", VehicleType.CAR,
                LocalDateTime.now().minusDays(1), LocalDateTime.now()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("startTime must be today or in the future");
    }

    private LocalDateTime futureStartTime() {
        return LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
    }

    private ParkingLot lot(ParkingSlot... slots) {
        return ParkingLot.builder().lotId("LOT-JKT01").slots(Map.of(VehicleType.CAR, List.of(slots))).build();
    }

    private ParkingSlot slot(String slotId, int floorLevel) {
        return ParkingSlot.builder()
                .slotId(slotId)
                .vehicleType(VehicleType.CAR)
                .status(SlotStatus.AVAILABLE)
                .floorLevel(floorLevel)
                .build();
    }
}
