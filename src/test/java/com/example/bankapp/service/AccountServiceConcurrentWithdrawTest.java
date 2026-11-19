package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceConcurrentWithdrawTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void concurrentWithdrawalsCannotOverdrawAccount() throws Exception {
        String username = "concurrent-" + System.nanoTime();
        Account created = accountService.registerAccount(username, "password");
        created.setBalance(new BigDecimal("100"));
        accountRepository.save(created);

        int threads = 10;
        BigDecimal amount = new BigDecimal("80");
        CountDownLatch loaded = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                // Mimic open-in-view: one EntityManager per request, bound for the whole request.
                EntityManager em = entityManagerFactory.createEntityManager();
                TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(em));
                try {
                    Account account = accountService.findAccountByUsername(username);
                    loaded.countDown();
                    loaded.await(10, TimeUnit.SECONDS);
                    accountService.withdraw(account, amount);
                    return true;
                } catch (RuntimeException e) {
                    return false;
                } finally {
                    TransactionSynchronizationManager.unbindResource(entityManagerFactory);
                    em.close();
                }
            }));
        }

        int succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                succeeded++;
            }
        }
        pool.shutdown();

        Account after = accountService.findAccountByUsername(username);
        assertEquals(1, succeeded, "only one $80 withdrawal can succeed from a $100 balance");
        assertEquals(0, new BigDecimal("20").compareTo(after.getBalance()), "balance should be 20 but was " + after.getBalance());
        assertEquals(1, accountService.getTransactionHistory(after).size());
    }
}
