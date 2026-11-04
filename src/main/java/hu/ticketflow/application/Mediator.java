package hu.ticketflow.application;

import hu.ticketflow.domain.event.DomainEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// A rendszer postása: minden esemény itt lép be, és egyetlen szálon, érkezési sorrendben kézbesítődik.
// Mivel egyszerre mindig csak egy esemény fut, a kezelőknek nem kell zár.
@Component
public class Mediator {

    private final ApplicationEventPublisher springPublisher;
    private final ExecutorService esemenySzal = Executors.newSingleThreadExecutor(feladat -> {
        Thread szal = new Thread(feladat, "esemeny");
        // háttérszál: kilépéskor nem tartja életben a programot
        szal.setDaemon(true);
        return szal;
    });

    public Mediator(ApplicationEventPublisher springPublisher) {
        this.springPublisher = springPublisher;
    }

    // Beállítja az eseményt a sorba. A Future akkor kész, amikor az esemény
    // és az összes belőle következő esemény (pl. foglalás -> fizetés) lefutott.
    public Future<?> send(DomainEvent esemeny) {
        return esemenySzal.submit(() -> springPublisher.publishEvent(esemeny));
    }
}
