package hu.ticketflow.domain.event;

// Indul a stresszteszt: a számlálók nullázódnak, a kiírás elhallgat
public record StressTestStarted() implements DomainEvent {
}
