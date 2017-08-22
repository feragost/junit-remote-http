package br.ufsc.bridge.junit.remote.http;

import java.lang.annotation.Annotation;

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

}
