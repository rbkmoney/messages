package com.rbkmoney.messages.service;

import com.rbkmoney.damsel.messages.*;
import com.rbkmoney.messages.dao.ConversationDao;
import com.rbkmoney.messages.dao.MessageDao;
import com.rbkmoney.messages.dao.UserDao;
import com.rbkmoney.messages.domain.Message;
import com.rbkmoney.messages.domain.mapper.ConversationMapper;
import com.rbkmoney.messages.domain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.rbkmoney.messages.domain.mapper.ConversationStatusMapper.fromThrift;

@Component
@RequiredArgsConstructor
public class MessagesService implements MessageServiceSrv.Iface {

    private final UserDao userDao;
    private final ConversationDao conversationDao;
    private final MessageDao messageDao;

    @Override
    public GetConversationResponse getConversations(List<String> conversationIds, ConversationFilter conversationFilter)
            throws TException {
        List<com.rbkmoney.messages.domain.Conversation> conversations = conversationDao.findAllById(conversationIds);

        checkAllConversationsFound(conversations, conversationIds);

        if (conversationFilter != null && conversationFilter.isSetConversationStatus()) {
            com.rbkmoney.messages.domain.ConversationStatus conversationStatus = fromThrift(conversationFilter.getConversationStatus());
            conversations.removeIf(conversation -> !conversation.getStatus().equals(conversationStatus));
        }
        conversations = enrichWithMessages(conversations, conversationIds);
        List<com.rbkmoney.messages.domain.User> users = getUsers(conversations);

        return convertToResponse(conversations, users);
    }

    @Override
    @Transactional
    public void saveConversations(List<Conversation> conversationsThrift, User user) throws TException {
        List<com.rbkmoney.messages.domain.Conversation> conversations = conversationsThrift.stream()
                .map(ConversationMapper::fromThrift)
                .collect(Collectors.toList());

        List<Message> messages = conversations.stream()
                .flatMap(conversation -> conversation.getMessages().stream())
                .collect(Collectors.toList());

        conversationDao.saveAll(conversations);
        userDao.save(UserMapper.fromThrift(user));
        messageDao.saveAll(messages);
    }


    private GetConversationResponse convertToResponse(List<com.rbkmoney.messages.domain.Conversation> conversations,
                                                      List<com.rbkmoney.messages.domain.User> users) {
        List<Conversation> conversationsList = conversations.stream()
                .map(ConversationMapper::toThrift)
                .collect(Collectors.toList());
        Map<String, User> idsUsersMap = users.stream()
                .map(UserMapper::toThrift)
                .collect(Collectors.toMap(
                        User::getUserId,
                        user -> user)
                );
        return new GetConversationResponse(
                conversationsList,
                idsUsersMap);
    }

    private List<com.rbkmoney.messages.domain.User> getUsers(
            List<com.rbkmoney.messages.domain.Conversation> conversations) {

        List<String> userIds = conversations.stream()
                .flatMap(conversation -> conversation.getMessages().stream())
                .map(Message::getUserId)
                .distinct()
                .collect(Collectors.toList());

        return userDao.findAllById(userIds);
    }

    private void checkAllConversationsFound(List<com.rbkmoney.messages.domain.Conversation> conversations,
                                            List<String> conversationIds) throws ConversationsNotFound {
        if (conversations.size() == conversationIds.size()) {
            return;
        }

        List<String> foundIds = conversations.stream()
                .map(com.rbkmoney.messages.domain.Conversation::getId)
                .collect(Collectors.toList());

        List<String> notFoundIds = ListUtils.removeAll(conversationIds, foundIds);

        throw new ConversationsNotFound(notFoundIds);
    }

    private List<com.rbkmoney.messages.domain.Conversation> enrichWithMessages(
            List<com.rbkmoney.messages.domain.Conversation> conversations,
            List<String> conversationIds) {

        Map<String, List<Message>> conversationIdsMessages = messageDao.findAllByConversationId(conversationIds)
                .stream()
                .collect(Collectors.groupingBy(Message::getConversationId));
        return conversations.stream()
                .map(conversation ->
                        conversation.withMessages(conversationIdsMessages.getOrDefault(conversation.getId(), List.of())))
                .collect(Collectors.toList());
    }

}
