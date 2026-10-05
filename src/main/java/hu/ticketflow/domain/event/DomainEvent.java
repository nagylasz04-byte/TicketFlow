package hu.ticketflow.domain.event;

import java.time.Instant;

public interface DomainEvent {

    Instant timestamp();
}
