# TicketFlow

Egyszerű, terminálos, eseményalapú jegyvásárló rendszer (Java 21, Spring Boot, Maven, Docker).
Konkurens jegyvásárlás túladás nélkül, stressztesztel.

## Architektúra

Minden kommunikáció esemény. A komponensek nem hívják egymást, csak eseményt küldenek és fogadnak.

```
ConsoleRunner / StressRunner
        |  mediator.send(esemény)
        v
    Mediator     <- egy sor + egy eseményszál: az eseményeket egyenként, érkezési sorrendben kézbesíti
        |
        v
  @EventListener kezelők (amit egy kezelő visszaad, az a következő esemény)

  TicketPurchaseRequested   --> TicketService  --> TicketReserved vagy TicketPurchaseFailed
  TicketReserved            --> PaymentHandler --> PaymentCompleted
  AvailableTicketsRequested --> TicketService  --> AvailableTicketsReported
  StressTestStarted / StressTestFinished       --> számlálók nullázása, kiírás ki/be

  minden esemény --> ConsoleLogger (kiírja), StatsCollector (megszámolja)
```

Egy vásárlás útja:

1. A menü elküldi a `TicketPurchaseRequested(NORMAL)` eseményt a Mediatornak.
2. A Mediator beteszi a sorba, az eseményszál átadja a `TicketService`-nek.
3. A `TicketService` megnézi a készletet, és visszaad egy `TicketReserved` vagy `TicketPurchaseFailed` eseményt.
4. `TicketReserved` esetén a `PaymentHandler` visszaad egy `PaymentCompleted` eseményt.
5. A `ConsoleLogger` mindegyiket kiírja, a `StatsCollector` mindegyiket megszámolja.
6. A `send()` által visszaadott `Future` ekkor lesz kész, a menü csak ezután rajzolódik ki újra.

## Build és futtatás

Szükséges: Java 21 és Maven.

```
mvn package
java -jar target/ticketflow.jar            # interaktív menü
java -jar target/ticketflow.jar --stress   # stresszteszt, menü nélkül
```

## Tesztek

```
mvn test
```

Ebben benne van a `StressTest` (10 000 párhuzamos vásárlás: 100 normál és 50 VIP jegy, a kérések felével-felével) és a párhuzamos VIP vásárlás teszt (50 VIP jegy).

## Docker

```
docker build -t ticketflow .
docker run --rm -it ticketflow             # interaktív menü (-it kell)
docker run --rm ticketflow --stress        # stresszteszt
```

Az image Alpine alapú, a program nem root felhasználóval fut.

## Beállítások

Az `application.properties` fájlban:

```
ticketflow.tickets=100
ticketflow.vip-tickets=50
ticketflow.stress.requests=10000
ticketflow.stress.threads=100
ticketflow.payment.delay-ms=0
```

## Példa stressz kimenet

Az értékek (idő, throughput) gépenként eltérnek, a többi mindig ugyanennyi.

```
Összes kérés:      10000
Normál sikeres:    100
Normál sikertelen: 4900
VIP sikeres:       50
VIP sikertelen:    4950
Fizetések:         150
Maradt normál:     0
Maradt VIP:        0
Futási idő:        190 ms
Throughput:        52631 kérés/mp
```

## Miért így?

- **Miért egyetlen eseményszál?** Olyan, mint egy pénztársor: akárhányan állnak be egyszerre, a pénztáros egyszerre csak egy vevővel foglalkozik. A 100 szál 10 000 kérése a Mediator sorába kerül, és a készletet mindig csak az eseményszál módosítja. Ezért elég egy sima `int`, nem kell zár vagy `AtomicInteger`, és túladás sem lehetséges.
- **Miért adnak vissza eseményt a kezelők?** Ha egy `@EventListener` metódus nem `void`, a Spring a visszaadott objektumot azonnal továbbküldi eseményként, ugyanazon a szálon. Így a kezelőnek nem kell tudnia, ki dolgozza fel az eredményét.
- **Honnan tudja a menü, hogy kész a vásárlás?** A `mediator.send()` egy `Future`-t ad vissza, ami akkor lesz kész, amikor az esemény és minden belőle következő esemény lefutott. A `.get()` ezt várja meg, várakozó ciklus (polling) nincs.
- **Miért van `@Order` a `ConsoleLogger`-en?** A `TicketReserved`-et a `PaymentHandler` is megkapja, és az ő `PaymentCompleted` eseménye azonnal lefut. A `@Order` miatt a logger kapja meg elsőként a foglalást, így a kiírás sorrendje: foglalás, utána fizetés.
- **Miért nincs broker (Kafka, RabbitMQ)?** Egy folyamaton belül dolgozunk, ehhez elég a Spring beépített eseménykezelése. Egy külső szolgáltatás csak bonyolítaná a beadandót.
- **Mire figyelj a `payment.delay-ms`-nél?** A fizetés is az eseményszálon fut, így a késleltetés az egész sort lassítja (150 fizetés × késleltetés).
