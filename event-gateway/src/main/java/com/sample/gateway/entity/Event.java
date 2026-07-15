package com.sample.gateway.entity;

import com.sample.gateway.util.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "events",
        uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String eventId;

    @Column(nullable=false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private EventType type;

    @Column(nullable=false)
    private BigDecimal amount;

    @Column(nullable=false)
    private String currency;

    @Column(nullable=false)
    private Instant eventTimestamp;

    @Lob
    private String metadata;

    @Column(nullable=false)
    private Instant createdAt;
}