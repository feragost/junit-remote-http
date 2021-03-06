package br.ufsc.bridge.junit.remote.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.runner.RunWith;
import org.junit.runner.Runner;

import br.ufsc.bridge.junit.remote.http.internal.ParameterizedProxyRunner;

/**
 * When a class is annotated with <code>&#064;RunInContainerWith</code> or extends a class annotated
 * with <code>&#064;RunInContainerWith</code>, JICUnit will invoke the class it references to run the
 * tests in that class instead of the default runner in JUnit. 
 * <p>
 * The annotation will only be picked up if the class is also annotated with {@link RunWith}({@link RemoteHttpRunner}.class)
 * <p>
 * To for instance use the Suite runner on a suite that shall be run in the JEE container:
 * <pre>
 * &#064;RunWith(JicUnitRunner.class)
 * &#064;RunInContainerWith(SomeRunner.class)
 * &#064;SuiteClasses({ATest.class, BTest.class, CTest.class})
 *   public class ABCSuite {
 * }
 * </pre>
 *
 * @author lucas.persson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RunInContainerWith {
  /**
   * @return a Runner class (must have a constructor that takes a single Class to run)
   */
  Class<? extends Runner> value();
}
