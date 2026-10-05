package hu.ticketflow.domain.event;

import java.time.Instant;

public record TicketPurchaseFailed(String reason, Instant timestamp) implements DomainEvent {
}
