package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceSubCentAmountTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account newAccount(BigDecimal balance) {
        Account account = accountService.registerAccount("user-" + UUID.randomUUID(), "pw");
        account.setBalance(balance);
        return accountRepository.save(account);
    }

    private BigDecimal storedBalance(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getBalance();
    }

    @Test
    void depositWithMoreThanTwoDecimalsIsRejectedAndDoesNotCreateMoney() {
        Account account = newAccount(BigDecimal.ZERO);

        assertThrows(RuntimeException.class, () -> accountService.deposit(account, new BigDecimal("10.005")));
        assertThrows(RuntimeException.class, () -> accountService.deposit(account, new BigDecimal("10.005")));

        assertEquals(0, storedBalance(account).compareTo(BigDecimal.ZERO));
        assertTrue(transactionRepository.findByAccountId(account.getId()).isEmpty());
    }

    @Test
    void subCentDepositIsRejectedInsteadOfRecordingZeroTransaction() {
        Account account = newAccount(BigDecimal.ZERO);

        assertThrows(RuntimeException.class, () -> accountService.deposit(account, new BigDecimal("0.004")));

        assertTrue(transactionRepository.findByAccountId(account.getId()).isEmpty());
    }

    @Test
    void withdrawWithMoreThanTwoDecimalsIsRejected() {
        Account account = newAccount(new BigDecimal("100.00"));

        assertThrows(RuntimeException.class, () -> accountService.withdraw(account, new BigDecimal("10.005")));

        assertEquals(0, storedBalance(account).compareTo(new BigDecimal("100.00")));
        assertTrue(transactionRepository.findByAccountId(account.getId()).isEmpty());
    }

    @Test
    void transferWithMoreThanTwoDecimalsIsRejected() {
        Account from = newAccount(new BigDecimal("100.00"));
        Account to = newAccount(BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(from, to.getUsername(), new BigDecimal("10.005")));

        assertEquals(0, storedBalance(from).compareTo(new BigDecimal("100.00")));
        assertEquals(0, storedBalance(to).compareTo(BigDecimal.ZERO));
    }

    @Test
    void amountsWithTwoDecimalsOrTrailingZerosAreAccepted() {
        Account account = newAccount(BigDecimal.ZERO);

        accountService.deposit(account, new BigDecimal("10.01"));
        accountService.deposit(account, new BigDecimal("5.000"));

        assertEquals(0, storedBalance(account).compareTo(new BigDecimal("15.01")));
    }
}
