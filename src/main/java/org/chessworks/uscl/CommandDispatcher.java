package org.chessworks.uscl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.chessworks.uscl.converters.ConversionException;
import org.chessworks.uscl.converters.Converter;
import org.chessworks.uscl.converters.IntegerConverter;
import org.chessworks.uscl.converters.PlayerConverter;
import org.chessworks.uscl.converters.StringArrayConverter;
import org.chessworks.uscl.converters.StringBufferConverter;
import org.chessworks.uscl.converters.StringConverter;
import org.chessworks.uscl.converters.UserConverter;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.User;
import org.chessworks.uscl.services.TournamentService;
import org.chessworks.uscl.services.simple.SimpleUserService;

public class CommandDispatcher {

	public static final String prefix = "cmd";

	private Object target;
	private Map<String, CallHandler> handlerMap = new HashMap<String, CallHandler>();

	ConverterFactory argConverters = new ConverterFactory();
	{
		argConverters.register(new StringConverter());
		argConverters.register(new StringArrayConverter());
		argConverters.register(new StringBufferConverter());
		argConverters.register(new IntegerConverter(), Integer.class);
		argConverters.register(new IntegerConverter(), Integer.TYPE);
	}

	ConverterFactory tellerConverters = new ConverterFactory();
	{
		tellerConverters.register(new StringConverter());
	}

	ConverterFactory tailConverters = new ConverterFactory();
	{
		argConverters.register(new StringArrayConverter());
		argConverters.register(new StringBufferConverter());
	}

	public CommandDispatcher(Object target) {
		Class<?> c = target.getClass();
		Method[] methods = c.getMethods();
		int len = prefix.length();
		for (Method m : methods) {
			String name = m.getName();
			int mod = m.getModifiers();
			if (Modifier.isStatic(mod))
				continue;
			if (!Modifier.isPublic(mod))
				continue;
			if (!name.startsWith(prefix))
				continue;
			CallHandler h = new CallHandler(m);
			name = name.substring(len);
			name = name.toLowerCase();
			handlerMap.put(name, h);
		}
	}

	public void dispatch(String teller, String cmdLine) throws ConversionException, NoSuchCommandException, InvocationTargetException {
		String s[] = cmdLine.split("[ \t]+", 2);
		if (s.length == 0)
			return;
		String cmd = s[0];
		cmd = cmd.replaceAll("[-]", "");
		cmd = cmd.toLowerCase();
		String args = (s.length == 1 ? " " : s[1]);
		CallHandler h = handlerMap.get(cmd);
		if (h == null)
			throw new NoSuchCommandException(cmd);
		h.onCommand(teller, args);
	}

	public void setUserService(SimpleUserService service) {
		if (service != null) {
			UserConverter teller = new UserConverter();
			UserConverter param = new UserConverter(null);
			teller.setUserService(service);
			param.setUserService(service);
			tellerConverters.register(teller, User.class);
			argConverters.register(param, User.class);
		} else {
			tellerConverters.unregister(User.class);
			argConverters.unregister(User.class);
		}
	}

	public void setPlayerService(TournamentService service) {
		if (service != null) {
			PlayerConverter teller = new PlayerConverter();
			PlayerConverter param = new PlayerConverter(null);
			teller.setPlayerService(service);
			param.setPlayerService(service);
			tellerConverters.register(teller, Player.class);
			argConverters.register(param, Player.class);
		} else {
			tellerConverters.unregister(Player.class);
			argConverters.unregister(Player.class);
		}
	}

	private class CallHandler {
		private final Method method;
		private final Converter<?>[] argConverter;
		private final Converter<?> tellerConverter;
		private final Converter<?> tailConverter;

		public CallHandler(Method method) {
			this.method = method;
			Class<?>[] paramTypes = method.getParameterTypes();
			this.argConverter = new Converter<?>[paramTypes.length];
			if (paramTypes.length == 0) {
				tailConverter = null;
				tellerConverter = null;
				return;
			}
			this.tellerConverter = tellerConverters.forType(paramTypes[0]);
			int i = (tellerConverter == null) ? 0 : 1;
			for (; i < paramTypes.length; i++) {
				Class<?> type = paramTypes[i];
				argConverter[i] = argConverters.forType(type);
			}
			Class<?> tailType = paramTypes[paramTypes.length - 1];
			tailConverter = tailConverters.forType(tailType);
		}

		public void onCommand(String teller, String msg) throws ConversionException, InvocationTargetException {
			int numParams = argConverter.length;
			Object[] params = new Object[numParams];

			int paramIndex = 0;
			if (tellerConverter != null) {
				params[0] = tellerConverter.convert(teller);
				paramIndex++;
			}

			String[] userArgs = msg.split(" +", numParams - paramIndex);
			int last = userArgs.length - 1;
			if (userArgs.length < numParams - paramIndex) {
				// Case 1: Too few user args
				int i = 0;
				for (; i < userArgs.length; i++) {
					params[paramIndex] = argConverter[i].convert(userArgs[i]);
					paramIndex++;
				}
				for (; i < params.length; i++) {
					params[paramIndex] = argConverter[i].convert(null);
				}
			} else if (userArgs[last].indexOf(' ') >= 0) {
				// Case 2: Too many user args
				int i = 0;
				for (; i < userArgs.length - 1; i++) {
					params[paramIndex] = argConverter[i].convert(userArgs[i]);
					paramIndex++;
				}
				params[paramIndex] = tailConverter.convert(userArgs[i]);
			} else {
				// Case 3: Exactly the right number
				int i = 0;
				for (; i < userArgs.length; i++) {
					params[paramIndex] = argConverter[i].convert(userArgs[i]);
					paramIndex++;
				}
			}

			try {
				method.invoke(target, params);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

}