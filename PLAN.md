# TicketFlow – Megvalósítási terv

Spec: [SPEC.md](SPEC.md)

## Döntések

| Téma | Döntés | Indok |
|---|---|---|
| Nyelv | Java 21 | async/konkurencia eszközök, Alpine-barát |
| Keretrendszer | Spring Boot (web nélkül) | DI + beépített eseménykezelés, kevés boilerplate |
| Eseménykezelés | Spring `@EventListener`, a kezelő visszatérési értéke a következő esemény | nem kell külső broker, nincs publisher-függőség a kezelőkben |
| Mediator | saját osztály: eseménysor + egyetlen eseményszál | átlátható, ez az egyetlen belépési pont |
| Konkurencia | egyetlen eseményszál sorosítja a kéréseket | zár nélkül sem lehet túladás, egyszerű `int` elég |
| Perzisztencia | nincs, memóriában | egyszerűség |
| Build | Maven | elterjedt, egyszerű Docker multi-stage |

**Fontos Spring-részlet:** az `@EventListener` szinkron, a publikáló szálon fut. A Mediator ezért egyetlen saját szálról publikál, így minden kezelő ezen az egy szálon fut, sorban. Ha egy kezelő eseményt ad vissza, a Spring azonnal továbbküldi, a lánc tehát egy `send()` alatt végigfut, és a visszakapott `Future` ennek a végét jelzi.

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
    │   │   ├── JegyTipus.java
    │   │   └── event/ DomainEvent, TicketPurchaseRequested, TicketReserved,
    │   │             TicketPurchaseFailed, PaymentCompleted,
    │   │             AvailableTicketsRequested, AvailableTicketsReported,
    │   │             StressTestStarted, StressTestFinished
    │   ├── application/
    │   │   ├── Mediator.java
    │   │   └── TicketService.java
    │   ├── handler/ PaymentHandler, ConsoleLogger, StatsCollector
    │   └── ui/ ConsoleRunner.java (CommandLineRunner), StressRunner.java
    └── test/java/hu/ticketflow/
        ├── TicketServiceTest.java
        ├── HandlerTest.java
        ├── MediatorTest.java
        └── StressTest.java
```

## Lépések

### 1. Projekt váz
- `pom.xml`: `spring-boot-starter-parent` 3.x, Java 21, függőségek: `spring-boot-starter`, `spring-boot-starter-test`.
- `TicketFlowApplication` + `application.properties` (web kikapcsolva).
- Ellenőrzés: `mvn -q package` lefut.

### 2. Domain események
- `DomainEvent` jelölő interfész + record-ok a kérésekre és a következményekre (SPEC 5. pont).
- Ellenőrzés: fordul.

### 3. TicketService (TDD)
- Teszt először: vásárlás csökkent, 0-nál sikertelen, VIP csak a VIP készletet csökkenti, elfogyott VIP mellett a normál jegy még vehető.
- Implementáció: két sima `int` (normál 100, VIP 50); a `@EventListener` metódusok a következő eseményt adják vissza.
- Lemondás nincs.

### 4. Mediator
- `Future<?> send(DomainEvent)`: egyszálú `ExecutorService` (háttérszál), ami a Spring `publishEvent`-et hívja.

### 5. Handlerek
- `StatsCollector` – típusonkénti számlálók és maradék készlet, `StressTestStarted`-re nulláz.
- `PaymentHandler` – `TicketReserved` → `PaymentCompleted` (opcionális késleltetés konfigból).
- `ConsoleLogger` – kiírás; `StressTestStarted` és `StressTestFinished` között hallgat.

### 6. Konzolos UI
- `ConsoleRunner` menü: vásárlás, VIP vásárlás, készlet (normál + VIP), stressz, kilépés. Csak a `Mediator`-t használja.
- Ha nincs `System.console()`/stdin (pl. `--stress` mód), ne induljon a menü.

### 7. Stresszteszt
- `StressRunner` (újrahasznosítva a JUnit teszttel): `ExecutorService` fix szálszám, `CountDownLatch` start-kapu, 10 000 `mediator.send(new TicketPurchaseRequested(tipus))` (páros sorszám normál, páratlan VIP), a `Future`-ök megvárása után az eredmény a `StatsCollector` számlálóiból.
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
- Architektúra-ábra, build/run/teszt parancsok, stresszteszt eredmény példa, rövid indoklás (miért egy eseményszál, miért nincs broker).

## Kockázatok

| Kockázat | Megoldás |
|---|---|
| Szinkron listener lassítja a stresszt | logger kikapcsolható; mérés külön |
| Konzol input Dockerben | `-it` kell; `--stress` argumentum nem interaktív módhoz |
| Spring startup lassú Alpine-on | elfogadható; teszt kontextust egyszer indít |
| Flaky stresszteszt | latch + `Future.get()` timeouttal; determinisztikus elvárt értékek |

## Kész-definíció

SPEC.md 12. pontjának mind a 4 kritériuma teljesül.
