package hu.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ticketflow.domain.JegyTipus;
import hu.ticketflow.domain.event.PaymentCompleted;
import hu.ticketflow.domain.event.StressTestStarted;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketReserved;
import hu.ticketflow.handler.PaymentHandler;
import hu.ticketflow.handler.StatsCollector;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HandlerTest {

    @Test
    void fizetesKezeloFizetesEsemennyelValaszolAFoglalasra() {
        PaymentHandler fizetesKezelo = new PaymentHandler(0L);
        UUID jegyAzonosito = UUID.randomUUID();

        PaymentCompleted fizetes = fizetesKezelo.foglalasKezelese(new TicketReserved(jegyAzonosito, JegyTipus.VIP));

        assertThat(fizetes.ticketId()).isEqualTo(jegyAzonosito);
        assertThat(fizetes.tipus()).isEqualTo(JegyTipus.VIP);
    }

    @Test
    void statsCollectorTipusonkentSzamolEsStresszIndulaskorNullaz() {
        StatsCollector statsCollector = new StatsCollector();

        statsCollector.foglalas(new TicketReserved(UUID.randomUUID(), JegyTipus.NORMAL));
        statsCollector.foglalas(new TicketReserved(UUID.randomUUID(), JegyTipus.VIP));
        statsCollector.sikertelen(new TicketPurchaseFailed(JegyTipus.VIP, "Nincs több VIP jegy"));
        assertThat(statsCollector.normalSikeres()).isEqualTo(1);
        assertThat(statsCollector.vipSikeres()).isEqualTo(1);
        assertThat(statsCollector.vipSikertelen()).isEqualTo(1);

        statsCollector.stresszIndul(new StressTestStarted());
        assertThat(statsCollector.normalSikeres()).isZero();
        assertThat(statsCollector.vipSikeres()).isZero();
        assertThat(statsCollector.vipSikertelen()).isZero();
    }
}
