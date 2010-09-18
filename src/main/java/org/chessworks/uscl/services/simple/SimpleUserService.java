package org.chessworks.uscl.services.simple;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.chessworks.uscl.model.Role;
import org.chessworks.uscl.model.User;
import org.chessworks.uscl.services.InvalidNameException;
import org.chessworks.uscl.util.SimpleNameLookupService;

public class SimpleUserService {

	private final SimpleNameLookupService<Role> roles = new SimpleNameLookupService<Role>();
	private final SimpleNameLookupService<User> users = new SimpleNameLookupService<User>();
	private final SimpleNameLookupService<User> registeredUsers = new SimpleNameLookupService<User>();

	private final Map<Role, Set<User>> roleCache = new HashMap<Role, Set<User>>();
	private final Map<Role, Set<User>> roleCacheReadOnly = new HashMap<Role, Set<User>>();

	public User findUser(String handle) {
		User u = users.lookup(handle);
		if (u == null) {
			u = new User(handle);
			users.register(u);
		}
		return u;
	}

	public Collection<User> findAllKnownUsers() {
		return users.all();
	}

	public Collection<User> findRegisteredUsers() {
		return registeredUsers.all();
	}

	public void register(User user) throws InvalidNameException {
		String handle = user.getHandle();
		User existing = findUser(handle);
		if (existing != null && !existing.equalsWithCase(user)) {
			throw new InvalidNameException("Another user with handle \"{1}\" is already registered.", handle);
		}
		users.register(user);
		registeredUsers.register(user);
		for (Role r : user.getRoles()) {
			Set<User> usersInRole = roleCache.get(r);
			usersInRole.add(user);
		}
	}

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

	public Role findRole(String role) {
		Role r = roles.lookup(role);
		return r;
	}

	public Set<User> findUsersInRole(Role role) {
		return roleCacheReadOnly.get(role);
	}

}
