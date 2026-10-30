package com.mazlove.backend.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trusted_contacts")
@Getter
@Setter
@NoArgsConstructor
public class TrustedContact {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;
  private String name;
  private String email;
  private String phone;
}
