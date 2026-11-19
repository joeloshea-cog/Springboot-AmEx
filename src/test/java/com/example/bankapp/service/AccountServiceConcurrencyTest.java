package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceConcurrencyTest {

    // Below the default Hikari pool size (10): each open-in-view thread holds a connection at the barrier
    private static final int THREADS = 8;
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void concurrentWithdrawalsCannotOverdraw() throws Exception {
        String username = newAccountWithBalance(HUNDRED);

        int succeeded = runConcurrently(account -> accountService.withdraw(account, HUNDRED), username);

        Account after = accountService.findAccountByUsername(username);
        long withdrawals = transactionRepository.findByAccountId(after.getId()).stream()
                .filter(t -> t.getType().equals("Withdrawal"))
                .count();
        assertThat(succeeded).isEqualTo(1);
        assertThat(withdrawals).isEqualTo(1);
        assertThat(after.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void concurrentTransfersCannotOverdrawSender() throws Exception {
        String sender = newAccountWithBalance(HUNDRED);
        String recipient = newAccountWithBalance(BigDecimal.ZERO);

        int succeeded = runConcurrently(account -> accountService.transferAmount(account, recipient, HUNDRED), sender);

        assertThat(succeeded).isEqualTo(1);
        assertThat(accountService.findAccountByUsername(sender).getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(accountService.findAccountByUsername(recipient).getBalance()).isEqualByComparingTo(HUNDRED);
    }

    private String newAccountWithBalance(BigDecimal balance) {
        String username = "user-" + UUID.randomUUID();
        Account account = accountService.registerAccount(username, "password");
        account.setBalance(balance);
        accountRepository.save(account);
        return username;
    }

    // Each thread mimics one web request: an open-in-view EntityManager is bound, the account is loaded
    // the way BankController does, then all threads call the service at once.
    private int runConcurrently(Consumer<Account> action, String username) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                results.add(pool.submit(() -> {
                    EntityManager entityManager = entityManagerFactory.createEntityManager();
                    TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManager));
                    try {
                        Account account = accountService.findAccountByUsername(username);
                        barrier.await(10, TimeUnit.SECONDS);
                        action.accept(account);
                        return true;
                    } catch (RuntimeException e) {
                        return false;
                    } finally {
                        TransactionSynchronizationManager.unbindResource(entityManagerFactory);
                        entityManager.close();
                    }
                }));
            }
            int succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }
            return succeeded;
        } finally {
            pool.shutdownNow();
        }
    }
}
