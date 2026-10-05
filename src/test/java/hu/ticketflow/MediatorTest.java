package hu.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ticketflow.application.EventPublisher;
import hu.ticketflow.application.Mediator;
import hu.ticketflow.application.TicketService;
import hu.ticketflow.application.command.GetAvailableQuery;
import hu.ticketflow.application.command.JegyTipus;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.application.command.PurchaseTicketCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MediatorTest {

    private Mediator mediator;

    @BeforeEach
    void elokeszites() {
        EventPublisher eventPublisher = new EventPublisher(esemeny -> { });
        TicketService ticketService = new TicketService(3, 2, eventPublisher);
        mediator = new Mediator(ticketService);
    }

    @Test
    void vasarlasParancsMegvasarolEgyJegyet() {
        PurchaseResult eredmeny = mediator.send(new PurchaseTicketCommand());

        assertThat(eredmeny.sikeres()).isTrue();
        assertThat(mediator.send(new GetAvailableQuery())).isEqualTo(2);
    }

    @Test
    void lekerdezesAKezdetiKeszletetAdjaVissza() {
        assertThat(mediator.send(new GetAvailableQuery())).isEqualTo(3);
        assertThat(mediator.send(new GetAvailableQuery(JegyTipus.VIP))).isEqualTo(2);
    }

    @Test
    void vipVasarlasParancsAVipKeszletbolVesz() {
        PurchaseResult eredmeny = mediator.send(new PurchaseTicketCommand(JegyTipus.VIP));

        assertThat(eredmeny.sikeres()).isTrue();
        assertThat(mediator.send(new GetAvailableQuery(JegyTipus.VIP))).isEqualTo(1);
        assertThat(mediator.send(new GetAvailableQuery())).isEqualTo(3);
    }
}
