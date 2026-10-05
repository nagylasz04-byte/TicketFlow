package hu.ticketflow.handler;

import hu.ticketflow.application.EventPublisher;
import hu.ticketflow.config.TicketProperties;
import hu.ticketflow.domain.event.PaymentCompleted;
import hu.ticketflow.domain.event.TicketReserved;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Szimulált fizetés: foglalás után (opcionális várakozással) lefizeti a jegyet
@Component
public class PaymentHandler {

    private final EventPublisher eventPublisher;
    private final long kesleltetesMs;

    @Autowired
    public PaymentHandler(EventPublisher eventPublisher, TicketProperties beallitasok) {
        this(eventPublisher, beallitasok.getPayment().getDelayMs());
    }

    public PaymentHandler(EventPublisher eventPublisher, long kesleltetesMs) {
        this.eventPublisher = eventPublisher;
        this.kesleltetesMs = kesleltetesMs;
    }

    @EventListener
    public void foglalasKezelese(TicketReserved esemeny) {
        varakozas();
        eventPublisher.publish(new PaymentCompleted(esemeny.ticketId(), Instant.now()));
    }

    private void varakozas() {
        if (kesleltetesMs <= 0) {
            return;
        }
        try {
            Thread.sleep(kesleltetesMs);
        } catch (InterruptedException hiba) {
            Thread.currentThread().interrupt();
        }
    }
}
