package hu.ticketflow.domain.event;

import hu.ticketflow.domain.JegyTipus;

// Valaki jegyet szeretne venni
public record TicketPurchaseRequested(JegyTipus tipus) implements DomainEvent {
}
