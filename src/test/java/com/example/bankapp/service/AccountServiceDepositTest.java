package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountServiceDepositTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void negativeDepositIsRejectedAndBalanceUnchanged() {
        Account account = accountService.registerAccount("neg-deposit-svc", "pw");

        assertThatThrownBy(() -> accountService.deposit(account, new BigDecimal("-100")))
                .isInstanceOf(RuntimeException.class);

        Account reloaded = accountService.findAccountByUsername("neg-deposit-svc");
        assertThat(reloaded.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(transactionRepository.findByAccountId(reloaded.getId())).isEmpty();
    }

    @Test
    void zeroDepositIsRejected() {
        Account account = accountService.registerAccount("zero-deposit-svc", "pw");

        assertThatThrownBy(() -> accountService.deposit(account, BigDecimal.ZERO))
                .isInstanceOf(RuntimeException.class);

        Account reloaded = accountService.findAccountByUsername("zero-deposit-svc");
        assertThat(transactionRepository.findByAccountId(reloaded.getId())).isEmpty();
    }

    @Test
    void positiveDepositStillWorks() {
        Account account = accountService.registerAccount("pos-deposit-svc", "pw");

        accountService.deposit(account, new BigDecimal("100"));

        Account reloaded = accountService.findAccountByUsername("pos-deposit-svc");
        assertThat(reloaded.getBalance()).isEqualByComparingTo("100");
        assertThat(transactionRepository.findByAccountId(reloaded.getId())).hasSize(1);
    }

    @Test
    void postNegativeDepositShowsErrorAndKeepsBalance() throws Exception {
        accountService.registerAccount("neg-deposit-web", "pw");

        mockMvc.perform(post("/deposit").param("amount", "-100").with(user("neg-deposit-web")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));

        Account reloaded = accountRepository.findByUsername("neg-deposit-web").orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(transactionRepository.findByAccountId(reloaded.getId())).isEmpty();
    }
}
