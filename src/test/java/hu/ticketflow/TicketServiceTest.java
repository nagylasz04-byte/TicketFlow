package hu.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ticketflow.application.EventPublisher;
import hu.ticketflow.application.TicketService;
import hu.ticketflow.application.command.JegyTipus;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketReserved;
import java.util.ArrayList;
import java.util.List;
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
        ticketService = new TicketService(2, 1, eventPublisher);
    }

    @Test
    void vasarlasCsokkentiASzabadJegyekSzamat() {
        PurchaseResult eredmeny = ticketService.vasarlas(JegyTipus.NORMAL);

        assertThat(eredmeny.sikeres()).isTrue();
        assertThat(eredmeny.jegyAzonosito()).isNotNull();
        assertThat(ticketService.szabadJegyek(JegyTipus.NORMAL)).isEqualTo(1);
    }

    @Test
    void vasarlasTicketReservedEsemenytKuld() {
        ticketService.vasarlas(JegyTipus.NORMAL);

        assertThat(kiadottEsemenyek).hasSize(1);
        assertThat(kiadottEsemenyek.get(0)).isInstanceOf(TicketReserved.class);
    }

    @Test
    void nullaSzabadJegynelSikertelenAVasarlas() {
        ticketService.vasarlas(JegyTipus.NORMAL);
        ticketService.vasarlas(JegyTipus.NORMAL);

        PurchaseResult eredmeny = ticketService.vasarlas(JegyTipus.NORMAL);

        assertThat(eredmeny.sikeres()).isFalse();
        assertThat(eredmeny.jegyAzonosito()).isNull();
        assertThat(ticketService.szabadJegyek(JegyTipus.NORMAL)).isZero();
    }

    @Test
    void sikertelenVasarlasTicketPurchaseFailedEsemenytKuld() {
        ticketService.vasarlas(JegyTipus.VIP);
        kiadottEsemenyek.clear();

        ticketService.vasarlas(JegyTipus.VIP);

        assertThat(kiadottEsemenyek).hasSize(1);
        assertThat(kiadottEsemenyek.get(0)).isInstanceOf(TicketPurchaseFailed.class);
    }

    @Test
    void vipVasarlasCsakAVipKeszletetCsokkenti() {
        PurchaseResult eredmeny = ticketService.vasarlas(JegyTipus.VIP);

        assertThat(eredmeny.sikeres()).isTrue();
        assertThat(ticketService.szabadJegyek(JegyTipus.VIP)).isZero();
        assertThat(ticketService.szabadJegyek(JegyTipus.NORMAL)).isEqualTo(2);
        assertThat(((TicketReserved) kiadottEsemenyek.get(0)).tipus()).isEqualTo(JegyTipus.VIP);
    }

    @Test
    void elfogyottVipMellettMegVanNormalJegy() {
        ticketService.vasarlas(JegyTipus.VIP);

        assertThat(ticketService.vasarlas(JegyTipus.VIP).sikeres()).isFalse();
        assertThat(ticketService.vasarlas(JegyTipus.NORMAL).sikeres()).isTrue();
    }
}
