# TicketFlow – Megvalósítási terv

Spec: [SPEC.md](SPEC.md)

## Döntések

| Téma | Döntés | Indok |
|---|---|---|
| Nyelv | Java 21 | async/konkurencia eszközök, Alpine-barát |
| Keretrendszer | Spring Boot (web nélkül) | DI + beépített eseménykezelés, kevés boilerplate |
| Eseménykezelés | Spring `ApplicationEventPublisher` + `@EventListener`, saját `EventPublisher` wrapper | nem kell külső broker |
| Mediator | saját, egyszerű osztály (nem MediatR-szerű library) | átlátható, beadandóba jobban magyarázható |
| Konkurencia | `AtomicInteger` + `compareAndSet` ciklus | lock-mentes, bizonyíthatóan nem megy 0 alá |
| Perzisztencia | nincs, memóriában | egyszerűség |
| Build | Maven | elterjedt, egyszerű Docker multi-stage |

**Fontos Spring-részlet:** az `@EventListener` alapból szinkron, a publisher szálán fut. Ez egyszerűvé teszi a teszteket (determinisztikus), és a stressznél a kérés szálán fut le a teljes lánc. Ha aszinkron kell, `@Async` + `@EnableAsync` – de akkor a stressztesztnek várnia kell a listenerekre. Kezdésnek: szinkron.

## Projektstruktúra

```
TicketFlow/
├── pom.xml
├── Dockerfile
├── .dockerignore
├── README.md
├── SPEC.md / PLAN.md
└── src/
    ├── main/java/hu/ticketflow/
    │   ├── TicketFlowApplication.java
    │   ├── config/TicketProperties.java
    │   ├── domain/
    │   │   ├── TicketId (opcionális)
    │   │   └── event/ DomainEvent, TicketReserved, PaymentCompleted,
    │   │             TicketPurchaseFailed
    │   ├── application/
    │   │   ├── Mediator.java
    │   │   ├── command/ PurchaseTicketCommand, GetAvailableQuery, JegyTipus, PurchaseResult
    │   │   ├── TicketService.java
    │   │   └── EventPublisher.java
    │   ├── handler/ PaymentHandler, ConsoleLogger, StatsCollector
    │   └── ui/ ConsoleRunner.java (CommandLineRunner), StressRunner.java
    └── test/java/hu/ticketflow/
        ├── TicketServiceTest.java
        ├── MediatorTest.java
        └── StressTest.java
```

## Lépések

### 1. Projekt váz
- `pom.xml`: `spring-boot-starter-parent` 3.x, Java 21, függőségek: `spring-boot-starter`, `spring-boot-starter-test`.
- `TicketFlowApplication` + `application.properties` (web kikapcsolva).
- Ellenőrzés: `mvn -q package` lefut.

### 2. Domain események
- `DomainEvent` interface + 3 record (`TicketReserved` a jegy típusát is hordozza).
- Ellenőrzés: fordul.

### 3. TicketService (TDD)
- Teszt először: vásárlás csökkent, 0-nál sikertelen, VIP csak a VIP készletet csökkenti, elfogyott VIP mellett a normál jegy még vehető.
- Implementáció: két `AtomicInteger` (normál 100, VIP 50), mindkettő CAS ciklussal; eseményeket `EventPublisher`-en át publikál.
- Lemondás nincs.

### 4. EventPublisher + Mediator
- `EventPublisher` wrapper.
- `Mediator`: 2 `send` metódus (vásárlás, lekérdezés), a `JegyTipus`-t továbbadja a `TicketService`-nek.

### 5. Handlerek
- `StatsCollector` (`LongAdder`) – számlálók eseményenként.
- `PaymentHandler` – `TicketReserved` → `PaymentCompleted` (opcionális késleltetés konfigból).
- `ConsoleLogger` – kiírás; stressz alatt kikapcsolható (flag), különben a kimenet lassítja a mérést.

### 6. Konzolos UI
- `ConsoleRunner` menü: vásárlás, VIP vásárlás, készlet (normál + VIP), stressz, kilépés. Csak a `Mediator`-t használja.
- Ha nincs `System.console()`/stdin (pl. `--stress` mód), ne induljon a menü.

### 7. Stresszteszt
- `StressRunner` (újrahasznosítva a JUnit teszttel): `ExecutorService` fix szálszám, `CountDownLatch` start-kapu, 10 000 `mediator.send(new PurchaseTicketCommand(tipus))` (páros sorszám normál, páratlan VIP), `Future`-ök összegyűjtése típusonként.
- Assert-ek a SPEC 10. pontja szerint; idő és throughput kiírás.
- Plusz teszt: csak VIP kérések párhuzamosan, pontosan 50 sikeres, a készlet 0.

### 8. Docker
```dockerfile
FROM maven:3-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /app/target/ticketflow.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```
- `.dockerignore`: `target/`, `.git/`.
- Ellenőrzés: `docker build -t ticketflow .`, `docker run --rm -it ticketflow`, `docker run --rm ticketflow --stress`.

### 9. README
- Architektúra-ábra, build/run/teszt parancsok, stresszteszt eredmény példa, rövid indoklás (miért CAS, miért nincs broker).

## Kockázatok

| Kockázat | Megoldás |
|---|---|
| Szinkron listener lassítja a stresszt | logger kikapcsolható; mérés külön |
| Konzol input Dockerben | `-it` kell; `--stress` argumentum nem interaktív módhoz |
| Spring startup lassú Alpine-on | elfogadható; teszt kontextust egyszer indít |
| Flaky stresszteszt | latch + `Future.get()` timeouttal; determinisztikus elvárt értékek |

## Kész-definíció

SPEC.md 12. pontjának mind a 4 kritériuma teljesül.
