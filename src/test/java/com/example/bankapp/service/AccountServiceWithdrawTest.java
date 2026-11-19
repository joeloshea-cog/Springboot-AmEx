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
class AccountServiceWithdrawTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void negativeWithdrawalIsRejectedAndLeavesAccountUnchanged() {
        Account account = new Account();
        account.setBalance(BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("-100")));

        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
        assertEquals(0L, account.getRewardsPoints());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
