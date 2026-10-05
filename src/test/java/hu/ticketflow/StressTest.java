package hu.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ticketflow.application.EventPublisher;
import hu.ticketflow.application.TicketService;
import hu.ticketflow.application.command.CancelResult;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.handler.StatsCollector;
import hu.ticketflow.ui.StressEredmeny;
import hu.ticketflow.ui.StressRunner;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
class StressTest {

    @Autowired
    private StressRunner stressRunner;

    @Autowired
    private StatsCollector statsCollector;

    // minden teszt után új kontextus kell, hogy újra legyen 100 jegy
    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void tizezerParhuzamosVasarlasUtanPontosanSzazJegyKelt() throws Exception {
        StressEredmeny eredmeny = stressRunner.futtat();
        stressRunner.kiir(eredmeny);

        assertThat(eredmeny.osszesKeres()).isEqualTo(10000);
        assertThat(eredmeny.sikeres()).isEqualTo(100);
        assertThat(eredmeny.sikertelen()).isEqualTo(9900);
        assertThat(eredmeny.maradtJegy()).isZero();
        assertThat(statsCollector.sikeresFoglalasok()).isEqualTo(100);
        assertThat(statsCollector.sikertelenFoglalasok()).isEqualTo(9900);
    }

    // saját service-t építünk, Spring nélkül
    @Test
    void vegyesVasarlasEsLemondasUtanErvenyesMarad() throws Exception {
        int osszes = 50;
        int kerelmekSzama = 2000;
        TicketService ticketService = new TicketService(osszes, new EventPublisher(esemeny -> { }));
        AtomicInteger sikeresVasarlasok = new AtomicInteger();
        AtomicInteger sikeresLemondasok = new AtomicInteger();
        AtomicInteger hibasAllapotok = new AtomicInteger();

        CountDownLatch rajt = new CountDownLatch(1);
        ExecutorService szalkezelo = Executors.newFixedThreadPool(50);
        List<Future<?>> feladatok = new ArrayList<>();

        try {
            for (int i = 0; i < kerelmekSzama; i++) {
                final int sorszam = i;
                feladatok.add(szalkezelo.submit(() -> {
                    try {
                        rajt.await();
                    } catch (InterruptedException hiba) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    PurchaseResult vasarlas = ticketService.vasarlas();
                    if (vasarlas.sikeres()) {
                        sikeresVasarlasok.incrementAndGet();
                        // minden másodikat lemondjuk
                        if (sorszam % 2 == 0) {
                            CancelResult lemondas = ticketService.lemondas(vasarlas.jegyAzonosito());
                            if (lemondas.sikeres()) {
                                sikeresLemondasok.incrementAndGet();
                            }
                        }
                    }
                    int szabad = ticketService.szabadJegyek();
                    if (szabad < 0 || szabad > osszes) {
                        hibasAllapotok.incrementAndGet();
                    }
                }));
            }

            rajt.countDown();
            for (Future<?> feladat : feladatok) {
                feladat.get(60, TimeUnit.SECONDS);
            }
        } finally {
            szalkezelo.shutdownNow();
        }

        int szabadVegen = ticketService.szabadJegyek();
        assertThat(hibasAllapotok.get()).isZero();
        assertThat(szabadVegen).isBetween(0, osszes);
        assertThat(szabadVegen).isEqualTo(osszes - sikeresVasarlasok.get() + sikeresLemondasok.get());
    }
}
