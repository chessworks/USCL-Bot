package org.chessworks.uscl.services.simple;

import java.util.Collection;
import java.util.Set;

import org.chessworks.uscl.model.Role;
import org.chessworks.uscl.model.User;
import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.services.UserService;

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
	 * @see org.chessworks.uscl.services.UserService#findAllKnownUsers()
	 */
	public Collection<User> findAllKnownUsers() {
		return service.findAllKnownUsers();
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.uscl.services.UserService#findOrCreateRole(java.lang.String)
	 */
	public Role findOrCreateRole(String role) {
		return service.findOrCreateRole(role);
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.uscl.services.UserService#findRegisteredUsers()
	 */
	public Collection<User> findRegisteredUsers() {
		return service.findRegisteredUsers();
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.uscl.services.UserService#findRole(java.lang.String)
	 */
	public Role findRole(String role) {
		return service.findRole(role);
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.uscl.services.UserService#findUser(java.lang.String)
	 */
	public User findUser(String handle) {
		return service.findUser(handle);
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.uscl.services.UserService#findUsersInRole(org.chessworks.uscl.model.Role)
	 */
	public Set<User> findUsersInRole(Role role) {
		return service.findUsersInRole(role);
	}

	/**
	 * Delegates all calls to the underlying {@link UserService}.
	 *
	 * @see org.chessworks.uscl.services.UserService#register(org.chessworks.uscl.model.User)
	 */
	public void register(User user) throws InvalidNameException {
		service.register(user);
	}

}
