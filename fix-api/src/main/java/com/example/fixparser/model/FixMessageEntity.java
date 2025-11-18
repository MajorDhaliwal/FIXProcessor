package com.example.fixparser.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fix_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String msgType;
    private String symbol;
    private String side;
    private String orderQty;
    private String price;
    private String senderCompID;
    private String targetCompID;
    private String sendingTime;
    private String rawMessage;

    @Column(updatable = false)
    private java.time.LocalDateTime receivedAt = java.time.LocalDateTime.now();
}
