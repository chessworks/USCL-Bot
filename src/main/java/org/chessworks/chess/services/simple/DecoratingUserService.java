package org.chessworks.chess.services.simple;

import java.util.Collection;
import java.util.Set;

import org.chessworks.chess.model.Role;
import org.chessworks.chess.model.User;
import org.chessworks.chess.services.InvalidNameException;
import org.chessworks.chess.services.UserService;

/**
 * Delegates all calls to the underlying {@link UserService}. This is principally intended to be used as a base class for services which wish to
 * decorate a UserService used for data storage with live functionality that connects with a chess server.
 *
 * @author Doug Bateman
 *
 */
public class DecoratingUserService implements UserService {

	private UserService service;

	public DecoratingUserService(UserService service) {
		this.service = service;
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.chess.services.UserService#addUserToRole(org.chessworks.chess.model.User, org.chessworks.chess.model.Role)
	 */
	public void addUserToRole(User user, Role role) {
		service.addUserToRole(user, role);
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.chess.services.UserService#findAllKnownUsers()
	 */
	public Collection<User> findAllKnownUsers() {
		return service.findAllKnownUsers();
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.chess.services.UserService#findOrCreateRole(java.lang.String)
	 */
	public Role findOrCreateRole(String role) {
		return service.findOrCreateRole(role);
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.chess.services.UserService#findRegisteredUsers()
	 */
	public Collection<User> findRegisteredUsers() {
		return service.findRegisteredUsers();
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.chess.services.UserService#findRole(java.lang.String)
	 */
	public Role findRole(String role) {
		return service.findRole(role);
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.chess.services.UserService#findUser(java.lang.String)
	 */
	public User findUser(String handle) {
		return service.findUser(handle);
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.chess.services.UserService#findUsersInRole(org.chessworks.chess.model.Role)
	 */
	public Set<User> findUsersInRole(Role role) {
		return service.findUsersInRole(role);
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.chess.services.UserService#isUserInRole(org.chessworks.chess.model.User, org.chessworks.chess.model.Role)
	 */
	public boolean isUserInRole(User user, Role role) {
		return service.isUserInRole(user, role);
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.chess.services.UserService#register(org.chessworks.chess.model.User)
	 */
	public void register(User user) throws InvalidNameException {
		service.register(user);
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.chess.services.UserService#flush()
	 */
	public void flush() {
		service.flush();
	}

}
