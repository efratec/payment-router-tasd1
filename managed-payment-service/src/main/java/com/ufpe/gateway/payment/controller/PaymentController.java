package com.ufpe.gateway.payment.controller;

import com.ufpe.gateway.payment.domain.model.PaymentRequest;
import com.ufpe.gateway.payment.domain.model.PaymentResult;
import com.ufpe.gateway.payment.service.PaymentExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentExecutionService paymentExecutionService;

    @PostMapping
    public ResponseEntity<PaymentResult> processPayment(@RequestBody PaymentRequest request) {
        String transactionId = UUID.randomUUID().toString();
        request.setTransactionId(transactionId);

        log.info("📥 [PAYMENT_API] Requisição recebida | transactionId={}", transactionId);

        try {
            PaymentResult result = paymentExecutionService.executePayment(request);
            log.info("✅ [PAYMENT_API] Pagamento finalizado | transactionId={} | acquirer={} | success={}",
                    transactionId, result.acquirerUsed(), result.success());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ [PAYMENT_API] Erro ao processar pagamento | transactionId={} | erro={}",
                    transactionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

}