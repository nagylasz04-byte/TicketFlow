package hu.ticketflow.domain.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompleted(UUID ticketId, Instant timestamp) implements DomainEvent {
}
