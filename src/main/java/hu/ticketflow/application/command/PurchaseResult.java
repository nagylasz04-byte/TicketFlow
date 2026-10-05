package hu.ticketflow.application.command;

import java.util.UUID;

// Sikertelen vásárlásnál a jegyAzonosito null
public record PurchaseResult(boolean sikeres, UUID jegyAzonosito, String uzenet) {
}
