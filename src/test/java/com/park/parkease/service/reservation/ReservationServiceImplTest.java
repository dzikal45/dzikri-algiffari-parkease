package com.park.parkease.service.reservation;

import com.park.parkease.domain.entities.OperatingHours;
import com.park.parkease.domain.entities.ParkingLot;
import com.park.parkease.domain.entities.ParkingSlot;
import com.park.parkease.domain.entities.ParkingInvoice;
import com.park.parkease.domain.entities.Reservation;
import com.park.parkease.domain.enums.ReservationStatus;
import com.park.parkease.domain.enums.SlotStatus;
import com.park.parkease.domain.enums.VehicleType;
import com.park.parkease.exception.ValidationException;
import com.park.parkease.repository.ParkingInvoiceRepository;
import com.park.parkease.repository.ParkingLotRepository;
import com.park.parkease.repository.ParkingLotRepositoryService;
import com.park.parkease.repository.RateCardRepository;
import com.park.parkease.repository.ReservationRepository;
import com.park.parkease.repository.ReservationRepositoryService;
import com.park.parkease.service.billing.BillingService;
import com.park.parkease.service.cache.CacheService;
import com.park.parkease.service.cache.CacheServiceImpl;
import com.park.parkease.service.parkingslot.ParkingSlotService;
import com.park.parkease.service.parkingslot.ParkingSlotServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceImplTest {
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final RateCardRepository rateCardRepository = mock(RateCardRepository.class);
    private final ParkingInvoiceRepository parkingInvoiceRepository = mock(ParkingInvoiceRepository.class);
    private final ParkingLotRepository parkingLotRepository = mock(ParkingLotRepository.class);
    private final BillingService billingService = mock(BillingService.class);
    @SuppressWarnings("unchecked")
    private final CacheService<Object, Object> cacheService = mock(CacheService.class);
    private final ParkingSlotService parkingSlotService = mock(ParkingSlotService.class);
    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationServiceImpl(
                reservationRepository, rateCardRepository, parkingInvoiceRepository, parkingLotRepository,
                billingService, cacheService, parkingSlotService);
        ReflectionTestUtils.setField(service, "doubleRequestTtl", Duration.ofMinutes(5));
        ReflectionTestUtils.setField(service, "noShowFee", new BigDecimal("5000.00"));
        when(cacheService.doubleRequest(anyString(), any(Duration.class))).thenReturn(false);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get())
                .when(cacheService).lockRequest(anyString(), any());
    }

    @Test
    void createReservation_withOpenEntryTime_savesPendingReservationInAvailableSlot() {
        Reservation reservation = reservation(nextWednesdayAt(21, 0));
        when(parkingLotRepository.findById("LOT-JKT01")).thenReturn(Optional.of(openLot()));
        when(parkingSlotService.findAvailableSlot(anyString(), any(), any(), any()))
                .thenReturn(Optional.of(ParkingSlot.builder().slotId("CAR-F1-001").build()));

        Reservation result = service.createReservation(reservation);

        assertThat(result.getReservationId()).isNotBlank();
        assertThat(result.getSlotId()).isEqualTo("CAR-F1-001");
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void checkIn_afterClosingTime_rejectsEntryWithoutSavingReservation() {
        LocalDateTime plannedStartTime = nextWednesdayAt(21, 30);
        Reservation reservation = reservation(plannedStartTime);
        reservation.setReservationId("reservation-1");
        reservation.setStatus(ReservationStatus.PENDING);
        when(reservationRepository.findById("reservation-1")).thenReturn(Optional.of(reservation));
        when(parkingLotRepository.findById("LOT-JKT01")).thenReturn(Optional.of(openLot()));

        assertThatThrownBy(() -> service.checkIn("reservation-1", plannedStartTime.withHour(22).withMinute(1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Parking lot is closed at the requested entry time");

        verify(reservationRepository, never()).save(reservation);
    }

    @Test
    void createReservation_withConcurrentRequestsForOneSlot_savesOnlyOneReservation() throws Exception {
        ParkingLotRepositoryService realLotRepository = new ParkingLotRepositoryService();
        ReservationRepositoryService realReservationRepository = new ReservationRepositoryService();
        CacheService<Object, Object> realCacheService = new CacheServiceImpl<>();
        ParkingSlotService realParkingSlotService = new ParkingSlotServiceImpl(
                realLotRepository, realReservationRepository);
        ReservationService concurrentService = new ReservationServiceImpl(
                realReservationRepository, rateCardRepository, parkingInvoiceRepository, realLotRepository,
                billingService, realCacheService, realParkingSlotService);
        ReflectionTestUtils.setField(concurrentService, "doubleRequestTtl", Duration.ofMinutes(5));
        realLotRepository.save(ParkingLot.builder()
                .lotId("LOT-JKT01")
                .slots(Map.of(VehicleType.CAR, java.util.List.of(ParkingSlot.builder()
                        .slotId("CAR-F1-001")
                        .vehicleType(VehicleType.CAR)
                        .floorLevel(1)
                        .status(SlotStatus.AVAILABLE)
                        .build())))
                .operatingHours(openLot().getOperatingHours())
                .build());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            LocalDateTime plannedStartTime = nextWednesdayAt(21, 0);
            Future<Boolean> first = executor.submit(() -> createAfterStart(
                    concurrentService, reservation(plannedStartTime, "customer-1", "B1234XYZ"), ready, start));
            Future<Boolean> second = executor.submit(() -> createAfterStart(
                    concurrentService, reservation(plannedStartTime, "customer-2", "B5678XYZ"), ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(java.util.List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(realReservationRepository.findAll()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void createReservation_withPastPlannedStartTime_rejectsRequest() {
        Reservation reservation = reservation(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> service.createReservation(reservation))
                .isInstanceOf(ValidationException.class)
                .hasMessage("plannedStartTime must be today or in the future");
    }

    @Test
    void processNoShows_forExpiredPendingReservation_createsFeeInvoiceOnce() {
        Reservation reservation = reservation(LocalDateTime.now().minusMinutes(31));
        reservation.setReservationId("reservation-1");
        reservation.setStatus(ReservationStatus.PENDING);
        ParkingInvoice invoice = ParkingInvoice.builder().invoiceId("invoice-1").build();
        when(reservationRepository.findAll()).thenReturn(List.of(reservation));
        when(reservationRepository.findById("reservation-1")).thenReturn(Optional.of(reservation));
        when(parkingInvoiceRepository.findByReservationId("reservation-1")).thenReturn(Optional.empty());
        when(billingService.generateNoShowInvoice(reservation, new BigDecimal("5000.00"))).thenReturn(invoice);

        service.processNoShows(LocalDateTime.now());
        service.processNoShows(LocalDateTime.now());

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
        verify(billingService).generateNoShowInvoice(reservation, new BigDecimal("5000.00"));
        verify(parkingInvoiceRepository).save(invoice);
    }

    @Test
    void processNoShows_forActiveReservation_doesNotCreateInvoice() {
        Reservation reservation = reservation(LocalDateTime.now().minusHours(1));
        reservation.setReservationId("reservation-1");
        reservation.setStatus(ReservationStatus.ACTIVE);
        when(reservationRepository.findAll()).thenReturn(List.of(reservation));

        service.processNoShows(LocalDateTime.now());

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        verify(billingService, never()).generateNoShowInvoice(any(), any());
    }

    @Test
    void cancelReservation_afterCheckInDeadline_marksNoShowAndRejectsCancellation() {
        Reservation reservation = reservation(LocalDateTime.now().minusMinutes(31));
        reservation.setReservationId("reservation-1");
        reservation.setStatus(ReservationStatus.PENDING);
        ParkingInvoice invoice = ParkingInvoice.builder().invoiceId("invoice-1").build();
        when(reservationRepository.findById("reservation-1")).thenReturn(Optional.of(reservation));
        when(parkingInvoiceRepository.findByReservationId("reservation-1")).thenReturn(Optional.empty());
        when(billingService.generateNoShowInvoice(reservation, new BigDecimal("5000.00"))).thenReturn(invoice);

        assertThatThrownBy(() -> service.cancelReservation("reservation-1", "change of plan"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Reservation can no longer be cancelled");

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
        verify(parkingInvoiceRepository).save(invoice);
    }

    private Reservation reservation(LocalDateTime plannedStartTime) {
        return reservation(plannedStartTime, "customer-1", "B1234XYZ");
    }

    private Reservation reservation(LocalDateTime plannedStartTime, String customerId, String licensePlate) {
        return Reservation.builder()
                .lotId("LOT-JKT01")
                .vehicleType(VehicleType.CAR)
                .licensePlate(licensePlate)
                .customerId(customerId)
                .plannedStartTime(plannedStartTime)
                .plannedEndTime(plannedStartTime.plusHours(2))
                .build();
    }

    private boolean createAfterStart(ReservationService concurrentService, Reservation reservation,
                                     CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            concurrentService.createReservation(reservation);
            return true;
        } catch (ValidationException exception) {
            return false;
        }
    }

    private LocalDateTime nextWednesdayAt(int hour, int minute) {
        return LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY)).atTime(hour, minute);
    }

    private ParkingLot openLot() {
        return ParkingLot.builder().lotId("LOT-JKT01")
                .operatingHours(OperatingHours.builder()
                        .hoursByDay(Map.of(DayOfWeek.WEDNESDAY, OperatingHours.DailyHours.builder()
                                .openTime(LocalTime.of(6, 0))
                                .closeTime(LocalTime.of(22, 0))
                                .build()))
                        .build())
                .build();
    }
}
