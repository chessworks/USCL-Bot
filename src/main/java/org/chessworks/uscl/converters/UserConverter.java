/**
 *
 */
package org.chessworks.uscl.converters;

import org.chessworks.uscl.model.User;
import org.chessworks.uscl.services.simple.SimpleUserService;

public class UserConverter extends AbstractConverter<User> {

	private SimpleUserService userService;

	public UserConverter() {
		super(User.class);
	}

	public UserConverter(User nullValue) {
		super(User.class, nullValue);
	}

	@Override
	public User convert(String s) throws ConversionException {
		if (checkNull(s))
			return nullValue;
		User u = userService.findUser(s);
		return u;
	}

	public void setUserService(SimpleUserService userService) {
		this.userService = userService;
	}

}
