package ru.skillbox.socialnetwork.api.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.skillbox.socialnetwork.model.enums.FriendStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserIdStatusResponse {

  @JsonProperty("user_id")
  private Long userId;
  private FriendStatus status;
}
