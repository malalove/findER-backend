package com.finder.domain;

import lombok.*;
import javax.persistence.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "linkedUserId"}))
public class Link extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "LINK_SEQUENCE_GENERATOR")
    @SequenceGenerator(name = "LINK_SEQUENCE_GENERATOR", sequenceName = "LINK_SQ", initialValue = 1, allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "USER_ID")
    private Users user;

    private Long linkedUserId;

    private String familyRelations;
}