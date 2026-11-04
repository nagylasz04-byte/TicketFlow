package hu.ticketflow.domain.event;

import hu.ticketflow.domain.JegyTipus;
import java.util.UUID;

// A lefoglalt jegyet kifizették, a vásárlás kész
public record PaymentCompleted(UUID ticketId, JegyTipus tipus) implements DomainEvent {
}
