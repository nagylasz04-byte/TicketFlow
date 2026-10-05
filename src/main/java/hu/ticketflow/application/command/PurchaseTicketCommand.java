package hu.ticketflow.application.command;

public record PurchaseTicketCommand(JegyTipus tipus) {

    public PurchaseTicketCommand() {
        this(JegyTipus.NORMAL);
    }
}
