package com.orderflow.business;

import com.orderflow.dao.UserDAO;
import com.orderflow.model.User;

public class UserService {

    private final UserDAO userDAO = new UserDAO();

    public User findByUsername(String username) {
        return userDAO.findByUsername(username);
    }
}
