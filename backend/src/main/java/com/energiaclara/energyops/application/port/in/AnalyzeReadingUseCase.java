package com.energiaclara.energyops.application.port.in;

public interface AnalyzeReadingUseCase {
    AnalyzeReadingResult analyze(AnalyzeReadingCommand command);
}
