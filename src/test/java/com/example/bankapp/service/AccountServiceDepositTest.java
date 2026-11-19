package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceDepositTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account newAccount() {
        Account account = new Account();
        account.setUsername("alice");
        account.setBalance(new BigDecimal("0.00"));
        return account;
    }

    @ParameterizedTest
    @ValueSource(strings = {"-100", "0", "-0.01"})
    void depositRejectsNonPositiveAmount(String amount) {
        Account account = newAccount();

        assertThrows(RuntimeException.class, () -> accountService.deposit(account, new BigDecimal(amount)));

        assertEquals(new BigDecimal("0.00"), account.getBalance());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void depositAddsPositiveAmount() {
        Account account = newAccount();

        accountService.deposit(account, new BigDecimal("100"));

        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any());
    }
}
