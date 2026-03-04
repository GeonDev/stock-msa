package com.stock.strategy.service.slippage;

import com.stock.strategy.enums.SlippageType;
import java.math.BigDecimal;

public class SlippageModelFactory {
    public static SlippageModel create(SlippageType type, BigDecimal fixedRate) {
        if (type == null) return new NoSlippageModel();
        
        return switch (type) {
            case NONE -> new NoSlippageModel();
            case FIXED -> new FixedSlippageModel(fixedRate);
            case VOLUME -> new VolumeBasedSlippageModel(null, null);
            default -> new NoSlippageModel();
        };
    }
}
