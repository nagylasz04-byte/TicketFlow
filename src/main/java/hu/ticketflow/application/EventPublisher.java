package hu.ticketflow.application;

import hu.ticketflow.domain.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// Vékony wrapper a Spring eseményküldő fölött
@Component
public class EventPublisher {

    private final ApplicationEventPublisher springPublisher;

    public EventPublisher(ApplicationEventPublisher springPublisher) {
        this.springPublisher = springPublisher;
    }

    public void publish(DomainEvent esemeny) {
        springPublisher.publishEvent(esemeny);
    }
}
