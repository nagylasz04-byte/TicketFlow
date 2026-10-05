package hu.ticketflow.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TicketReserved(UUID ticketId, Instant timestamp) implements DomainEvent {
}
