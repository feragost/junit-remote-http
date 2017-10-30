package br.ufsc.bridge.junit.remote.http;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;

import com.google.gson.Gson;

public class Utils {

	public static <A extends Annotation> A findAnnotation(final Class<?> clazz, final Class<A> annotationType) {
		A annotation = clazz.getAnnotation(annotationType);
		if (annotation != null) {
			return annotation;
		}
		for (Class<?> ifc : clazz.getInterfaces()) {
			annotation = findAnnotation(ifc, annotationType);
			if (annotation != null) {
				return annotation;
			}
		}
		return null;
	}

	public static String serialize(Serializable obj) throws IOException {

		Gson gson = new Gson();
		String json = gson.toJson(obj);
		return json;

	}

	public static <T> T deserialize(String data, Class<? extends T> clazz) throws IOException, ClassNotFoundException {

		Gson gson = new Gson();
		T obj = gson.fromJson(data, clazz);
		return obj;

	}

}
