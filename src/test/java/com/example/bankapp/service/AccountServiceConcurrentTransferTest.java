package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
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
class AccountServiceConcurrentTransferTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void concurrentTransfersCannotOverdrawSenderOrLoseUpdates() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String sender = "hank-" + suffix;
        String recipient = "ivy-" + suffix;
        Account hank = accountService.registerAccount(sender, "password");
        hank.setBalance(new BigDecimal("100"));
        accountRepository.save(hank);
        accountService.registerAccount(recipient, "password");

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
                    Account from = accountService.findAccountByUsername(sender);
                    loaded.countDown();
                    loaded.await(10, TimeUnit.SECONDS);
                    accountService.transferAmount(from, recipient, amount);
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

        Account hankAfter = accountService.findAccountByUsername(sender);
        Account ivyAfter = accountService.findAccountByUsername(recipient);
        List<Transaction> hankTransfers = accountService.getTransactionHistory(hankAfter).stream()
                .filter(t -> t.getType().startsWith("Transfer Out"))
                .toList();
        assertEquals(1, succeeded, "only one $80 transfer can succeed from a $100 balance");
        assertEquals(1, hankTransfers.size(), "ledger should record exactly one Transfer Out");
        assertEquals(0, new BigDecimal("20").compareTo(hankAfter.getBalance()), "sender balance should be 20 but was " + hankAfter.getBalance());
        assertEquals(0, new BigDecimal("80").compareTo(ivyAfter.getBalance()), "recipient balance should be 80 but was " + ivyAfter.getBalance());
    }
}
