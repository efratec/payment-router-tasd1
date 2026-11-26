package com.ufpe.gateway.payment.effectors;

import com.ufpe.gateway.payment.domain.model.AcquirerType;

public record AdaptationCommandDTO(AcquirerType targetAcquirer, String reason) {
}
