package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.TransactionRepository;
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

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void toDateIsInclusive() {
        Account account = new Account();
        account.setId(1L);
        LocalDate today = LocalDate.of(2026, 11, 19);
        List<Transaction> transactions = List.of(
                new Transaction(new BigDecimal("10"), "Deposit", today.atTime(0, 0), account),
                new Transaction(new BigDecimal("20"), "Deposit", today.atTime(12, 30), account),
                new Transaction(new BigDecimal("30"), "Withdrawal", today.atTime(23, 59, 59), account),
                new Transaction(new BigDecimal("40"), "Deposit", today.plusDays(1).atStartOfDay(), account)
        );
        when(transactionRepository.findByAccountId(1L)).thenReturn(transactions);

        List<Transaction> result = accountService.getTransactionHistory(account, today, today);

        assertThat(result).containsExactlyElementsOf(transactions.subList(0, 3));
    }
}
