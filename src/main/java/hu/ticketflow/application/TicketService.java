package hu.ticketflow.application;

import hu.ticketflow.application.command.JegyTipus;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.config.TicketProperties;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketReserved;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private final AtomicInteger szabadNormal;
    private final AtomicInteger szabadVip;
    private final EventPublisher eventPublisher;

    @Autowired
    public TicketService(TicketProperties beallitasok, EventPublisher eventPublisher) {
        this(beallitasok.getTickets(), beallitasok.getVipTickets(), eventPublisher);
    }

    public TicketService(int normalJegyek, int vipJegyek, EventPublisher eventPublisher) {
        this.szabadNormal = new AtomicInteger(normalJegyek);
        this.szabadVip = new AtomicInteger(vipJegyek);
        this.eventPublisher = eventPublisher;
    }

    public PurchaseResult vasarlas(JegyTipus tipus) {
        AtomicInteger keszlet = keszlet(tipus);

        while (true) {
            int jelenlegiSzabad = keszlet.get();

            if (jelenlegiSzabad <= 0) {
                eventPublisher.publish(new TicketPurchaseFailed("Nincs több jegy", Instant.now()));
                return new PurchaseResult(false, null, "Sikertelen vásárlás, nincs több " + tipus + " jegy.");
            }

            // csak akkor csökkentünk, ha közben más nem változtatta meg az értéket
            if (keszlet.compareAndSet(jelenlegiSzabad, jelenlegiSzabad - 1)) {
                UUID jegyAzonosito = UUID.randomUUID();
                eventPublisher.publish(new TicketReserved(jegyAzonosito, tipus, Instant.now()));
                return new PurchaseResult(true, jegyAzonosito, "Sikeres vásárlás (" + tipus + ").");
            }
        }
    }

    public int szabadJegyek(JegyTipus tipus) {
        return keszlet(tipus).get();
    }

    private AtomicInteger keszlet(JegyTipus tipus) {
        return tipus == JegyTipus.VIP ? szabadVip : szabadNormal;
    }
}
