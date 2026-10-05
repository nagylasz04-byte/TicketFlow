package hu.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ticketflow.application.EventPublisher;
import hu.ticketflow.application.TicketService;
import hu.ticketflow.application.command.JegyTipus;
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
        assertThat(eredmeny.maradtNormal()).isZero();
        assertThat(eredmeny.maradtVip()).isZero();
        assertThat(statsCollector.sikeresFoglalasok()).isEqualTo(150);
        assertThat(statsCollector.sikertelenFoglalasok()).isEqualTo(9850);
        assertThat(statsCollector.teljesitettFizetesek()).isEqualTo(150);
    }

    // saját service-t építünk, Spring nélkül: 50 VIP jegyre sem lehet túladni
    @Test
    void parhuzamosVipVasarlasPontosanOtvenJegyetAd() throws Exception {
        int vipKeszlet = 50;
        int kerelmekSzama = 2000;
        TicketService ticketService = new TicketService(0, vipKeszlet, new EventPublisher(esemeny -> { }));
        AtomicInteger sikeresVasarlasok = new AtomicInteger();

        CountDownLatch rajt = new CountDownLatch(1);
        ExecutorService szalkezelo = Executors.newFixedThreadPool(50);
        List<Future<?>> feladatok = new ArrayList<>();

        try {
            for (int i = 0; i < kerelmekSzama; i++) {
                feladatok.add(szalkezelo.submit(() -> {
                    try {
                        rajt.await();
                    } catch (InterruptedException hiba) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (ticketService.vasarlas(JegyTipus.VIP).sikeres()) {
                        sikeresVasarlasok.incrementAndGet();
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

        assertThat(sikeresVasarlasok.get()).isEqualTo(vipKeszlet);
        assertThat(ticketService.szabadJegyek(JegyTipus.VIP)).isZero();
    }
}
