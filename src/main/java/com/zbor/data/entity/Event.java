package com.zbor.data.entity;

import com.zbor.data.enums.EventCategory;
import com.zbor.data.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.PUBLISHED;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER) //todo Пофиксить в дальнейшем и получать по частям напрямую из бд
    @JoinTable(
            name = "event_participants",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "participant_id")
    )
    private Set<User> participants = new HashSet<>();


    public void addParticipant(User participant) {
        this.participants.add(participant);
        participant.getEvents().add(this);
    }

    public void removeParticipant(User participant) {
        this.participants.remove(participant);
        participant.getEvents().remove(this);
    }

    public int getRegistrationCount() {
        return participants.size();
    }

    public boolean isFull() {
        return maxParticipants != null && getRegistrationCount() >= maxParticipants;
    }

    public boolean isPublished() {
        return status == EventStatus.PUBLISHED;
    }

    public boolean isParticipant(User user){
        return participants.contains(user);
    }
}
