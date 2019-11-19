package com.rbkmoney.messages.dao;

import com.rbkmoney.messages.domain.User;
import com.rbkmoney.messages.exception.DaoException;

import java.util.List;

public interface UserDao {

    void saveAll(List<User> users) throws DaoException;

    void save(User user) throws DaoException;

    List<User> findAllById(List<String> ids) throws DaoException;
}
