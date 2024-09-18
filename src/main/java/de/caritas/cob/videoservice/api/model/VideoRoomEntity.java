package de.caritas.cob.videoservice.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "videoroom")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class VideoRoomEntity {

  @Id
  @SequenceGenerator(name = "id_seq", allocationSize = 1, sequenceName = "SEQUENCE_VIDEOROOM")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "rocketchat_room_id", updatable = false, nullable = false)
  private String rocketChatRoomId;

  @Column(name = "jitsi_room_id", updatable = false, nullable = false)
  private String jitsiRoomId;

  @Column(name = "session_id", updatable = false)
  private Long sessionId;

  @Column(name = "group_chat_id", updatable = false)
  private Long groupChatId;

  @Column(name = "create_date", nullable = false)
  private LocalDateTime createDate;

  @Column(name = "close_date")
  private LocalDateTime closeDate;
}
