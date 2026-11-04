package hu.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ticketflow.application.Mediator;
import hu.ticketflow.application.TicketService;
import hu.ticketflow.domain.JegyTipus;
import hu.ticketflow.domain.event.AvailableTicketsRequested;
import hu.ticketflow.domain.event.TicketPurchaseRequested;
import hu.ticketflow.domain.event.TicketReserved;
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

    // minden teszt után új kontextus kell, hogy újra legyen 100 normál és 50 VIP jegy
    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void tizezerParhuzamosVasarlasUtanPontosanSzazNormalEsOtvenVipJegyKelt() throws Exception {
        StressEredmeny eredmeny = stressRunner.futtat();
        stressRunner.kiir(eredmeny);

        assertThat(eredmeny.osszesKeres()).isEqualTo(10000);
        assertThat(eredmeny.normalSikeres()).isEqualTo(100);
        assertThat(eredmeny.normalSikertelen()).isEqualTo(4900);
        assertThat(eredmeny.vipSikeres()).isEqualTo(50);
        assertThat(eredmeny.vipSikertelen()).isEqualTo(4950);
        assertThat(eredmeny.fizetesek()).isEqualTo(150);
        assertThat(eredmeny.maradtNormal()).isZero();
        assertThat(eredmeny.maradtVip()).isZero();
    }

    // Spring nélkül: a Mediator sorba állítja a 2000 párhuzamos VIP kérést, így pontosan 50 jegy kel el
    @Test
    void parhuzamosVipVasarlasPontosanOtvenJegyetAd() throws Exception {
        int vipKeszlet = 50;
        int kerelmekSzama = 2000;
        TicketService ticketService = new TicketService(0, vipKeszlet);
        AtomicInteger sikeresVasarlasok = new AtomicInteger();
        // a Spring helyett ez a lambda kézbesíti az eseményt a TicketService-nek
        Mediator mediator = new Mediator(esemeny -> {
            if (ticketService.vasarlas((TicketPurchaseRequested) esemeny) instanceof TicketReserved) {
                sikeresVasarlasok.incrementAndGet();
            }
        });

        CountDownLatch rajt = new CountDownLatch(1);
        ExecutorService szalkezelo = Executors.newFixedThreadPool(50);
        List<Future<?>> feladatok = new ArrayList<>();

        try {
            for (int i = 0; i < kerelmekSzama; i++) {
                feladatok.add(szalkezelo.submit(() -> {
                    rajt.await();
                    mediator.send(new TicketPurchaseRequested(JegyTipus.VIP)).get();
                    return null;
                }));
            }

            rajt.countDown();
            for (Future<?> feladat : feladatok) {
                feladat.get(60, TimeUnit.SECONDS);
            }
        } finally {
            szalkezelo.shutdownNow();
        }

        assertThat(sikeresVasarlasok.get()).isEqualTo(vipKeszlet);
        assertThat(ticketService.keszlet(new AvailableTicketsRequested()).vip()).isZero();
    }
}
