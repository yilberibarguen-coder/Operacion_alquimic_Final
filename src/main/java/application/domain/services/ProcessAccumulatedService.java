package application.domain.services;

import application.domain.models.AlchemyReport;
import application.domain.ports.out.ReadingRepositoryPort;

/** F3 - Processes the information accumulated so far; fails with "insufficient data" when it is not enough. */
public class ProcessAccumulatedService {

    private final ReadingRepositoryPort readingRepositoryPort;
    private final AlchemyAnalyzer analyzer;

    public ProcessAccumulatedService(ReadingRepositoryPort readingRepositoryPort, AlchemyAnalyzer analyzer) {
        this.readingRepositoryPort = readingRepositoryPort;
        this.analyzer = analyzer;
    }

    public AlchemyReport process() {
        return analyzer.analyze(readingRepositoryPort.findAll());
    }
}
