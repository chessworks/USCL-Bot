package org.chessworks.uscl.services;

import java.util.Collection;
import java.util.Set;

import org.chessworks.uscl.model.Role;
import org.chessworks.uscl.model.User;
import org.chessworks.uscl.services.InvalidNameException;

public interface UserService {

	User findUser(String handle);

	Collection<User> findAllKnownUsers();

	Collection<User> findRegisteredUsers();

	void register(User user) throws InvalidNameException;

	Role findOrCreateRole(String role);

	Role findRole(String role);

	Set<User> findUsersInRole(Role role);

	boolean isUserInRole(User user, Role role);

	void addUserToRole(User user, Role role);

}
