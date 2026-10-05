package hu.ticketflow.ui;

import hu.ticketflow.application.Mediator;
import hu.ticketflow.application.command.GetAvailableQuery;
import hu.ticketflow.application.command.JegyTipus;
import hu.ticketflow.application.command.PurchaseResult;
import hu.ticketflow.application.command.PurchaseTicketCommand;
import java.util.Arrays;
import java.util.Scanner;
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
            case "1" -> vasarlas(JegyTipus.NORMAL);
            case "2" -> vasarlas(JegyTipus.VIP);
            case "3" -> jegyekSzama();
            case "4" -> stresszFuttatasa();
            case "5" -> {
                System.out.println("Viszlát!");
                return false;
            }
            default -> System.out.println("Ismeretlen menüpont.");
        }
        return true;
    }

    private void vasarlas(JegyTipus tipus) {
        PurchaseResult eredmeny = mediator.send(new PurchaseTicketCommand(tipus));
        if (eredmeny.sikeres()) {
            System.out.println(eredmeny.uzenet() + " Jegy azonosító: " + eredmeny.jegyAzonosito());
        } else {
            System.out.println(eredmeny.uzenet());
        }
    }

    private void jegyekSzama() {
        int normal = mediator.send(new GetAvailableQuery(JegyTipus.NORMAL));
        int vip = mediator.send(new GetAvailableQuery(JegyTipus.VIP));
        System.out.println("Szabad jegyek száma: " + normal + " (VIP: " + vip + ")");
    }

    private void stresszFuttatasa() throws Exception {
        System.out.println("Stresszteszt indul...");
        StressEredmeny eredmeny = stressRunner.futtat();
        stressRunner.kiir(eredmeny);
    }
}
