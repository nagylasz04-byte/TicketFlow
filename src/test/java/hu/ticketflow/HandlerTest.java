package hu.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ticketflow.application.EventPublisher;
import hu.ticketflow.domain.event.PaymentCompleted;
import hu.ticketflow.domain.event.TicketReserved;
import hu.ticketflow.handler.PaymentHandler;
import hu.ticketflow.handler.StatsCollector;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HandlerTest {

    @Test
    void statsCollectorSzamolEsNullaz() {
        StatsCollector statsCollector = new StatsCollector();

        statsCollector.foglalasEsemeny(new TicketReserved(UUID.randomUUID(), Instant.now()));
        statsCollector.foglalasEsemeny(new TicketReserved(UUID.randomUUID(), Instant.now()));
        assertThat(statsCollector.sikeresFoglalasok()).isEqualTo(2);

        statsCollector.nullazas();
        assertThat(statsCollector.sikeresFoglalasok()).isZero();
    }

    @Test
    void fizetesKezeloFizetesEsemenytKuldFoglalasra() {
        List<Object> kapottEsemenyek = new ArrayList<>();
        EventPublisher eventPublisher = new EventPublisher(kapottEsemenyek::add);
        PaymentHandler fizetesKezelo = new PaymentHandler(eventPublisher, 0L);
        UUID jegyAzonosito = UUID.randomUUID();

        fizetesKezelo.foglalasKezelese(new TicketReserved(jegyAzonosito, Instant.now()));

        assertThat(kapottEsemenyek).hasSize(1);
        assertThat(kapottEsemenyek.get(0)).isInstanceOf(PaymentCompleted.class);
        assertThat(((PaymentCompleted) kapottEsemenyek.get(0)).ticketId()).isEqualTo(jegyAzonosito);
    }
}
