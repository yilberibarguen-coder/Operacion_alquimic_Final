package application.configuration;

import application.domain.ports.out.ReadingRepositoryPort;
import application.domain.services.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Framework wiring stays outside the domain. */
@Configuration(proxyBeanMethods = false)
public class DomainConfiguration {
    @Bean AlchemyAnalyzer alchemyAnalyzer() { return new AlchemyAnalyzer(); }
    @Bean ProcessCompleteInformationService processCompleteInformationService(AlchemyAnalyzer analyzer) {
        return new ProcessCompleteInformationService(analyzer);
    }
    @Bean RegisterReadingService registerReadingService(ReadingRepositoryPort repository) {
        return new RegisterReadingService(repository);
    }
    @Bean ConsultReadingsService consultReadingsService(ReadingRepositoryPort repository, AlchemyAnalyzer analyzer) {
        return new ConsultReadingsService(repository, analyzer);
    }
    @Bean ProcessAccumulatedService processAccumulatedService(ReadingRepositoryPort repository, AlchemyAnalyzer analyzer) {
        return new ProcessAccumulatedService(repository, analyzer);
    }
    @Bean ReplaceReadingService replaceReadingService(ReadingRepositoryPort repository) {
        return new ReplaceReadingService(repository);
    }
    @Bean UpdateReadingService updateReadingService(ReadingRepositoryPort repository) {
        return new UpdateReadingService(repository);
    }
    @Bean RemoveReadingService removeReadingService(ReadingRepositoryPort repository) {
        return new RemoveReadingService(repository);
    }
}
