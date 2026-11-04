package hu.ticketflow.ui;

import hu.ticketflow.application.Mediator;
import hu.ticketflow.domain.JegyTipus;
import hu.ticketflow.domain.event.AvailableTicketsRequested;
import hu.ticketflow.domain.event.DomainEvent;
import hu.ticketflow.domain.event.TicketPurchaseRequested;
import java.util.Arrays;
import java.util.Scanner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Konzolos menü: minden menüpont egy eseményt küld a Mediatornak
@Component
public class ConsoleRunner implements CommandLineRunner {

    private final Mediator mediator;
    private final StressRunner stressRunner;

    public ConsoleRunner(Mediator mediator, StressRunner stressRunner) {
        this.mediator = mediator;
        this.stressRunner = stressRunner;
    }

    @Override
    public void run(String... args) throws Exception {
        if (Arrays.asList(args).contains("--stress")) {
            stresszFuttatasa();
            return;
        }
        // terminál nélkül (pl. tesztek, docker -it nélkül) nem indítjuk a menüt
        if (System.console() == null) {
            return;
        }
        menuFuttatasa();
    }

    private void menuFuttatasa() throws Exception {
        Scanner bemenet = new Scanner(System.in);
        boolean fut = true;

        while (fut) {
            menuKiir();
            // ha nincs több bemenet (pl. nincs terminál), kilépünk
            if (!bemenet.hasNextLine()) {
                return;
            }
            String valasztas = bemenet.nextLine().trim();
            fut = valasztasKezelese(valasztas);
        }
    }

    private void menuKiir() {
        System.out.println();
        System.out.println("1. Jegy vásárlása");
        System.out.println("2. VIP jegy vásárlása");
        System.out.println("3. Jegyek száma");
        System.out.println("4. Stresszteszt futtatása");
        System.out.println("5. Kilépés");
        System.out.print("Választás: ");
    }

    // igazat ad vissza, ha a menü tovább fut
    private boolean valasztasKezelese(String valasztas) throws Exception {
        switch (valasztas) {
            case "1" -> kuld(new TicketPurchaseRequested(JegyTipus.NORMAL));
            case "2" -> kuld(new TicketPurchaseRequested(JegyTipus.VIP));
            case "3" -> kuld(new AvailableTicketsRequested());
            case "4" -> stresszFuttatasa();
            case "5" -> {
                System.out.println("Viszlát!");
                return false;
            }
            default -> System.out.println("Ismeretlen menüpont.");
        }
        return true;
    }

    // Az eredményt a ConsoleLogger írja ki az eseményekből. Itt csak megvárjuk,
    // hogy minden következmény lefusson, és csak utána rajzoljuk ki újra a menüt.
    private void kuld(DomainEvent esemeny) throws Exception {
        mediator.send(esemeny).get();
    }

    private void stresszFuttatasa() throws Exception {
        System.out.println("Stresszteszt indul...");
        StressEredmeny eredmeny = stressRunner.futtat();
        stressRunner.kiir(eredmeny);
    }
}
