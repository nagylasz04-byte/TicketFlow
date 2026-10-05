package hu.ticketflow.ui;

import hu.ticketflow.application.Mediator;
import hu.ticketflow.application.command.CancelResult;
import hu.ticketflow.application.command.CancelTicketCommand;
import hu.ticketflow.application.command.GetAvailableQuery;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.application.command.PurchaseTicketCommand;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Konzolos menü, csak a Mediatoron keresztül dolgozik
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
            fut = valasztasKezelese(valasztas, bemenet);
        }
    }

    private void menuKiir() {
        System.out.println();
        System.out.println("1. Jegy vásárlása");
        System.out.println("2. Jegyek száma");
        System.out.println("3. Jegy lemondása");
        System.out.println("4. Stresszteszt futtatása");
        System.out.println("5. Kilépés");
        System.out.print("Választás: ");
    }

    // igazat ad vissza, ha a menü tovább fut
    private boolean valasztasKezelese(String valasztas, Scanner bemenet) throws Exception {
        switch (valasztas) {
            case "1" -> vasarlas();
            case "2" -> jegyekSzama();
            case "3" -> lemondas(bemenet);
            case "4" -> stresszFuttatasa();
            case "5" -> {
                System.out.println("Viszlát!");
                return false;
            }
            default -> System.out.println("Ismeretlen menüpont.");
        }
        return true;
    }

    private void vasarlas() {
        PurchaseResult eredmeny = mediator.send(new PurchaseTicketCommand());
        if (eredmeny.sikeres()) {
            System.out.println(eredmeny.uzenet() + " Jegy azonosító: " + eredmeny.jegyAzonosito());
        } else {
            System.out.println(eredmeny.uzenet());
        }
    }

    private void jegyekSzama() {
        int szabad = mediator.send(new GetAvailableQuery());
        System.out.println("Szabad jegyek száma: " + szabad);
    }

    private void lemondas(Scanner bemenet) {
        System.out.print("Add meg a jegy azonosítóját: ");
        if (!bemenet.hasNextLine()) {
            return;
        }
        String szoveg = bemenet.nextLine().trim();

        try {
            UUID jegyAzonosito = UUID.fromString(szoveg);
            CancelResult eredmeny = mediator.send(new CancelTicketCommand(jegyAzonosito));
            System.out.println(eredmeny.uzenet());
        } catch (IllegalArgumentException hiba) {
            System.out.println("Hibás jegy azonosító.");
        }
    }

    private void stresszFuttatasa() throws Exception {
        System.out.println("Stresszteszt indul...");
        StressEredmeny eredmeny = stressRunner.futtat();
        stressRunner.kiir(eredmeny);
    }
}
