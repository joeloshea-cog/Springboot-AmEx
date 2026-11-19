package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceTransferAmountTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account alice;
    private Account bob;

    @BeforeEach
    void setUp() {
        alice = new Account();
        alice.setUsername("alice");
        alice.setBalance(new BigDecimal("150"));

        bob = new Account();
        bob.setUsername("bob");
        bob.setBalance(new BigDecimal("100"));

        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-60", "0"})
    void rejectsNonPositiveAmountAndLeavesBalancesUnchanged(String amount) {
        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(alice, "bob", new BigDecimal(amount)));

        assertEquals(new BigDecimal("150"), alice.getBalance());
        assertEquals(new BigDecimal("100"), bob.getBalance());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
