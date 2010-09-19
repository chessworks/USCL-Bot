package org.chessworks.uscl.services;

import java.io.Flushable;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.chessworks.uscl.model.Role;
import org.chessworks.uscl.model.User;

public interface UserService extends Flushable {

	User findUser(String handle);

	Collection<User> findAllKnownUsers();

	Collection<User> findRegisteredUsers();

	void register(User user) throws InvalidNameException;

	Role findOrCreateRole(String role);

	Role findRole(String role);

	Set<User> findUsersInRole(Role role);

	boolean isUserInRole(User user, Role role);

	void addUserToRole(User user, Role role);

	/** Saves any unwritten data. */
	void flush();

}
