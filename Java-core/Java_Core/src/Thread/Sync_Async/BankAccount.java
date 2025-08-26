package Thread.Sync_Async;

public class BankAccount {
    private double balance;
    private String name;

    private final Object lockName = new Object();
    private final Object lockBalance = new Object();

    public BankAccount(String name, double balance) {
        this.balance = balance;
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }


    public String getName() {
        return name;
    }

    public  void setName(String name) {
        synchronized (lockName) {
            this.name = name;
            System.out.println("Updated Name = " + this.name);
        }

    }

    public synchronized void withdraw(double amount) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        double originBalance = balance;

        if (amount <= balance) {
            balance -= amount;
            System.out.printf("STARTING BALANCE: %.0f, WITHDRAW (%.0f)" +
                    ": NEW BALANCE = %.0f!", originBalance, amount, balance);
        } else {
            System.out.printf("STARTING BALANCE: %.0f, WITHDRAW (%.0f)" +
                    ": INSUFFICIENT FUNDS!", originBalance, amount);
        }
        System.out.println();
    }

    public  void deposit(double amount) {
        try {
            System.out.println("Deposit - Talking to the teller at the bank ... ");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        synchronized (lockBalance) {
            double originBalance = balance;
            balance += amount;

            System.out.printf("STARTING BALANCE: %.0f, DEPOSIT (%.0f)" +
                    ": NEW BALANCE = %.0f!", originBalance, amount, balance);
            addPromoDollars(amount);
            System.out.println();
        }
    }

    private void addPromoDollars(double amount) {
        if (amount >= 5000) {
            synchronized (lockBalance) {
                System.out.println("Congratulations, you earn a promotional deposit");
                balance += 25;
            }
        }
    }
}
