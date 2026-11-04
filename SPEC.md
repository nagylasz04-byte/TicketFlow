# TicketFlow – Specifikáció

## 1. Cél

Egyszerű, terminálos, eseményalapú jegyvásárló rendszer egyetemi beadandóhoz. A hangsúly:

- event-driven működés
- Mediator → Service réteg
- konkurens jegyvásárlás (nincs túladás)
- Docker / Alpine Linux
- stresszteszt

**Nem cél:** adatbázis, webes frontend, REST API, Kafka/RabbitMQ, autentikáció, valódi fizetés, mikroszolgáltatások.

## 2. Technológia

| Elem | Választás |
|---|---|
| Nyelv | Java 21 |
| Keretrendszer | Spring Boot 3.x (web nélkül: `spring-boot-starter`) |
| Build | Maven |
| Teszt | JUnit 5 + Spring Boot Test, AssertJ |
| Konténer | Docker multi-stage, `eclipse-temurin:21-jre-alpine` |

Spring Boot szerepe: DI (bean-ek), `ApplicationEventPublisher` / `@EventListener` (visszatérési értékkel láncolva), `CommandLineRunner` a konzolos belépési ponthoz, `@ConfigurationProperties` a konfighoz. Web réteg nincs.

## 3. Architektúra

```
ConsoleRunner / StressRunner
        │  mediator.send(esemény)
        ▼
    Mediator            ← egy sor + egy eseményszál, sorban kézbesít
        │
        ▼
  @EventListener kezelők (a visszaadott esemény automatikusan továbbmegy)

  TicketPurchaseRequested   → TicketService  → TicketReserved / TicketPurchaseFailed
  TicketReserved            → PaymentHandler → PaymentCompleted
  AvailableTicketsRequested → TicketService  → AvailableTicketsReported
  minden esemény            → ConsoleLogger, StatsCollector
```

Szabály: a komponensek egymást nem hívják közvetlenül. Minden változás eseményként, a Mediatoron keresztül lép be a rendszerbe.

## 4. Komponensek

### 4.1 Mediator
- `Future<?> send(DomainEvent e)` – sorba állítja az eseményt, egyetlen (háttér)szál kézbesíti a Spring `ApplicationEventPublisher`-rel.
- A `Future` akkor kész, amikor az esemény és minden belőle következő esemény lefutott.
- Üzleti logika nincs benne.

### 4.2 TicketService
- Állapot: két külön készlet, normál (100) és VIP (50), sima `int`. Csak az eseményszál éri el, ezért zár nem kell.
- `TicketPurchaseRequested` → ha van szabad jegy az adott típusból, csökkent és `TicketReserved`-et ad vissza, ha nincs, `TicketPurchaseFailed`-et. A két készlet független.
- `AvailableTicketsRequested` → `AvailableTicketsReported(normal, vip)`.
- Lemondás nincs.
- Invariáns: `0 ≤ szabad ≤ kezdeti készlet`, típusonként; eladott + szabad = kezdeti készlet.

### 4.3 Handlerek (listener)
- `PaymentHandler` – `TicketReserved`-re szimulált fizetés, `PaymentCompleted`-et ad vissza.
- `ConsoleLogger` – események kiírása; `StressTestStarted` és `StressTestFinished` között hallgat.
- `StatsCollector` – típusonkénti számlálók (sikeres, sikertelen, fizetés, maradék készlet); `StressTestStarted`-re nulláz. A stresszteszt a végén ebből olvas.

## 5. Események (Java record-ok)

| Esemény | Mezők | Ki kezeli |
|---|---|---|
| `TicketPurchaseRequested` | `JegyTipus tipus` | TicketService |
| `TicketReserved` | `UUID ticketId`, `JegyTipus tipus` | PaymentHandler, ConsoleLogger, StatsCollector |
| `TicketPurchaseFailed` | `JegyTipus tipus`, `String reason` | ConsoleLogger, StatsCollector |
| `PaymentCompleted` | `UUID ticketId`, `JegyTipus tipus` | ConsoleLogger, StatsCollector |
| `AvailableTicketsRequested` | – | TicketService |
| `AvailableTicketsReported` | `int normal`, `int vip` | ConsoleLogger, StatsCollector |
| `StressTestStarted` | – | ConsoleLogger, StatsCollector |
| `StressTestFinished` | – | ConsoleLogger |

