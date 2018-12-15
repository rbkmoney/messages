package com.rbkmoney.messages.dao;

import com.rbkmoney.messages.AbstractIT;
import com.rbkmoney.messages.TestData;
import com.rbkmoney.messages.domain.Conversation;
import com.rbkmoney.messages.domain.ConversationStatus;
import com.rbkmoney.messages.domain.User;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class DaoImplIT extends AbstractIT {

    @Autowired
    UserDao userDao;

    @Autowired
    ConversationDao conversationDao;

    @Autowired
    MessageDao messageDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @After
    public void cleanup() {
        jdbcTemplate.update("DELETE FROM msgs.message");
        jdbcTemplate.update("DELETE FROM msgs.author");
        jdbcTemplate.update("DELETE FROM msgs.conversation");
    }

    @Test
    public void modelDbConversationTest() {

        User user = TestData.createUser("1");
        userDao.saveAll(List.of(user));
        var users = userDao.findAllById(List.of("1"));
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(user, users.get(0));

        Conversation conversation = TestData.createConversation("1", ConversationStatus.ACTUAL);
        conversationDao.saveAll(List.of(conversation));
        var conversations = conversationDao.findAllById(List.of("1"));
        Assert.assertEquals(1, conversations.size());
        Assert.assertEquals(conversation, conversations.get(0));

        var messages = TestData.createMessages(List.of(
                Pair.of("1", "1"),
                Pair.of("2", "1")
        ), "1");
        messageDao.saveAll(messages);
        var messagesFound = messageDao.findAllByConversationId(List.of("1"));
        Assert.assertEquals(2, messagesFound.size());
        Assert.assertTrue(messagesFound.containsAll(messages));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void usersConstraintViolation() {
        conversationDao.saveAll(List.of(TestData.createConversation("1", ConversationStatus.ACTUAL)));

        var messages = TestData.createMessages(List.of(
                Pair.of("1", "1"),
                Pair.of("2", "1")
        ), "1");
        messageDao.saveAll(messages);
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void conversationsConstraintViolation() {
        userDao.saveAll(List.of(TestData.createUser("1")));

        var messages = TestData.createMessages(List.of(
                Pair.of("1", "1"),
                Pair.of("2", "1")
        ), "1");
        messageDao.saveAll(messages);
    }

}