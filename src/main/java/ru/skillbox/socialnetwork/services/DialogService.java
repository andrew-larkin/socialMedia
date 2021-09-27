package ru.skillbox.socialnetwork.services;

import org.springframework.stereotype.Service;
import ru.skillbox.socialnetwork.api.requests.DialogRequest;
import ru.skillbox.socialnetwork.api.requests.LinkRequest;
import ru.skillbox.socialnetwork.api.requests.MessageTextRequest;
import ru.skillbox.socialnetwork.api.responses.ErrorTimeDataResponse;
import ru.skillbox.socialnetwork.api.responses.ErrorTimeTotalOffsetPerPageListDataResponse;

import java.util.List;

@Service
public interface DialogService {

    public ErrorTimeDataResponse createDialog(List<Long> userIds);
    public ErrorTimeTotalOffsetPerPageListDataResponse getDialogsLastMessages();
    public ErrorTimeTotalOffsetPerPageListDataResponse getDialogsLastMessages(DialogRequest dialogRequest);
    public ErrorTimeTotalOffsetPerPageListDataResponse getMessagesByDialogId(Long id, String query, Integer offset, Integer limit);
    public ErrorTimeDataResponse sendMessage(Long id, MessageTextRequest messageTextRequest);
    public ErrorTimeDataResponse getPersonStatus(Long dialogId, Long personId);
    public ErrorTimeDataResponse getNewMessagesCount();

    /**
     * методы ниже не вызываются фронтом
     */
    public ErrorTimeDataResponse addUsersToDialog(Long dialogId, List<Long> userIds);
    public ErrorTimeDataResponse deleteUsersFromDialog(Long dialogId, List<Long> userIds);
    public ErrorTimeDataResponse getInviteLink(Long dialogId);
    public ErrorTimeDataResponse joinByInvite(Long dialogId, LinkRequest inviteLink);
    public ErrorTimeDataResponse deleteDialog(Long id);
    public ErrorTimeDataResponse deleteMessage(Long dialogId, Long messageId);
    public ErrorTimeDataResponse recoverMessage(Long dialogId, Long messageId);
    public ErrorTimeDataResponse markReadMessage(Long dialogId, Long messageId);
    public ErrorTimeDataResponse changeMessage(Long dialogId, Long messageId, MessageTextRequest messageTextRequest);
    public ErrorTimeDataResponse setPersonStatus(Long id, Long personId);

}
