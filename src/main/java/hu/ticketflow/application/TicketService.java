package hu.ticketflow.application;

import hu.ticketflow.application.command.CancelResult;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.config.TicketProperties;
import hu.ticketflow.domain.event.TicketCancelled;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketReserved;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private final int osszesJegy;
    private final AtomicInteger szabadJegyek;
    // a jelenleg foglalt jegyek azonosítói, így csak valódi jegyet lehet lemondani
    private final Set<UUID> foglaltJegyek = ConcurrentHashMap.newKeySet();
    private final EventPublisher eventPublisher;

    @Autowired
    public TicketService(TicketProperties beallitasok, EventPublisher eventPublisher) {
        this(beallitasok.getTickets(), eventPublisher);
    }

    public TicketService(int osszesJegy, EventPublisher eventPublisher) {
        this.osszesJegy = osszesJegy;
        this.szabadJegyek = new AtomicInteger(osszesJegy);
        this.eventPublisher = eventPublisher;
    }

    public PurchaseResult vasarlas() {
        while (true) {
            int jelenlegiSzabad = szabadJegyek.get();

            if (jelenlegiSzabad <= 0) {
                eventPublisher.publish(new TicketPurchaseFailed("Nincs több jegy", Instant.now()));
                return new PurchaseResult(false, null, "Sikertelen vásárlás, nincs több jegy.");
            }

            // csak akkor csökkentünk, ha közben más nem változtatta meg az értéket
            if (szabadJegyek.compareAndSet(jelenlegiSzabad, jelenlegiSzabad - 1)) {
                UUID jegyAzonosito = UUID.randomUUID();
                foglaltJegyek.add(jegyAzonosito);
                eventPublisher.publish(new TicketReserved(jegyAzonosito, Instant.now()));
                return new PurchaseResult(true, jegyAzonosito, "Sikeres vásárlás.");
            }
        }
    }

    public CancelResult lemondas(UUID jegyAzonosito) {
        // a remove csak egyszer ad igazat, így dupla lemondás nem növeli a készletet
        boolean voltFoglalt = foglaltJegyek.remove(jegyAzonosito);

        if (!voltFoglalt) {
            return new CancelResult(false, "Ismeretlen vagy már lemondott jegy.");
        }

        szabadJegyek.incrementAndGet();
        eventPublisher.publish(new TicketCancelled(jegyAzonosito, Instant.now()));
        return new CancelResult(true, "Jegy lemondva.");
    }

    public int szabadJegyek() {
        return szabadJegyek.get();
    }

    public int osszesJegy() {
        return osszesJegy;
    }
}
