package hu.ticketflow.handler;

import hu.ticketflow.domain.event.PaymentCompleted;
import hu.ticketflow.domain.event.TicketCancelled;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketReserved;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Megszámolja az eseményeket, a stresszteszt ebből ellenőriz
@Component
public class StatsCollector {

    private final LongAdder sikeresFoglalasok = new LongAdder();
    private final LongAdder sikertelenFoglalasok = new LongAdder();
    private final LongAdder toroltJegyek = new LongAdder();
    private final LongAdder teljesitettFizetesek = new LongAdder();

    @EventListener
    public void foglalasEsemeny(TicketReserved esemeny) {
        sikeresFoglalasok.increment();
    }

    @EventListener
    public void sikertelenEsemeny(TicketPurchaseFailed esemeny) {
        sikertelenFoglalasok.increment();
    }

    @EventListener
    public void torlesEsemeny(TicketCancelled esemeny) {
        toroltJegyek.increment();
    }

    @EventListener
    public void fizetesEsemeny(PaymentCompleted esemeny) {
        teljesitettFizetesek.increment();
    }

    public long sikeresFoglalasok() {
        return sikeresFoglalasok.sum();
    }

    public long sikertelenFoglalasok() {
        return sikertelenFoglalasok.sum();
    }

    public long toroltJegyek() {
        return toroltJegyek.sum();
    }

    public long teljesitettFizetesek() {
        return teljesitettFizetesek.sum();
    }

    public void nullazas() {
        sikeresFoglalasok.reset();
        sikertelenFoglalasok.reset();
        toroltJegyek.reset();
        teljesitettFizetesek.reset();
    }
}
