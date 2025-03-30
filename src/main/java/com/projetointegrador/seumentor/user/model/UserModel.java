package com.projetointegrador.seumentor.user.model;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class UserModel {

    private Integer id;
    private String name;
    private String email;
    private String password;
    private String birthday;
    private String city;
    private String state;
    private String country;
    private Boolean isMentor;
    private String role; // "mentor" or "mentee"  
}
