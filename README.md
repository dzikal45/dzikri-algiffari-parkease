# Parkease

Parkease is an in-memory parking-lot reservation API. It supports configured lots and slots, availability lookup, reservation lifecycle actions, and checkout billing.

## Run
./mvnw spring-boot:run

## Test
./mvnw test


## Configuration

Configuration is in `src/main/resources/application.properties`.

- `parking.inventory.lots[...]` defines lots, per-floor slot capacities, and daily operating hours.
- `parking.reservation.double-request-ttl` controls duplicate-request suppression; the default is `5m`.
- `parking.reservation.no-show-fee` is the global fee invoiced when a pending reservation passes its check-in deadline; the default is `5000.00`.
- `parking.reservation.no-show-check-interval` controls the no-show scheduler delay in milliseconds; the default is `60000`.
- `parking.billing.hourly-rate-divisor` splits an hourly rate into billing blocks. The default `2` creates 30-minute blocks at half the hourly rate; `4` creates 15-minute blocks at one quarter of the hourly rate. The divisor must divide one hour evenly.

## Business-rule assumptions

- A reservation start and availability search must be today or later. A new reservation or check-in is allowed only during the lot's operating hours on the entry day. Parking may continue overnight and checkout is allowed after closing.
- A check-in within 30 minutes before or after the planned start becomes `ACTIVE`; an early check-in is rejected.
- A pending reservation is automatically marked `NO_SHOW` after the 30-minute check-in deadline and user receives the configured no-show invoice charge, and cannot then be cancelled. Cancellation is allowed only while the reservation is pending and before that deadline.
- Active and pending reservations prevent overlapping use of the same slot. Extension is allowed for pending or active reservations.
- Billing applies grace time once per stay, rounds each remaining daily segment up to a complete configured billing block, caps each calendar day, and charges the overnight surcharge once for every midnight crossing.
- Currency values use `BigDecimal` and `HALF_UP` rounding to two decimal places.
- parking billing hour late divisor can be configurable to allow for different billing block sizes (e.g., 15-minute blocks instead of 30-minute blocks). example divisor is 4 for 15-minute blocks from 1 hour and block rate price/hour also divided by 4

## Limitations and trade-offs

- Data is stored only in memory. Lots are seeded from properties at startup; reservations and invoices are lost on restart.
- The locking and duplicate-request cache protect concurrent requests only inside one application instance. A multi-instance deployment needs shared persistence and distributed locking.
- An actual checkout after the planned end time does not yet reserve extra slot time or resolve collisions with later reservations; this edge case requires operational handling.
- There is no authentication, authorization, or user management.
