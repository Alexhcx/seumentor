package com.projetointegrador.seumentor.user.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.projetointegrador.seumentor.common.model.BaseEntity;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Table(name = "users")
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue
    private Integer id;
    private String firstName;
    private String lastName;
    private String profileImg;
    @Column(unique = true)
    private String email;
    private String password;
    private String birthday;
    private String city;
    private String state;
    private String country;
    private Boolean isMentor;
    @Enumerated(EnumType.STRING)
    private Role role;
    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiry;

    @OneToMany(mappedBy = "mentor", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Tutoring> mentoredSessions = new HashSet<>();

    @OneToMany(mappedBy = "mentee", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Tutoring> menteeSessions = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<UserDisciplinePreference> disciplinePreferences = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
