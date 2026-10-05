package hu.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ticketflow.application.EventPublisher;
import hu.ticketflow.application.TicketService;
import hu.ticketflow.application.command.CancelResult;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.domain.event.TicketCancelled;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketReserved;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketServiceTest {

    private List<Object> kiadottEsemenyek;
    private TicketService ticketService;

    @BeforeEach
    void elokeszites() {
        kiadottEsemenyek = new ArrayList<>();
        // a Spring publisher helyett egy egyszerű lista gyűjti az eseményeket
        EventPublisher eventPublisher = new EventPublisher(esemeny -> kiadottEsemenyek.add(esemeny));
        ticketService = new TicketService(2, eventPublisher);
    }

    @Test
    void vasarlasCsokkentiASzabadJegyekSzamat() {
        PurchaseResult eredmeny = ticketService.vasarlas();

        assertThat(eredmeny.sikeres()).isTrue();
        assertThat(eredmeny.jegyAzonosito()).isNotNull();
        assertThat(ticketService.szabadJegyek()).isEqualTo(1);
    }

    @Test
    void vasarlasTicketReservedEsemenytKuld() {
        ticketService.vasarlas();

        assertThat(kiadottEsemenyek).hasSize(1);
        assertThat(kiadottEsemenyek.get(0)).isInstanceOf(TicketReserved.class);
    }

    @Test
    void nullaSzabadJegynelSikertelenAVasarlas() {
        ticketService.vasarlas();
        ticketService.vasarlas();

        PurchaseResult eredmeny = ticketService.vasarlas();

        assertThat(eredmeny.sikeres()).isFalse();
        assertThat(eredmeny.jegyAzonosito()).isNull();
        assertThat(ticketService.szabadJegyek()).isEqualTo(0);
    }

    @Test
    void sikertelenVasarlasTicketPurchaseFailedEsemenytKuld() {
        ticketService.vasarlas();
        ticketService.vasarlas();
        kiadottEsemenyek.clear();

        ticketService.vasarlas();

        assertThat(kiadottEsemenyek).hasSize(1);
        assertThat(kiadottEsemenyek.get(0)).isInstanceOf(TicketPurchaseFailed.class);
    }

    @Test
    void lemondasNoveliASzabadJegyekSzamat() {
        PurchaseResult vasarlas = ticketService.vasarlas();

        CancelResult eredmeny = ticketService.lemondas(vasarlas.jegyAzonosito());

        assertThat(eredmeny.sikeres()).isTrue();
        assertThat(ticketService.szabadJegyek()).isEqualTo(2);
    }

    @Test
    void lemondasTicketCancelledEsemenytKuld() {
        PurchaseResult vasarlas = ticketService.vasarlas();
        kiadottEsemenyek.clear();

        ticketService.lemondas(vasarlas.jegyAzonosito());

        assertThat(kiadottEsemenyek).hasSize(1);
        assertThat(kiadottEsemenyek.get(0)).isInstanceOf(TicketCancelled.class);
    }

    @Test
    void ismeretlenJegyLemondasaNemNoveliAKeszletet() {
        CancelResult eredmeny = ticketService.lemondas(UUID.randomUUID());

        assertThat(eredmeny.sikeres()).isFalse();
        assertThat(ticketService.szabadJegyek()).isEqualTo(2);
        assertThat(kiadottEsemenyek).isEmpty();
    }

    @Test
    void duplaLemondasNemNoveliTobbszorAKeszletet() {
        PurchaseResult vasarlas = ticketService.vasarlas();
        ticketService.lemondas(vasarlas.jegyAzonosito());

        CancelResult masodik = ticketService.lemondas(vasarlas.jegyAzonosito());

        assertThat(masodik.sikeres()).isFalse();
        assertThat(ticketService.szabadJegyek()).isEqualTo(2);
    }

    @Test
    void szabadJegyekSzamaNemLehetTobbMintAzOsszes() {
        ticketService.lemondas(UUID.randomUUID());
        PurchaseResult vasarlas = ticketService.vasarlas();
        ticketService.lemondas(vasarlas.jegyAzonosito());
        ticketService.lemondas(vasarlas.jegyAzonosito());

        assertThat(ticketService.szabadJegyek()).isLessThanOrEqualTo(ticketService.osszesJegy());
    }
}
