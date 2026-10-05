package hu.ticketflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// A mezőnevek a properties kulcsokhoz igazodnak (Spring ezek alapján köti be)
@ConfigurationProperties(prefix = "ticketflow")
public class TicketProperties {

    private int tickets = 100;
    private Stress stress = new Stress();
    private Payment payment = new Payment();

    public int getTickets() {
        return tickets;
    }

    public void setTickets(int tickets) {
        this.tickets = tickets;
    }

    public Stress getStress() {
        return stress;
    }

    public void setStress(Stress stress) {
        this.stress = stress;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public static class Stress {

        private int requests = 10000;
        private int threads = 100;

        public int getRequests() {
            return requests;
        }

        public void setRequests(int requests) {
            this.requests = requests;
        }

        public int getThreads() {
            return threads;
        }

        public void setThreads(int threads) {
            this.threads = threads;
        }
    }

    public static class Payment {

        private long delayMs = 0;

        public long getDelayMs() {
            return delayMs;
        }

        public void setDelayMs(long delayMs) {
            this.delayMs = delayMs;
        }
    }
}
