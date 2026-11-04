package hu.ticketflow.handler;

import hu.ticketflow.domain.event.AvailableTicketsReported;
import hu.ticketflow.domain.event.PaymentCompleted;
import hu.ticketflow.domain.event.StressTestFinished;
import hu.ticketflow.domain.event.StressTestStarted;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketReserved;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Kiírja az eseményeket; stresszteszt alatt hallgat, hogy ne lassítsa a mérést
@Component
public class ConsoleLogger {

    private boolean kiirasBekapcsolva = true;

    @EventListener
    public void stresszIndul(StressTestStarted esemeny) {
        kiirasBekapcsolva = false;
    }

    @EventListener
    public void stresszVege(StressTestFinished esemeny) {
        kiirasBekapcsolva = true;
    }

    // elsőként kapja meg a foglalást, különben a fizetés kiírása megelőzné
    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void foglalasEsemeny(TicketReserved esemeny) {
        kiir("[Esemény] Jegy lefoglalva (" + esemeny.tipus() + "): " + esemeny.ticketId());
    }

    @EventListener
    public void fizetesEsemeny(PaymentCompleted esemeny) {
        kiir("[Esemény] Fizetés teljesítve, sikeres vásárlás (" + esemeny.tipus() + "): " + esemeny.ticketId());
    }

    @EventListener
    public void sikertelenEsemeny(TicketPurchaseFailed esemeny) {
        kiir("[Esemény] Sikertelen vásárlás: " + esemeny.reason());
    }

    @EventListener
    public void keszletEsemeny(AvailableTicketsReported esemeny) {
        kiir("[Esemény] Szabad jegyek: " + esemeny.normal() + " normál, " + esemeny.vip() + " VIP");
    }

    private void kiir(String szoveg) {
        if (kiirasBekapcsolva) {
            System.out.println(szoveg);
        }
    }
}
