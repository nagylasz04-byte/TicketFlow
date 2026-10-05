package hu.ticketflow.application;

import hu.ticketflow.application.command.JegyTipus;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.config.TicketProperties;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketReserved;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private final ReentrantLock zar = new ReentrantLock(true);
    private int szabadNormal;
    private int szabadVip;
    private final EventPublisher eventPublisher;

    @Autowired
    public TicketService(TicketProperties beallitasok, EventPublisher eventPublisher) {
        this(beallitasok.getTickets(), beallitasok.getVipTickets(), eventPublisher);
    }

    public TicketService(int normalJegyek, int vipJegyek, EventPublisher eventPublisher) {
        this.szabadNormal = normalJegyek;
        this.szabadVip = vipJegyek;
        this.eventPublisher = eventPublisher;
    }

    public PurchaseResult vasarlas(JegyTipus tipus) {
        // fair lock: a szálak érkezési (várakozási) sorrendben jutnak be
        zar.lock();
        try {
            if (szabad(tipus) <= 0) {
                eventPublisher.publish(new TicketPurchaseFailed("Nincs több jegy", Instant.now()));
                return new PurchaseResult(false, null, "Sikertelen vásárlás, nincs több " + tipus + " jegy.");
            }

            csokkent(tipus);
            UUID jegyAzonosito = UUID.randomUUID();
            eventPublisher.publish(new TicketReserved(jegyAzonosito, tipus, Instant.now()));
            return new PurchaseResult(true, jegyAzonosito, "Sikeres vásárlás (" + tipus + ").");
        } finally {
            zar.unlock();
        }
    }

    public int szabadJegyek(JegyTipus tipus) {
        zar.lock();
        try {
            return szabad(tipus);
        } finally {
            zar.unlock();
        }
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
