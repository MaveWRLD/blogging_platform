package org.amalitech.service;

import org.amalitech.dao.UserRoleDao;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService {

    private final UserRoleDao userRoleDao;

    public UserRoleService(UserRoleDao userRoleDao) {
        this.userRoleDao = userRoleDao;
    }

    public void assignRoleToUser(int userId, int roleId) {
        userRoleDao.assignRoleToUser(userId, roleId);
    }

    public void removeRoleFromUser(int userId, int roleId) {
        userRoleDao.removeRoleFromUser(userId, roleId);
    }
}
