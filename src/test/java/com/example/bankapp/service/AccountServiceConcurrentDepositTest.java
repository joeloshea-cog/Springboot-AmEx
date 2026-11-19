package com.example.bankapp.service;

import com.example.bankapp.model.Account;
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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceConcurrentDepositTest {

    private static final int DEPOSITS = 50;
    // Each simulated request holds a pooled connection, so stay within Hikari's default pool size of 10.
    private static final int THREADS = 10;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void concurrentDepositsAreAllApplied() throws Exception {
        String username = "concurrent-" + System.nanoTime();
        Long accountId = accountService.registerAccount(username, "password").getId();

        CyclicBarrier allLoaded = new CyclicBarrier(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<?>> results = new ArrayList<>();
            for (int i = 0; i < DEPOSITS; i++) {
                results.add(pool.submit(() -> {
                    simulateRequest(() -> {
                        Account account = accountService.findAccountByUsername(username);
                        allLoaded.await(30, TimeUnit.SECONDS);
                        accountService.deposit(account, BigDecimal.ONE);
                        return null;
                    });
                    return null;
                }));
            }
            for (Future<?> result : results) {
                result.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(transactionRepository.findByAccountId(accountId)).hasSize(DEPOSITS);
        assertThat(accountService.findAccountByUsername(username).getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(DEPOSITS));
    }

    // Mimics spring.jpa.open-in-view: one EntityManager bound to the thread for the whole request.
    private void simulateRequest(java.util.concurrent.Callable<Void> body) throws Exception {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManager));
        try {
            body.call();
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
            entityManager.close();
        }
    }
}
