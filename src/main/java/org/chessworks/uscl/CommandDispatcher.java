package org.chessworks.uscl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.chessworks.uscl.converters.ConversionException;
import org.chessworks.uscl.converters.Converter;
import org.chessworks.uscl.converters.IntegerConverter;
import org.chessworks.uscl.converters.StringArrayConverter;
import org.chessworks.uscl.converters.StringConverter;

public class CommandDispatcher {

	public static final String prefix = "cmd";

	private static Converter<?> getConverter(Class<?> type) {
		if (String.class == type) {
			return new StringConverter();
		} else if (String[].class == type) {
			return new StringArrayConverter();
		} else if (Integer.class == type) {
			return new IntegerConverter();
		} else if (Integer.TYPE == type) {
			return new IntegerConverter();
		}
		return null;
	}

	private static Converter<?> getTailConverter(Class<?> type) {
		if (String.class == type) {
			return new StringConverter("");
		} else if (String[].class == type) {
			return new StringArrayConverter(new String[0]);
		} else if (Integer.class == type) {
			return new IntegerConverter(null);
		} else if (Integer.TYPE == type) {
			return new IntegerConverter();
		}
		return null;
	}

	private static Converter<?> getUserConverter(Class<?> type) {
		if (String.class == type) {
			return new StringConverter();
		}
		return null;
	}

	private static class Handler {
		private final Object instance;
		private final Method method;
		private final Converter<?>[] converters;
		private final int offset;

		public Handler(Object instance, Method method) {
			this.instance = instance;
			this.method = method;
			Class<?>[] paramTypes = method.getParameterTypes();
			this.converters = new Converter<?>[paramTypes.length];
			if (paramTypes.length == 0) {
				offset = 0;
				return;
			}
			this.converters[0] = getUserConverter(paramTypes[0]);
			offset = (converters[0] == null) ? 0 : 1;
			int tail = paramTypes.length - 1;
			for (int i = offset; i < tail; i++) {
				Class<?> type = paramTypes[i];
				converters[i] = getConverter(type);
			}
			if (tail >= offset) {
				Class<?> tailType = paramTypes[tail];
				converters[tail] = getTailConverter(tailType);
			}
		}

		public void onCommand(String teller, String msg)
				throws ConversionException, InvocationTargetException {
			int numParams = converters.length;
			Object[] params = new Object[numParams];
			if (offset > 0) {
				params[0] = converters[0].convert(teller);
			}
			String[] args = msg.split(" +", numParams - 1);
			int param = this.offset;
			for (int i = 0; i < args.length; i++) {
				params[param] = converters[param].convert(args[i]);
				param++;
			}
			for (; param < params.length; param++) {
				params[param] = converters[param].convert(null);
			}
			try {
				method.invoke(instance, params);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
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
			Handler h = new Handler(target, m);
			name = name.substring(len);
			name = name.toLowerCase();
			handlerMap.put(name, h);
		}
	}

	Map<String, Handler> handlerMap = new HashMap<String, Handler>();

	public void dispatch(String teller, String cmdLine)
			throws ConversionException, NoSuchCommandException,
			InvocationTargetException {
		String s[] = cmdLine.split("[ \t]+", 2);
		if (s.length == 0)
			return;
		String cmd = s[0];
		cmd = cmd.replaceAll("[-]", "");
		cmd = cmd.toLowerCase();
		String args = (s.length == 1 ? " " : s[1]);
		Handler h = handlerMap.get(cmd);
		if (h == null) throw new NoSuchCommandException(cmd);
		h.onCommand(teller, args);
	}
}
