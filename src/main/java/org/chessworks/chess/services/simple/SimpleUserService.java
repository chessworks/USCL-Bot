package org.chessworks.chess.services.simple;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.chessworks.chess.model.Role;
import org.chessworks.chess.model.User;
import org.chessworks.chess.services.InvalidNameException;
import org.chessworks.chess.services.UserService;
import org.chessworks.common.service.BasicLifecycle;
import org.chessworks.uscl.services.InvalidPlayerException;

public class SimpleUserService extends BasicLifecycle implements UserService {

	private final SimpleNameLookupService<Role> roles = new SimpleNameLookupService<Role>();
	private final SimpleNameLookupService<User> users = new SimpleNameLookupService<User>();
	private final SimpleNameLookupService<User> registeredUsers = new SimpleNameLookupService<User>();

	private final Map<Role, Set<User>> roleCache = new HashMap<Role, Set<User>>();
	private final Map<Role, Set<User>> roleCacheReadOnly = new HashMap<Role, Set<User>>();

	/**
	 * {@inheritDoc}
	 *
	 * @see org.org.chessworks.chess.services.UserService#findUser(java.lang.String)
	 */
	@Override
	public User findUser(String handle) {
		User u = users.lookup(handle);
		if (u == null) {
			u = new User(handle);
			users.register(u);
		}
		return u;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.org.chessworks.chess.services.UserService#findAllKnownUsers()
	 */
	@Override
	public Collection<User> findAllKnownUsers() {
		return users.all();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.org.chessworks.chess.services.UserService#findRegisteredUsers()
	 */
	@Override
	public Collection<User> findRegisteredUsers() {
		return registeredUsers.all();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.org.chessworks.chess.services.UserService#register(org.chessworks.chess.model.User)
	 */
	@Override
	public void register(User user) throws InvalidNameException {
		String handle = user.getHandle();
		User existing = findUser(handle);
		if (existing != null && !existing.equalsWithCase(user)) {
			throw new InvalidPlayerException("Another user with handle \"{1}\" is already registered.", handle);
		}
		users.register(user);
		registeredUsers.register(user);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.org.chessworks.chess.services.UserService#findOrCreateRole(java.lang.String)
	 */
	@Override
	public Role findOrCreateRole(String role) {
		Role r = findRole(role);
		if (r == null) {
			r = new Role(role);
			roles.register(r);
			Set<User> set = new LinkedHashSet<User>();
			Set<User> readOnly = Collections.unmodifiableSet(set);
			roleCache.put(r, set);
			roleCacheReadOnly.put(r, readOnly);
		}
		return r;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.org.chessworks.chess.services.UserService#findRole(java.lang.String)
	 */
	@Override
	public Role findRole(String role) {
		Role r = roles.lookup(role);
		return r;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.org.chessworks.chess.services.UserService#findUsersInRole(org.chessworks.chess.model.Role)
	 */
	@Override
	public Set<User> findUsersInRole(Role role) {
		Set<User> users = roleCacheReadOnly.get(role);
		return users;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.chess.services.UserService#isUserInRole(org.chessworks.chess.model.User, org.chessworks.chess.model.Role)
	 */
	@Override
	public boolean isUserInRole(User user, Role role) {
		Set<User> set = roleCache.get(role);
		if (set == null)
			return false;
		boolean result = set.contains(user);
		return result;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.chess.services.UserService#addUserToRole(User, Role)
	 */
	@Override
	public void addUserToRole(User user, Role role) {
		Set<User> set = roleCache.get(role);
		set.add(user);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.chessworks.chess.services.UserService#flush()
	 */
	@Override
	public void flush() {
	}

	protected void reset() {
		this.users.clear();
		this.roles.clear();
		this.registeredUsers.clear();
		this.roleCache.clear();
	}
}
