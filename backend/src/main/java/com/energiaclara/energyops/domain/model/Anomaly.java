package com.energiaclara.energyops.domain.model;

import com.energiaclara.energyops.domain.AnomalyType;
import com.energiaclara.energyops.domain.event.AnomalyDetectedEvent;
import com.energiaclara.energyops.domain.event.AnomalyResolvedEvent;
import com.energiaclara.energyops.domain.event.DomainEvent;
import com.energiaclara.energyops.domain.vo.AnomalyScope;
import com.energiaclara.energyops.domain.vo.DeviationPercent;
import com.energiaclara.energyops.domain.vo.ImpactEstimate;
import com.energiaclara.energyops.domain.vo.SeverityLevel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Anomaly {
    private final AnomalyId id;
    private final AnomalyScope scope;
    private final AnomalyType type;
    private final SeverityLevel severity;
    private final DeviationPercent deviationPercent;
    private final ImpactEstimate impact;
    private final String explanation;
    private final String recommendation;
    private AnomalyStatus status;
    private final Instant detectedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Anomaly(
            AnomalyId id,
            AnomalyScope scope,
            AnomalyType type,
            SeverityLevel severity,
            DeviationPercent deviationPercent,
            ImpactEstimate impact,
            String explanation,
            String recommendation,
            AnomalyStatus status,
            Instant detectedAt
    ) {
        this.id = id;
        this.scope = scope;
        this.type = type;
        this.severity = severity;
        this.deviationPercent = deviationPercent;
        this.impact = impact;
        this.explanation = explanation;
        this.recommendation = recommendation;
        this.status = status;
        this.detectedAt = detectedAt;
    }

    public static Anomaly detect(
            AnomalyScope scope,
            AnomalyType type,
            DeviationPercent deviation,
            ImpactEstimate impact,
            String explanation,
            String recommendation
    ) {
        Anomaly anomaly = new Anomaly(
                AnomalyId.generate(),
                scope,
                type,
                SeverityLevel.from(deviation),
                deviation,
                impact,
                explanation,
                recommendation,
                AnomalyStatus.DETECTED,
                Instant.now()
        );
        anomaly.domainEvents.add(new AnomalyDetectedEvent(anomaly.id, scope, anomaly.severity, Instant.now()));
        return anomaly;
    }

    public static Anomaly reconstruct(
            AnomalyId id,
            AnomalyScope scope,
            AnomalyType type,
            SeverityLevel severity,
            DeviationPercent deviation,
            ImpactEstimate impact,
            String explanation,
            String recommendation,
            AnomalyStatus status,
            Instant detectedAt
    ) {
        return new Anomaly(id, scope, type, severity, deviation, impact, explanation, recommendation, status, detectedAt);
    }

    public void notificar() {
        if (status != AnomalyStatus.DETECTED) {
            throw new IllegalStateException("Solo se puede notificar una anomalia en estado DETECTED");
        }
        status = AnomalyStatus.NOTIFIED;
    }

    public void iniciarAccion() {
        if (status != AnomalyStatus.NOTIFIED) {
            throw new IllegalStateException("Solo se puede iniciar accion para una anomalia NOTIFIED");
        }
        status = AnomalyStatus.IN_ACTION;
    }

    public void resolver(String resolutionNote) {
        if (status != AnomalyStatus.IN_ACTION) {
            throw new IllegalStateException("Solo se puede resolver una anomalia en estado IN_ACTION");
        }
        status = AnomalyStatus.RESOLVED;
        domainEvents.add(new AnomalyResolvedEvent(id, resolutionNote, Instant.now()));
    }

    public void ignorar() {
        if (status != AnomalyStatus.DETECTED && status != AnomalyStatus.NOTIFIED) {
            throw new IllegalStateException("Solo se puede ignorar una anomalia en estado DETECTED o NOTIFIED");
        }
        status = AnomalyStatus.IGNORED;
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public AnomalyId getId() {
        return id;
    }

    public AnomalyScope getScope() {
        return scope;
    }

    public AnomalyType getType() {
        return type;
    }

    public SeverityLevel getSeverity() {
        return severity;
    }

    public DeviationPercent getDeviationPercent() {
        return deviationPercent;
    }

    public ImpactEstimate getImpact() {
        return impact;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public AnomalyStatus getStatus() {
        return status;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }
}
