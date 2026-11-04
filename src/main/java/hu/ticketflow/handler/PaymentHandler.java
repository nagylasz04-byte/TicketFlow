package hu.ticketflow.handler;

import hu.ticketflow.config.TicketProperties;
import hu.ticketflow.domain.event.PaymentCompleted;
import hu.ticketflow.domain.event.TicketReserved;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Szimulált fizetés: a foglalásra (opcionális várakozás után) fizetés-eseménnyel válaszol
@Component
public class PaymentHandler {

    private final long kesleltetesMs;

    @Autowired
    public PaymentHandler(TicketProperties beallitasok) {
        this(beallitasok.getPayment().getDelayMs());
    }

    public PaymentHandler(long kesleltetesMs) {
        this.kesleltetesMs = kesleltetesMs;
    }

    @EventListener
    public PaymentCompleted foglalasKezelese(TicketReserved foglalas) {
        varakozas();
        return new PaymentCompleted(foglalas.ticketId(), foglalas.tipus());
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