Közös jelölő interfész: `DomainEvent`.

## 6. Folyamatok

**Sikeres vásárlás:** Console → `TicketPurchaseRequested` → Mediator → TicketService → (van jegy) → `TicketReserved` → PaymentHandler → `PaymentCompleted` → ConsoleLogger: „Fizetés teljesítve, sikeres vásárlás”.

**Elfogyott:** Console → `TicketPurchaseRequested` → Mediator → TicketService → (nincs jegy az adott típusból) → `TicketPurchaseFailed` → ConsoleLogger: „Sikertelen vásárlás: Nincs több <típus> jegy”.

## 7. Terminál UI

```
1. Jegy vásárlása
2. VIP jegy vásárlása
3. Jegyek száma (normál és VIP)
4. Stresszteszt futtatása
5. Kilépés
```

Alapértelmezett készlet: 100 normál jegy (`ticketflow.tickets=100`) és 50 VIP jegy (`ticketflow.vip-tickets=50`).

## 8. Konfiguráció (`application.properties`)

```
ticketflow.tickets=100
ticketflow.vip-tickets=50
ticketflow.stress.requests=10000
ticketflow.stress.threads=100
ticketflow.payment.delay-ms=0
spring.main.web-application-type=none
```

## 9. Konkurencia-követelmények

- Típusonként soha nem lehet több sikeres vásárlás, mint a kezdeti jegyszám (100 normál, 50 VIP).
- Egyik készlet sem lehet negatív, és a két készlet nem hat egymásra.
- Az események száma konzisztens a kimenetekkel (minden kérés pontosan egy `TicketReserved` vagy `TicketPurchaseFailed` eseményt eredményez).
- Megoldás: a Mediator egyetlen eseményszála sorosítja a párhuzamos kéréseket, így a készletet egyszerre mindig csak egy szál módosítja.

## 10. Stresszteszt

Bemenet: 100 normál és 50 VIP jegy, 10 000 párhuzamos vásárlási kérés (felük normál, felük VIP, váltakozva), 100 szálú `ExecutorService`, `CountDownLatch` start-kapuval (egyszerre indulnak).

Elvárt eredmény:

```
Összes kérés:      10 000
Normál sikeres:       100
Normál sikertelen:  4 900
VIP sikeres:           50
VIP sikertelen:     4 950
Maradt normál:          0
Maradt VIP:             0
```

Ellenőrzések (a `StatsCollector` eseményszámlálóiból): normál sikeres == 100, normál sikertelen == 4 900, VIP sikeres == 50, VIP sikertelen == 4 950, mindkét készlet == 0, `PaymentCompleted` == 150. Külön teszt: csak VIP kérések párhuzamosan, pontosan 50 sikeres. A futási idő és throughput (kérés/mp) kiírásra kerül.

Futtatás: JUnit `StressTest` (Maven) és a 4-es menüpont / `--stress` argumentum a konténerben.

## 11. Docker

- Multi-stage: `maven:3-eclipse-temurin-21-alpine` (build) → `eclipse-temurin:21-jre-alpine` (runtime).
- Futtatás interaktívan: `docker run --rm -it ticketflow`
- Stresszteszt nem interaktívan: `docker run --rm ticketflow --stress`
- Nem root felhasználó a runtime image-ben.

## 12. Elfogadási kritériumok

1. `mvn test` zöld, benne a stresszteszt.
2. `docker build` és `docker run` Alpine-alapú image-en működik.
3. A menü minden pontja működik, események láthatók a kimeneten.
4. A README leírja a build/run/teszt lépéseket.
