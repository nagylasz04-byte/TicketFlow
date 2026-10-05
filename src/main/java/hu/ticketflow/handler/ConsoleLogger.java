package hu.ticketflow.handler;

import hu.ticketflow.domain.event.PaymentCompleted;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketReserved;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Kiírja az eseményeket, stressz alatt ki lehet kapcsolni
@Component
public class ConsoleLogger {

    private volatile boolean kiirasBekapcsolva = true;

    public boolean isKiirasBekapcsolva() {
        return kiirasBekapcsolva;
    }

    public void setKiirasBekapcsolva(boolean kiirasBekapcsolva) {
        this.kiirasBekapcsolva = kiirasBekapcsolva;
    }

    @EventListener
    public void foglalasEsemeny(TicketReserved esemeny) {
        kiir("[Esemény] Jegy lefoglalva (" + esemeny.tipus() + "): " + esemeny.ticketId());
    }

    @EventListener
    public void fizetesEsemeny(PaymentCompleted esemeny) {
        kiir("[Esemény] Fizetés teljesítve: " + esemeny.ticketId());
    }

    @EventListener
    public void sikertelenEsemeny(TicketPurchaseFailed esemeny) {
        kiir("[Esemény] Sikertelen vásárlás: " + esemeny.reason());
    }

    private void kiir(String szoveg) {
        if (kiirasBekapcsolva) {
            System.out.println(szoveg);
        }
    }
}
