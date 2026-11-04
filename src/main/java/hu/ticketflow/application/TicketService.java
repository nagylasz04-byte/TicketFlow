package hu.ticketflow.application;

import hu.ticketflow.config.TicketProperties;
import hu.ticketflow.domain.JegyTipus;
import hu.ticketflow.domain.event.AvailableTicketsReported;
import hu.ticketflow.domain.event.AvailableTicketsRequested;
import hu.ticketflow.domain.event.DomainEvent;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketPurchaseRequested;
import hu.ticketflow.domain.event.TicketReserved;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

// A jegykészlet. Csak a Mediator eseményszála hívja, ezért elég egy sima int, nem kell zár.
// A metódusok által visszaadott eseményt a Spring automatikusan továbbküldi a többi kezelőnek.
@Service
public class TicketService {

    private int szabadNormal;
    private int szabadVip;

    @Autowired
    public TicketService(TicketProperties beallitasok) {
        this(beallitasok.getTickets(), beallitasok.getVipTickets());
    }

    public TicketService(int normalJegyek, int vipJegyek) {
        this.szabadNormal = normalJegyek;
        this.szabadVip = vipJegyek;
    }

    @EventListener
    public DomainEvent vasarlas(TicketPurchaseRequested kerelem) {
        JegyTipus tipus = kerelem.tipus();
        if (szabad(tipus) <= 0) {
            return new TicketPurchaseFailed(tipus, "Nincs több " + tipus + " jegy");
        }
        csokkent(tipus);
        return new TicketReserved(UUID.randomUUID(), tipus);
    }

    @EventListener
    public AvailableTicketsReported keszlet(AvailableTicketsRequested kerelem) {
        return new AvailableTicketsReported(szabadNormal, szabadVip);
    }

    private int szabad(JegyTipus tipus) {
        return tipus == JegyTipus.VIP ? szabadVip : szabadNormal;
    }

    private void csokkent(JegyTipus tipus) {
        if (tipus == JegyTipus.VIP) {
            szabadVip--;
        } else {
            szabadNormal--;
        }
    }
}
