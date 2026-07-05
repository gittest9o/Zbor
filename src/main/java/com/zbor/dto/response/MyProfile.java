package com.zbor.dto.response;

import com.zbor.data.enums.Gender;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class MyProfile {

    private String username;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private int age;
    private Gender gender;
    private Boolean isBlocked = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    //private List<ShortEventResponse> organizedEvents = new ArrayList<>();
    //private Set<ShortEventResponse> events = new HashSet<>();

}
