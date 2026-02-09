package org.amalitech.interfaces;

public interface UserRoleRepository {
    void assignRoleToUser(int userId, int roleId);
    void removeRoleFromUser(int userId, int roleId);
}
