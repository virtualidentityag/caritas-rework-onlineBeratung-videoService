package de.caritas.cob.videoservice.api.facade;

import static de.caritas.cob.videoservice.api.service.session.SessionStatus.IN_PROGRESS;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import de.caritas.cob.videoservice.api.authorization.VideoUser;
import de.caritas.cob.videoservice.api.exception.httpresponse.BadRequestException;
import de.caritas.cob.videoservice.api.model.CreateVideoCallDTO;
import de.caritas.cob.videoservice.api.model.CreateVideoCallResponseDTO;
import de.caritas.cob.videoservice.api.model.VideoRoomEntity;
import de.caritas.cob.videoservice.api.service.LogService;
import de.caritas.cob.videoservice.api.service.UuidRegistry;
import de.caritas.cob.videoservice.api.service.liveevent.LiveEventNotificationService;
import de.caritas.cob.videoservice.api.service.message.MessageService;
import de.caritas.cob.videoservice.api.service.session.ChatService;
import de.caritas.cob.videoservice.api.service.session.SessionService;
import de.caritas.cob.videoservice.api.service.statistics.StatisticsService;
import de.caritas.cob.videoservice.api.service.statistics.event.StartVideoCallStatisticsEvent;
import de.caritas.cob.videoservice.api.service.statistics.event.StopVideoCallStatisticsEvent;
import de.caritas.cob.videoservice.api.service.video.VideoCallUrlGeneratorService;
import de.caritas.cob.videoservice.api.service.video.VideoRoomService;
import de.caritas.cob.videoservice.liveservice.generated.web.model.EventType;
import de.caritas.cob.videoservice.liveservice.generated.web.model.LiveEventMessage;
import de.caritas.cob.videoservice.liveservice.generated.web.model.VideoCallRequestDTO;
import de.caritas.cob.videoservice.statisticsservice.generated.web.model.UserRole;
import de.caritas.cob.videoservice.userservice.generated.web.model.ChatInfoResponseDTO;
import de.caritas.cob.videoservice.userservice.generated.web.model.ChatMembersResponseDTO;
import de.caritas.cob.videoservice.userservice.generated.web.model.ConsultantSessionDTO;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Facade for video call starts and stops. */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoCallFacade {

  private final @NonNull SessionService sessionService;

  private final @NonNull ChatService chatService;
  private final @NonNull LiveEventNotificationService liveEventNotificationService;
  private final @NonNull VideoUser authenticatedUser;
  private final @NonNull VideoCallUrlGeneratorService videoCallUrlGeneratorService;
  private final @NonNull UuidRegistry uuidRegistry;
  private final @NonNull StatisticsService statisticsService;
  private final @NonNull VideoRoomService videoRoomService;

  private final @NonNull MessageService messageService;

  /**
   * Generates unique video call URLs and triggers a live event to inform the receiver of the call.
   *
   * @param createVideoCallRequest The requested DTO containing session id and optional initiators
   *     username
   * @param initiatorRcUserId initiator Rocket.Chat user ID
   * @return {@link CreateVideoCallResponseDTO}
   */
  public CreateVideoCallResponseDTO startVideoCall(
      CreateVideoCallDTO createVideoCallRequest, String initiatorRcUserId) {
    if (createVideoCallRequest.getGroupChatId() != null) {
      return startGroupVideoCall(createVideoCallRequest, initiatorRcUserId);
    } else {
      var sessionId = createVideoCallRequest.getSessionId();
      return startOneToOneVideoCall(createVideoCallRequest, initiatorRcUserId, sessionId);
    }
  }

  private CreateVideoCallResponseDTO startOneToOneVideoCall(
      CreateVideoCallDTO createVideoCallRequest, String initiatorRcUserId, Long sessionId) {
    var consultantSessionDto = this.sessionService.findSessionOfCurrentConsultant(sessionId);
    verifySessionStatus(consultantSessionDto);

    var videoCallUuid = uuidRegistry.generateUniqueUuid();
    var videoCallUrls = this.videoCallUrlGeneratorService.generateVideoCallUrls(videoCallUuid);

    this.liveEventNotificationService.sendVideoCallRequestLiveEvent(
        buildLiveEventMessage(
            consultantSessionDto.getGroupId(),
            videoCallUrls.getUserVideoUrl(),
            initiatorRcUserId,
            createVideoCallRequest.getInitiatorDisplayName()),
        singletonList(consultantSessionDto.getAskerId()));

    this.videoRoomService.createOneToOneVideoRoom(
        consultantSessionDto.getId(), videoCallUuid, videoCallUrls.getModeratorVideoUrl());
    var createVideoCallResponseDto =
        new CreateVideoCallResponseDTO()
            .moderatorVideoCallUrl(videoCallUrls.getModeratorVideoUrl());

    statisticsService.fireEvent(
        new StartVideoCallStatisticsEvent(
            authenticatedUser.getUserId(), UserRole.CONSULTANT, sessionId, videoCallUuid));

    return createVideoCallResponseDto;
  }

  private CreateVideoCallResponseDTO startGroupVideoCall(
      CreateVideoCallDTO createVideoCallRequest, String initiatorRcUserId) {

    chatService.assertCanModerateChat(createVideoCallRequest.getGroupChatId());

    ChatInfoResponseDTO chatById =
        chatService.findChatById(createVideoCallRequest.getGroupChatId());

    ChatMembersResponseDTO chatMembers =
        chatService.getChatMembers(createVideoCallRequest.getGroupChatId());

    var videoCallUuid = uuidRegistry.generateUniqueUuid();
    var videoCallUrls = this.videoCallUrlGeneratorService.generateVideoCallUrls(videoCallUuid);
    List<String> chatMemberIds =
        chatMembers.getMembers().stream()
            .map(member -> member.getId())
            .collect(Collectors.toList());
    this.liveEventNotificationService.sendVideoCallRequestLiveEvent(
        buildLiveEventMessage(
            chatById.getGroupId(),
            videoCallUrls.getUserVideoUrl(),
            initiatorRcUserId,
            createVideoCallRequest.getInitiatorDisplayName()),
        chatMemberIds);

    VideoRoomEntity groupVideoRoom =
        this.videoRoomService.createGroupVideoRoom(
            createVideoCallRequest.getGroupChatId(),
            videoCallUuid,
            videoCallUrls.getModeratorVideoUrl());

    messageService.createAndSendVideoChatStartdMessage(
        chatById.getGroupId(), authenticatedUser.getUsername(), groupVideoRoom);

    return new CreateVideoCallResponseDTO()
        .moderatorVideoCallUrl(videoCallUrls.getModeratorVideoUrl());
  }

  /**
   * @param roomId room ID
   */
  public void stopVideoCall(String roomId) {
    VideoRoomEntity byJitsiRoomId = videoRoomService.findByJitsiRoomId(roomId).orElseThrow();
    if (byJitsiRoomId.getGroupChatId() != null) {
      stopGroupVideoCall(byJitsiRoomId);
    } else {
      stopOneToOneVideoCall(roomId);
    }
  }

  private void stopGroupVideoCall(VideoRoomEntity videoRoomEntity) {
    ChatInfoResponseDTO chatById = chatService.findChatById(videoRoomEntity.getGroupChatId());
    videoRoomService.closeVideoRoom(videoRoomEntity);
    messageService.createAndSendMessage(
        chatById.getGroupId(), "Video-Call stopped", videoRoomEntity);

    fireVideoCallStoppedStatisticsEvent(videoRoomEntity.getJitsiRoomId());
  }

  private void fireVideoCallStoppedStatisticsEvent(String roomId) {
    var event =
        new StopVideoCallStatisticsEvent(
            authenticatedUser.getUserId(), UserRole.CONSULTANT, roomId);
    statisticsService.fireEvent(event);
  }

  private void stopOneToOneVideoCall(String roomId) {
    fireVideoCallStoppedStatisticsEvent(roomId);
  }

  private void verifySessionStatus(ConsultantSessionDTO consultantSessionDto) {
    if (!IN_PROGRESS.getValue().equals(consultantSessionDto.getStatus())) {
      throw new BadRequestException("Session must be in progress", LogService::logWarning);
    }
  }

  private LiveEventMessage buildLiveEventMessage(
      String rcGroupId,
      String videoChatUrl,
      String initiatorRcUserId,
      String initiatorDisplayName) {
    var username =
        isNotBlank(initiatorDisplayName) ? initiatorDisplayName : authenticatedUser.getUsername();

    var videoCallRequestDto =
        new VideoCallRequestDTO()
            .videoCallUrl(videoChatUrl)
            .rcGroupId(rcGroupId)
            .initiatorRcUserId(initiatorRcUserId)
            .initiatorUsername(username);

    return new LiveEventMessage()
        .eventType(EventType.VIDEOCALLREQUEST)
        .eventContent(videoCallRequestDto);
  }
}
