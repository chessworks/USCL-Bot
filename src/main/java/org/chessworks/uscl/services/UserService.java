package org.chessworks.uscl.services;

import java.util.Collection;
import java.util.Set;

import org.chessworks.uscl.model.Role;
import org.chessworks.uscl.model.User;
import org.chessworks.uscl.services.InvalidNameException;

public interface UserService {

	void addUserToRole(User user, Role role);

	Collection<User> findAllKnownUsers();

	Role findOrCreateRole(String role);

	Collection<User> findRegisteredUsers();

	Role findRole(String role);

	User findUser(String handle);

	Set<User> findUsersInRole(Role role);

	boolean isUserInRole(User user, Role role);

	void register(User user) throws InvalidNameException;

}
