package hu.ticketflow.handler;

import hu.ticketflow.domain.JegyTipus;
import hu.ticketflow.domain.event.AvailableTicketsReported;
import hu.ticketflow.domain.event.PaymentCompleted;
import hu.ticketflow.domain.event.StressTestStarted;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketReserved;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Megszámolja az eseményeket. Csak az eseményszál írja, a stresszteszt a végén olvassa ki.
@Component
public class StatsCollector {

    private int normalSikeres;
    private int normalSikertelen;
    private int vipSikeres;
    private int vipSikertelen;
    private int fizetesek;
    private int maradtNormal;
    private int maradtVip;

    @EventListener
    public void stresszIndul(StressTestStarted esemeny) {
        normalSikeres = 0;
        normalSikertelen = 0;
        vipSikeres = 0;
        vipSikertelen = 0;
        fizetesek = 0;
    }

    @EventListener
    public void foglalas(TicketReserved esemeny) {
        if (esemeny.tipus() == JegyTipus.VIP) {
            vipSikeres++;
        } else {
            normalSikeres++;
        }
    }

    @EventListener
    public void sikertelen(TicketPurchaseFailed esemeny) {
        if (esemeny.tipus() == JegyTipus.VIP) {
            vipSikertelen++;
        } else {
            normalSikertelen++;
        }
    }

    @EventListener
    public void fizetes(PaymentCompleted esemeny) {
        fizetesek++;
    }

    @EventListener
    public void keszlet(AvailableTicketsReported esemeny) {
        maradtNormal = esemeny.normal();
        maradtVip = esemeny.vip();
    }

    public int normalSikeres() {
        return normalSikeres;
    }

    public int normalSikertelen() {
        return normalSikertelen;
    }

    public int vipSikeres() {
        return vipSikeres;
    }

    public int vipSikertelen() {
        return vipSikertelen;
    }

    public int fizetesek() {
        return fizetesek;
    }

    public int maradtNormal() {
        return maradtNormal;
    }

    public int maradtVip() {
        return maradtVip;
    }
}
