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

Spring Boot szerepe: DI (bean-ek), `ApplicationEventPublisher` / `@EventListener`, `CommandLineRunner` a konzolos belépési ponthoz, `@ConfigurationProperties` a konfighoz. Web réteg nincs.

## 3. Architektúra

```
Console UI (CommandLineRunner)
        │  parancs
        ▼
    Mediator            ← egyetlen belépési pont a service felé
        │
        ▼
  TicketService         ← jegykészlet, konkurens védelem
        │  esemény
        ▼
  EventPublisher        ← Spring ApplicationEventPublisher wrapper
        │
   ┌────┼──────────────┬────────────────────┐
   ▼    ▼              ▼                    ▼
TicketReserved  PaymentCompleted  TicketPurchaseFailed
   │ (listener-ek: PaymentHandler, ConsoleLogger, StatsCollector)
```

Szabály: a komponensek egymást nem hívják közvetlenül, csak a Mediatoron és eseményeken át kommunikálnak.

## 4. Komponensek

### 4.1 Mediator
- `PurchaseResult send(PurchaseTicketCommand cmd)` – a parancs `JegyTipus`-t (`NORMAL` / `VIP`) hordoz
- `int send(GetAvailableQuery q)` – a lekérdezés `JegyTipus`-t hordoz
- Feladata: parancs → megfelelő service-hívás. Nincs üzleti logika.

### 4.2 TicketService
- Állapot: két külön készlet, normál (100) és VIP (50); mindkettő `AtomicInteger`, CAS ciklussal.
- `vasarlas(tipus)`: ha van szabad jegy az adott típusból, csökkent és `TicketReserved`-et publikál; ha nincs, `TicketPurchaseFailed`-et. A két készlet független: elfogyott VIP mellett a normál jegy még vehető, és fordítva.
- Lemondás nincs.
- Invariáns: `0 ≤ szabad ≤ kezdeti készlet`, típusonként; eladott + szabad = kezdeti készlet.

### 4.3 EventPublisher
- `publish(DomainEvent e)` – vékony wrapper a Spring `ApplicationEventPublisher` fölött (csere-/tesztelhetőség).

### 4.4 Handlerek (listener)
- `PaymentHandler` – `TicketReserved`-re szimulált fizetést indít, majd `PaymentCompleted`-et publikál.
- `ConsoleLogger` – események kiírása (stressz módban kikapcsolható/összesített).
- `StatsCollector` – számlálók (sikeres, sikertelen, fizetett) `LongAdder`-rel; a stresszteszt ezt ellenőrzi.

## 5. Események (Java record-ok)

| Esemény | Mezők |
|---|---|
| `TicketReserved` | `UUID ticketId`, `JegyTipus tipus`, `Instant timestamp` |
| `PaymentCompleted` | `UUID ticketId`, `Instant timestamp` |
| `TicketPurchaseFailed` | `String reason`, `Instant timestamp` |

Közös interfész: `DomainEvent` (`Instant timestamp()`).

## 6. Folyamatok

**Sikeres vásárlás:** Console → Mediator → TicketService → (van jegy) → `TicketReserved` → `PaymentHandler` → `PaymentCompleted` → "Sikeres vásárlás".

**Elfogyott:** Console → Mediator → TicketService → (nincs jegy az adott típusból) → `TicketPurchaseFailed` → "Sikertelen vásárlás, nincs több <típus> jegy."

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

Ellenőrzések: normál sikeres == 100, normál sikertelen == 4 900, VIP sikeres == 50, VIP sikertelen == 4 950, mindkét készlet == 0, `TicketReserved` események száma == 150, `TicketPurchaseFailed` == 9 850, `PaymentCompleted` == 150. Külön teszt: csak VIP kérések párhuzamosan, pontosan 50 sikeres. A futási idő és throughput (kérés/mp) kiírásra kerül.

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
