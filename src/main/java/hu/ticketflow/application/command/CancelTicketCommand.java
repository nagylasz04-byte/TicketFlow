package hu.ticketflow.application.command;

import java.util.UUID;

public record CancelTicketCommand(UUID jegyAzonosito) {
}
