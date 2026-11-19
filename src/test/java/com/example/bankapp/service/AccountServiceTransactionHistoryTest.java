package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTransactionHistoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
    }

    @Test
    void statementIncludesTransactionsOnTheToDate() {
        List<Transaction> todays = List.of(
                new Transaction(new BigDecimal("10"), "Deposit", TODAY.atTime(9, 0), account),
                new Transaction(new BigDecimal("20"), "Deposit", TODAY.atTime(13, 30), account),
                new Transaction(new BigDecimal("5"), "Withdrawal", TODAY.atTime(23, 59, 59), account));
        when(transactionRepository.findByAccountId(1L)).thenReturn(todays);

        assertThat(accountService.getTransactionHistory(account, TODAY, TODAY)).hasSize(3);
    }

    @Test
    void statementExcludesTransactionsAfterTheToDate() {
        Transaction inRange = new Transaction(new BigDecimal("10"), "Deposit", TODAY.atTime(12, 0), account);
        Transaction nextDay = new Transaction(new BigDecimal("20"), "Deposit", TODAY.plusDays(1).atStartOfDay(), account);
        Transaction dayBefore = new Transaction(new BigDecimal("30"), "Deposit", TODAY.minusDays(1).atTime(23, 59), account);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(dayBefore, inRange, nextDay));

        assertThat(accountService.getTransactionHistory(account, TODAY, TODAY)).containsExactly(inRange);
    }
}
