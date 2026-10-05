package hu.ticketflow.application;

import hu.ticketflow.application.command.GetAvailableQuery;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.application.command.PurchaseTicketCommand;
import org.springframework.stereotype.Component;

// Csak továbbítja a kéréseket a service-nek, üzleti logika nincs benne
@Component
public class Mediator {

    private final TicketService ticketService;

    public Mediator(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public PurchaseResult send(PurchaseTicketCommand parancs) {
        return ticketService.vasarlas(parancs.tipus());
    }

    public int send(GetAvailableQuery lekerdezes) {
        return ticketService.szabadJegyek(lekerdezes.tipus());
    }
}
