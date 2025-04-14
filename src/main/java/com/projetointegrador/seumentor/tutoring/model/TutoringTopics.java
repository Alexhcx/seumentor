package com.projetointegrador.seumentor.tutoring.model;

import com.projetointegrador.seumentor.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tutoring_topics")
@EqualsAndHashCode(callSuper = false, exclude = { "tutoring" })
@ToString(exclude = { "tutoring" })
public class TutoringTopics extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutoring_id", nullable = false)
    private Tutoring tutoring;
}
