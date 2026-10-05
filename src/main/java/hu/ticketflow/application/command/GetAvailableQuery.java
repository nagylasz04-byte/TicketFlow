package hu.ticketflow.application.command;

public record GetAvailableQuery(JegyTipus tipus) {

    public GetAvailableQuery() {
        this(JegyTipus.NORMAL);
    }
}
