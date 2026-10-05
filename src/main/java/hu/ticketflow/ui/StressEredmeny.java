package hu.ticketflow.ui;

public record StressEredmeny(
        int osszesKeres,
        int normalSikeres,
        int normalSikertelen,
        int vipSikeres,
        int vipSikertelen,
        int maradtNormal,
        int maradtVip,
        long idoMs,
        long kerescMasodpercenkent) {

    public int sikeres() {
        return normalSikeres + vipSikeres;
    }

    public int sikertelen() {
        return normalSikertelen + vipSikertelen;
    }
}
