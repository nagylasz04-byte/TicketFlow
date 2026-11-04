package hu.ticketflow.ui;

public record StressEredmeny(
        int osszesKeres,
        int normalSikeres,
        int normalSikertelen,
        int vipSikeres,
        int vipSikertelen,
        int fizetesek,
        int maradtNormal,
        int maradtVip,
        long idoMs) {

    public long keresMasodpercenkent() {
        return idoMs > 0 ? osszesKeres * 1000L / idoMs : 0;
    }
}
