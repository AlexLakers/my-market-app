package com.alex.market.mvc.security.model;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@FieldNameConstants
@Table("users")
public class User {

    @Id
    private Long id;

    @Column("first_name")
    private String firstname;

    @Column("last_name")
    private String lastname;

    @Column("username")
    private String username;

    @Column("password")
    private String password;

    @Column("birthday")
    private LocalDate birthday;

    @Column("role")
    private Role role;

}
