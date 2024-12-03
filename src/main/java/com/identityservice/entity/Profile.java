package com.identityservice.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document("profile")
public class Profile {

    String profileId;
    String userId;
    String email;
    String username;
    String firstName;
    String lastName;
    LocalDate dob;

}
