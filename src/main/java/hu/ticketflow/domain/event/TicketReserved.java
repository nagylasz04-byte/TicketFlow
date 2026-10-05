package hu.ticketflow.domain.event;

import hu.ticketflow.application.command.JegyTipus;
import java.time.Instant;
import java.util.UUID;

public record TicketReserved(UUID ticketId, JegyTipus tipus, Instant timestamp) implements DomainEvent {
}
