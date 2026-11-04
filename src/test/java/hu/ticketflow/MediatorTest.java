package hu.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ticketflow.application.Mediator;
import hu.ticketflow.domain.JegyTipus;
import hu.ticketflow.domain.event.AvailableTicketsRequested;
import hu.ticketflow.domain.event.TicketPurchaseRequested;
import hu.ticketflow.handler.StatsCollector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

// A teljes eseménylánc Springgel: Mediator -> TicketService -> PaymentHandler -> StatsCollector
@SpringBootTest(properties = {"ticketflow.tickets=3", "ticketflow.vip-tickets=2"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MediatorTest {

    @Autowired
    private Mediator mediator;

    @Autowired
    private StatsCollector statsCollector;

    @Test
    void vasarlasVegigmegyAzEsemenylancon() throws Exception {
        mediator.send(new TicketPurchaseRequested(JegyTipus.NORMAL)).get();
        mediator.send(new AvailableTicketsRequested()).get();

        assertThat(statsCollector.normalSikeres()).isEqualTo(1);
        assertThat(statsCollector.fizetesek()).isEqualTo(1);
        assertThat(statsCollector.maradtNormal()).isEqualTo(2);
    }

    @Test
    void keszletLekerdezesAKezdetiKeszletetAdja() throws Exception {
        mediator.send(new AvailableTicketsRequested()).get();

        assertThat(statsCollector.maradtNormal()).isEqualTo(3);
        assertThat(statsCollector.maradtVip()).isEqualTo(2);
    }

    @Test
    void vipVasarlasAVipKeszletbolVesz() throws Exception {
        mediator.send(new TicketPurchaseRequested(JegyTipus.VIP)).get();
        mediator.send(new AvailableTicketsRequested()).get();

        assertThat(statsCollector.vipSikeres()).isEqualTo(1);
        assertThat(statsCollector.maradtVip()).isEqualTo(1);
        assertThat(statsCollector.maradtNormal()).isEqualTo(3);
    }

    @Test
    void elfogyottJegyreSikertelenEsemenyJonEsNincsFizetes() throws Exception {
        for (int i = 0; i < 3; i++) {
            mediator.send(new TicketPurchaseRequested(JegyTipus.VIP)).get();
        }

        assertThat(statsCollector.vipSikeres()).isEqualTo(2);
        assertThat(statsCollector.vipSikertelen()).isEqualTo(1);
        assertThat(statsCollector.fizetesek()).isEqualTo(2);
    }
}
