# TicketFlow

Egyszerű, terminálos, eseményalapú jegyvásárló rendszer (Java 21, Spring Boot, Maven, Docker).
Konkurens jegyvásárlás túladás nélkül, stressztesztel.

## Architektúra

```
Console UI (CommandLineRunner)
        |  parancs
        v
    Mediator            <- egyetlen belépési pont a service felé
        |
        v
  TicketService         <- jegykészlet, konkurens védelem
        |  esemény
        v
  EventPublisher        <- Spring ApplicationEventPublisher wrapper
        |
   +----+--------------+--------------------+
   v    v              v                    v
TicketReserved  PaymentCompleted  TicketCancelled  TicketPurchaseFailed
   | (listener-ek: PaymentHandler, ConsoleLogger, StatsCollector)
```

A komponensek egymást nem hívják közvetlenül, csak a Mediatoron és eseményeken át kommunikálnak.

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

Ebben benne van a `StressTest` (10 000 párhuzamos vásárlás 100 jegyre) és a vegyes vásárlás + lemondás teszt.

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
ticketflow.stress.requests=10000
ticketflow.stress.threads=100
ticketflow.payment.delay-ms=0
```

## Példa stressz kimenet

Az értékek (idő, throughput) gépenként eltérnek, a többi mindig ugyanennyi.

```
Összes kérés:     10000
Sikeres vásárlás: 100
Sikertelen:       9900
Maradt jegy:      0
Futási idő:       350 ms
Throughput:       28571 kérés/mp
Események:        foglalás=100, sikertelen=9900, fizetés=100
```

## Miért így?

- **Miért CAS (`AtomicInteger.compareAndSet`)?** Lock nélkül is biztos, hogy a készlet nem megy 0 alá: a csökkentés csak akkor sikerül, ha közben senki nem módosította az értéket, különben újrapróbáljuk.
- **Miért nincs broker (Kafka, RabbitMQ)?** Egy folyamaton belül dolgozunk, ehhez elég a Spring beépített eseménykezelése. Egy külső szolgáltatás csak bonyolítaná a beadandót.
- **Miért szinkron a listener?** Az `@EventListener` alapból a publikáló szálon fut, így a tesztek determinisztikusak: mire a vásárlás visszatér, minden esemény feldolgozva. Ezért a stressztesztben a számlálók azonnal ellenőrizhetők.
- **Miért saját Mediator?** Kicsi és átlátható: parancsot a megfelelő service-hívásra képez le, üzleti logika nincs benne.
