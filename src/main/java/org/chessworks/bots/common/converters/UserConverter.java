/**
 *
 */
package org.chessworks.bots.common.converters;

import org.chessworks.chess.model.User;
import org.chessworks.chess.services.UserService;

public class UserConverter extends AbstractConverter<User> {

	private UserService userService;

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

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

}
