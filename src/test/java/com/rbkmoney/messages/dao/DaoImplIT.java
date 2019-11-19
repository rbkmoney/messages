package com.rbkmoney.messages.dao;

import com.rbkmoney.messages.AbstractIT;
import com.rbkmoney.messages.TestData;
import com.rbkmoney.messages.domain.Conversation;
import com.rbkmoney.messages.domain.ConversationStatus;
import com.rbkmoney.messages.domain.Message;
import com.rbkmoney.messages.domain.User;
import com.rbkmoney.messages.exception.DaoException;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
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
        List<User> users = userDao.findAllById(List.of("1"));
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(user, users.get(0));

        Conversation conversation = TestData.createConversation("1", ConversationStatus.ACTUAL);
        conversationDao.saveAll(List.of(conversation));
        List<Conversation> conversations = conversationDao.findAllById(List.of("1"));
        Assert.assertEquals(1, conversations.size());
        Assert.assertEquals(conversation, conversations.get(0));

        List<Message> messages = TestData.createMessages(List.of(
                Pair.of("1", "1"),
                Pair.of("2", "1")
        ), "1");
        messageDao.saveAll(messages);
        List<Message> messagesFound = messageDao.findAllByConversationId(List.of("1"));
        Assert.assertEquals(2, messagesFound.size());
        Assert.assertTrue(messagesFound.containsAll(messages));
    }

    @Test(expected = DaoException.class)
    public void usersConstraintViolation() {
        conversationDao.saveAll(List.of(TestData.createConversation("1", ConversationStatus.ACTUAL)));

        List<Message> messages = TestData.createMessages(List.of(
                Pair.of("1", "1"),
                Pair.of("2", "1")
        ), "1");
        messageDao.saveAll(messages);
    }

    @Test(expected = DaoException.class)
    public void conversationsConstraintViolation() {
        userDao.saveAll(List.of(TestData.createUser("1")));

        List<Message> messages = TestData.createMessages(List.of(
                Pair.of("1", "1"),
                Pair.of("2", "1")
        ), "1");
        messageDao.saveAll(messages);
    }

    @Test
    public void conversationsSaveDuplicationTest() {
        Conversation conversation = TestData.createConversation("1", ConversationStatus.ACTUAL);
        conversationDao.saveAll(Collections.singletonList(conversation));
        Conversation conversation2 = TestData.createConversation("1", ConversationStatus.OUTDATED);
        conversationDao.saveAll(Collections.singletonList(conversation2));

        List<Conversation> conversations = conversationDao.findAllById(Collections.singletonList("1"));
        Assert.assertSame(conversations.get(0).getStatus(), ConversationStatus.OUTDATED);
    }

    @Test
    public void userModifySaveTest() {
        User user = TestData.createUser("1");
        userDao.save(user);

        User userDb2 = userDao.findAllById(Collections.singletonList("1")).get(0);

        User userModified = TestData.createUser("1");
        userModified.setFullName("Test name");
        userModified.setEmail("newtest@mail.com");
        userDao.save(userModified);

        User userDb = userDao.findAllById(Collections.singletonList("1")).get(0);
        Assert.assertEquals(userModified.getFullName(), userDb.getFullName());
        Assert.assertEquals(userModified.getEmail(), userDb.getEmail());
    }

}
