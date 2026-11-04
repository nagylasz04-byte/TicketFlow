package hu.ticketflow.ui;

import hu.ticketflow.application.Mediator;
import hu.ticketflow.config.TicketProperties;
import hu.ticketflow.domain.JegyTipus;
import hu.ticketflow.domain.event.AvailableTicketsRequested;
import hu.ticketflow.domain.event.StressTestFinished;
import hu.ticketflow.domain.event.StressTestStarted;
import hu.ticketflow.domain.event.TicketPurchaseRequested;
import hu.ticketflow.handler.StatsCollector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

// Sok szálról egyszerre küld vásárlási kérést a Mediatornak, a végén a StatsCollector számlálóiból készít jelentést
@Component
public class StressRunner {

    private static final int VARAKOZAS_MASODPERC = 120;

    private final Mediator mediator;
    private final StatsCollector statsCollector;
    private final TicketProperties beallitasok;

    public StressRunner(Mediator mediator, StatsCollector statsCollector, TicketProperties beallitasok) {
        this.mediator = mediator;
        this.statsCollector = statsCollector;
        this.beallitasok = beallitasok;
    }

    public StressEredmeny futtat() throws Exception {
        int keresekSzama = beallitasok.getStress().getRequests();
        int szalakSzama = beallitasok.getStress().getThreads();

        // nullázza a számlálókat és elnémítja a kiírást
        megvar(mediator.send(new StressTestStarted()));
        ExecutorService szalkezelo = Executors.newFixedThreadPool(szalakSzama);
        try {
            long idoMs = keresekKuldese(szalkezelo, keresekSzama);
            // a válaszeseményből a StatsCollector feljegyzi a maradék készletet
            megvar(mediator.send(new AvailableTicketsRequested()));
            return new StressEredmeny(keresekSzama,
                    statsCollector.normalSikeres(), statsCollector.normalSikertelen(),
                    statsCollector.vipSikeres(), statsCollector.vipSikertelen(),
                    statsCollector.fizetesek(),
                    statsCollector.maradtNormal(), statsCollector.maradtVip(),
                    idoMs);
        } finally {
            szalkezelo.shutdownNow();
            megvar(mediator.send(new StressTestFinished()));
        }
    }

    // elküldi az összes kérést, és visszaadja, hány ms alatt dolgozta fel őket a rendszer
    private long keresekKuldese(ExecutorService szalkezelo, int keresekSzama) throws Exception {
        CountDownLatch rajt = new CountDownLatch(1);
        List<Future<?>> feladatok = new ArrayList<>();

        for (int i = 0; i < keresekSzama; i++) {
            // a kérések felét normál, felét VIP jegyre indítjuk
            JegyTipus tipus = i % 2 == 0 ? JegyTipus.NORMAL : JegyTipus.VIP;
            feladatok.add(szalkezelo.submit(() -> {
                // minden szál itt vár, amíg el nem indítjuk őket egyszerre
                rajt.await();
                megvar(mediator.send(new TicketPurchaseRequested(tipus)));
                return null;
            }));
        }

        long kezdet = System.nanoTime();
        rajt.countDown();
        for (Future<?> feladat : feladatok) {
            megvar(feladat);
        }
        return (System.nanoTime() - kezdet) / 1_000_000;
    }

    private static void megvar(Future<?> feladat) throws Exception {
        feladat.get(VARAKOZAS_MASODPERC, TimeUnit.SECONDS);
    }

    public void kiir(StressEredmeny eredmeny) {
        System.out.println("Összes kérés:      " + eredmeny.osszesKeres());
        System.out.println("Normál sikeres:    " + eredmeny.normalSikeres());
        System.out.println("Normál sikertelen: " + eredmeny.normalSikertelen());
        System.out.println("VIP sikeres:       " + eredmeny.vipSikeres());
        System.out.println("VIP sikertelen:    " + eredmeny.vipSikertelen());
        System.out.println("Fizetések:         " + eredmeny.fizetesek());
        System.out.println("Maradt normál:     " + eredmeny.maradtNormal());
        System.out.println("Maradt VIP:        " + eredmeny.maradtVip());
        System.out.println("Futási idő:        " + eredmeny.idoMs() + " ms");
        System.out.println("Throughput:        " + eredmeny.keresMasodpercenkent() + " kérés/mp");
    }
}
