package hu.ticketflow.ui;

import hu.ticketflow.application.Mediator;
import hu.ticketflow.application.command.GetAvailableQuery;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.application.command.PurchaseTicketCommand;
import hu.ticketflow.config.TicketProperties;
import hu.ticketflow.handler.ConsoleLogger;
import hu.ticketflow.handler.StatsCollector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

// Sok szálról egyszerre vásárol jegyet, és megméri az eredményt
@Component
public class StressRunner {

    private static final int VARAKOZAS_MASODPERC = 120;

    private final Mediator mediator;
    private final StatsCollector statsCollector;
    private final ConsoleLogger consoleLogger;
    private final TicketProperties beallitasok;

    public StressRunner(Mediator mediator, StatsCollector statsCollector,
                        ConsoleLogger consoleLogger, TicketProperties beallitasok) {
        this.mediator = mediator;
        this.statsCollector = statsCollector;
        this.consoleLogger = consoleLogger;
        this.beallitasok = beallitasok;
    }

    public StressEredmeny futtat() throws Exception {
        int keresekSzama = beallitasok.getStress().getRequests();
        int szalakSzama = beallitasok.getStress().getThreads();

        // a stressz alatt nem írunk ki eseményeket, mert lassítaná a mérést
        statsCollector.nullazas();
        boolean regiKiiras = consoleLogger.isKiirasBekapcsolva();
        consoleLogger.setKiirasBekapcsolva(false);

        ExecutorService szalkezelo = Executors.newFixedThreadPool(szalakSzama);
        try {
            return kerekekFuttatasa(szalkezelo, keresekSzama);
        } finally {
            szalkezelo.shutdownNow();
            consoleLogger.setKiirasBekapcsolva(regiKiiras);
        }
    }

    private StressEredmeny kerekekFuttatasa(ExecutorService szalkezelo, int keresekSzama) throws Exception {
        CountDownLatch rajt = new CountDownLatch(1);
        List<Future<PurchaseResult>> eredmenyek = new ArrayList<>();

        for (int i = 0; i < keresekSzama; i++) {
            eredmenyek.add(szalkezelo.submit(() -> {
                // minden szál itt vár, amíg el nem indítjuk őket egyszerre
                rajt.await();
                return mediator.send(new PurchaseTicketCommand());
            }));
        }

        long kezdet = System.nanoTime();
        rajt.countDown();

        int sikeres = 0;
        int sikertelen = 0;
        for (Future<PurchaseResult> eredmeny : eredmenyek) {
            PurchaseResult vasarlas = eredmeny.get(VARAKOZAS_MASODPERC, TimeUnit.SECONDS);
            if (vasarlas.sikeres()) {
                sikeres++;
            } else {
                sikertelen++;
            }
        }
        long idoMs = (System.nanoTime() - kezdet) / 1_000_000;

        int maradtJegy = mediator.send(new GetAvailableQuery());
        long kerescMasodpercenkent = 0;
        if (idoMs > 0) {
            kerescMasodpercenkent = keresekSzama * 1000L / idoMs;
        }
        return new StressEredmeny(keresekSzama, sikeres, sikertelen, maradtJegy, idoMs, kerescMasodpercenkent);
    }

    public void kiir(StressEredmeny eredmeny) {
        System.out.println("Összes kérés:     " + eredmeny.osszesKeres());
        System.out.println("Sikeres vásárlás: " + eredmeny.sikeres());
        System.out.println("Sikertelen:       " + eredmeny.sikertelen());
        System.out.println("Maradt jegy:      " + eredmeny.maradtJegy());
        System.out.println("Futási idő:       " + eredmeny.idoMs() + " ms");
        System.out.println("Throughput:       " + eredmeny.kerescMasodpercenkent() + " kérés/mp");
        System.out.println("Események:        foglalás=" + statsCollector.sikeresFoglalasok()
                + ", sikertelen=" + statsCollector.sikertelenFoglalasok()
                + ", fizetés=" + statsCollector.teljesitettFizetesek());
    }
}
