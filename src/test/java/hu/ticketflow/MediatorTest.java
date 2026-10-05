package hu.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ticketflow.application.EventPublisher;
import hu.ticketflow.application.Mediator;
import hu.ticketflow.application.TicketService;
import hu.ticketflow.application.command.CancelResult;
import hu.ticketflow.application.command.CancelTicketCommand;
import hu.ticketflow.application.command.GetAvailableQuery;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.application.command.PurchaseTicketCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MediatorTest {

    private Mediator mediator;

    @BeforeEach
    void elokeszites() {
        EventPublisher eventPublisher = new EventPublisher(esemeny -> { });
        TicketService ticketService = new TicketService(3, eventPublisher);
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
    }

    @Test
    void lemondasParancsVisszaadjaAJegyet() {
        PurchaseResult vasarlas = mediator.send(new PurchaseTicketCommand());

        CancelResult eredmeny = mediator.send(new CancelTicketCommand(vasarlas.jegyAzonosito()));

        assertThat(eredmeny.sikeres()).isTrue();
        assertThat(mediator.send(new GetAvailableQuery())).isEqualTo(3);
    }
}
