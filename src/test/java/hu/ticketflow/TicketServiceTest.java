package hu.ticketflow;

import static org.assertj.core.api.Assertions.assertThat;

import hu.ticketflow.application.TicketService;
import hu.ticketflow.domain.JegyTipus;
import hu.ticketflow.domain.event.AvailableTicketsReported;
import hu.ticketflow.domain.event.AvailableTicketsRequested;
import hu.ticketflow.domain.event.DomainEvent;
import hu.ticketflow.domain.event.TicketPurchaseFailed;
import hu.ticketflow.domain.event.TicketPurchaseRequested;
import hu.ticketflow.domain.event.TicketReserved;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Spring nélkül: közvetlenül meghívjuk a kezelőt, és megnézzük, milyen eseményt ad vissza
class TicketServiceTest {

    private TicketService ticketService;

    @BeforeEach
    void elokeszites() {
        ticketService = new TicketService(2, 1);
    }

    @Test
    void vasarlasTicketReservedEsemenytAd() {
        DomainEvent eredmeny = vasarol(JegyTipus.NORMAL);

        assertThat(eredmeny).isInstanceOf(TicketReserved.class);
        assertThat(((TicketReserved) eredmeny).ticketId()).isNotNull();
    }

    @Test
    void vasarlasCsokkentiASzabadJegyekSzamat() {
        vasarol(JegyTipus.NORMAL);

        assertThat(keszlet().normal()).isEqualTo(1);
    }

    @Test
    void nullaSzabadJegynelTicketPurchaseFailedEsemenytAd() {
        vasarol(JegyTipus.NORMAL);
        vasarol(JegyTipus.NORMAL);

        assertThat(vasarol(JegyTipus.NORMAL)).isInstanceOf(TicketPurchaseFailed.class);
        assertThat(keszlet().normal()).isZero();
    }

    @Test
    void vipVasarlasCsakAVipKeszletetCsokkenti() {
        DomainEvent eredmeny = vasarol(JegyTipus.VIP);

        assertThat(((TicketReserved) eredmeny).tipus()).isEqualTo(JegyTipus.VIP);
        assertThat(keszlet().vip()).isZero();
        assertThat(keszlet().normal()).isEqualTo(2);
    }

    @Test
    void elfogyottVipMellettMegVanNormalJegy() {
        vasarol(JegyTipus.VIP);

        assertThat(vasarol(JegyTipus.VIP)).isInstanceOf(TicketPurchaseFailed.class);
        assertThat(vasarol(JegyTipus.NORMAL)).isInstanceOf(TicketReserved.class);
    }

    private DomainEvent vasarol(JegyTipus tipus) {
        return ticketService.vasarlas(new TicketPurchaseRequested(tipus));
    }

    private AvailableTicketsReported keszlet() {
        return ticketService.keszlet(new AvailableTicketsRequested());
    }
}
