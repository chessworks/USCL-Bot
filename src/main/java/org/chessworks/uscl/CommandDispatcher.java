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
import org.chessworks.uscl.services.TournamentService;
import org.chessworks.uscl.services.UserService;

public class CommandDispatcher {

	public static final String prefix = "cmd";

	private Object target;
	private Map<String, CallHandler> handlerMap = new HashMap<String, CallHandler>();

	/* The fields must be listed prior to the argConverter fields. This ensures these values have been set in time. */
	UserConverter userConverter = new UserConverter();
	PlayerConverter playerConverter = new PlayerConverter();

	ConverterFactory argConverters = new ConverterFactory();
	{
		argConverters.register(new StringConverter());
		argConverters.register(new StringArrayConverter());
		argConverters.register(new StringBufferConverter());
		argConverters.register(new IntegerConverter(), Integer.class);
		argConverters.register(new IntegerConverter(), Integer.TYPE);
		argConverters.register(userConverter);
		argConverters.register(playerConverter);
	}

	ConverterFactory tellerConverters = new ConverterFactory();
	{
		tellerConverters.register(new StringConverter());
		tellerConverters.register(userConverter);
		tellerConverters.register(playerConverter);
	}

	ConverterFactory tailConverters = new ConverterFactory();
	{
		argConverters.register(new StringArrayConverter());
		argConverters.register(new StringBufferConverter());
	}

	public CommandDispatcher(Object target) {
		this.target = target;
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
		h.onCommand(teller, cmd, args);
	}

	public void setUserService(UserService service) {
		userConverter.setUserService(service);
	}

	public void setTournamentService(TournamentService service) {
		playerConverter.setTournamentService(service);
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
			int pos = (tellerConverter == null) ? 0 : 1;
			int end = paramTypes.length - 1;
			if (pos > end) {
				tailConverter = null;
				return;
			}
			this.tailConverter = tailConverters.forType(paramTypes[end]);
			if (tailConverter != null)
				end--;
			for (; pos <= end; pos++) {
				Class<?> type = paramTypes[pos];
				argConverter[pos] = argConverters.forType(type);
			}
		}

		public void onCommand(String teller, String cmd, String msg) throws ConversionException, InvocationTargetException {
			int numParams = argConverter.length;
			Object[] params = new Object[numParams];

			int paramPos = 0;
			int paramEnd = numParams - 1;
			if (tellerConverter != null) {
				params[0] = tellerConverter.convert(teller);
				paramPos++;
			}

			String[] userArgs = msg.split(" +", numParams - paramPos);
			int argPos = 0;
			int argEnd = userArgs.length - 1;
			if (argEnd >= 0 && userArgs[argEnd].trim().isEmpty()) {
				argEnd--;
			}
			if ((argEnd - argPos) < (paramEnd - paramPos)) {
				// Too few user args, try using null values.
				for (; argPos <= argEnd; argPos++, paramPos++) {
					String arg = userArgs[argPos];
					params[paramPos] = argConverter[paramPos].convert(arg);
				}
				for (; paramPos <= paramEnd; argPos++, paramPos++) {
					params[paramPos] = argConverter[paramPos].convert(null);
				}
			} else if (tailConverter != null) {
				// We can handle an arbitrary number of args.
				params[paramEnd] = tailConverter.convert(userArgs[argEnd]);
				paramEnd--;
				argEnd--;
			} else if (argEnd >= 0 && userArgs[argEnd].indexOf(' ') >= 0) {
				// Too many args.
				throw new ConversionException("Cmd has too many inputs.");
			}
			for (; argPos <= argEnd; argPos++, paramPos++) {
				String arg = userArgs[argPos];
				params[paramPos] = argConverter[paramPos].convert(arg);
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