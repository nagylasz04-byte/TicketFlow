package hu.ticketflow.ui;

public record StressEredmeny(
        int osszesKeres,
        int sikeres,
        int sikertelen,
        int maradtJegy,
        long idoMs,
        long kerescMasodpercenkent) {
}
