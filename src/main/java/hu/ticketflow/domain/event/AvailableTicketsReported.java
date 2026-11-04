package hu.ticketflow.domain.event;

// A jegykészlet válasza: ennyi szabad jegy van típusonként
public record AvailableTicketsReported(int normal, int vip) implements DomainEvent {
}
