package hu.ticketflow.domain.event;

import hu.ticketflow.domain.JegyTipus;

// Nem volt szabad jegy
public record TicketPurchaseFailed(JegyTipus tipus, String reason) implements DomainEvent {
}
