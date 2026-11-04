package hu.ticketflow.domain.event;

import hu.ticketflow.domain.JegyTipus;
import java.util.UUID;

// Volt szabad jegy, lefoglaltuk
public record TicketReserved(UUID ticketId, JegyTipus tipus) implements DomainEvent {
}
