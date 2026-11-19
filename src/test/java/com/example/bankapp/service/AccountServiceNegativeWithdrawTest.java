package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AccountServiceNegativeWithdrawTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void withdrawRejectsNegativeAmount() {
        Account account = new Account();
        account.setBalance(new BigDecimal("-100"));
        account.setRewardsPoints(0L);

        assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("-250")));

        assertEquals(0, new BigDecimal("-100").compareTo(account.getBalance()));
        assertEquals(0L, account.getRewardsPoints());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdrawRejectsZeroAmount() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100"));
        account.setRewardsPoints(0L);

        assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, BigDecimal.ZERO));

        verify(transactionRepository, never()).save(any());
    }
}
