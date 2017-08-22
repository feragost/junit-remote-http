package br.ufsc.bridge.junit.remote.http;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

import br.ufsc.bridge.junit.remote.http.internal.BasicProxyRunner;
import br.ufsc.bridge.junit.remote.http.internal.ParameterizedProxyRunner;

/**
 * A JUnit4 runner that delegates the actual execution of the test to be
 * performed in a JEE container. This is done via a HTTP call to the
 * <code>JicUnitServlet</code> via the URL specified in the System property of
 * <code>jicunit.url</code>. Typical the URL should be set to
 * <code> http://localhost:7001/my-jicunit-war/tests</code>
 *
 * <p>
 * When the test is executed locally then this runner will delegate the
 * execution to the runner in the container. However this runner is also
 * instantiated in the container and then this runner will delegate the
 * execution to the default JUnit runner (or the runner specified by the
 * {@link RunInContainerWith} annotation).
 *
 *
 * @author lucas.persson
 *
 */
public class RemoteHttpRunner extends Runner implements Filterable, Sortable {

	private static Logger sLog = Logger.getLogger(RemoteHttpRunner.class.getName());

	private Runner mRunner;

	public RemoteHttpRunner(Class<?> testClass) throws Throwable {
		Class<? extends Runner> runnerClass;
		RunInContainerWith runInContainerWith = Utils.findAnnotation(testClass, RunInContainerWith.class);
		// figure out if this is happening locally or in the JEE container
		String containerUrl = this.getContainerUrl();
		if (containerUrl == null) {
			// this code is executed in the JEE container
			if (runInContainerWith != null) {
				runnerClass = runInContainerWith.value();
			} else {
				runnerClass = JUnit4.class;
			}
			try {
				Constructor<? extends Runner> constructor = runnerClass.getDeclaredConstructor(Class.class);
				this.mRunner = constructor.newInstance(testClass);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				Throwable cause = e.getCause() != null ? e.getCause() : e;
				String msg = "Unable to create instance of " + runnerClass + " using test class " + testClass.getName() + " Reason: " + cause;
				if (cause instanceof InitializationError) {
					InitializationError initializationError = (InitializationError) cause;
					msg = msg + " " + initializationError.getCauses();
				}
				sLog.log(Level.SEVERE, msg);
				throw new RuntimeException(msg, cause);
			}
		} else {
			// this code is executed locally so create a ProxyRunner which will
			// forward the execution to the container
			if (runInContainerWith != null) {
				runnerClass = runInContainerWith.value();
				if (Parameterized.class.isAssignableFrom(runnerClass)) {
					this.mRunner = new ParameterizedProxyRunner(testClass, containerUrl);
				} else if (Suite.class.isAssignableFrom(runnerClass)) {
					throw new IllegalArgumentException(
							RunInContainerWith.class.getSimpleName() + " annotation does not support Suite runner or any subclass of Suite except Parameterized");
				} else {
					Runner runInContainerRunner = runnerClass.getDeclaredConstructor(Class.class).newInstance(testClass);
					Description desc = runInContainerRunner.getDescription();
					this.mRunner = new BasicProxyRunner(testClass, containerUrl, desc);
				}
			} else {
				this.mRunner = new BasicProxyRunner(testClass, containerUrl);
			}
		}

	}

	@Override
	public Description getDescription() {
		Description description = this.mRunner.getDescription();
		return description;
	}

	@Override
	public void run(RunNotifier notifier) {
		this.mRunner.run(notifier);
	}

	@Override
	public void sort(Sorter sorter) {
		// it is not sure that the custom runner support sorting so sorting is done
		// here too.
		if (this.mRunner instanceof Sortable) {
			Sortable sortableRunner = (Sortable) this.mRunner;
			sortableRunner.sort(sorter);
		}
	}

	@Override
	public void filter(Filter filter) throws NoTestsRemainException {
		// it is not sure that the custom runner support filtering so filtering is
		// done here too.
		if (this.mRunner instanceof Filterable) {
			Filterable filterableRunner = (Filterable) this.mRunner;
			filterableRunner.filter(filter);
		}
	}

	/**
	 * Retrieves the container from the system property
	 * {@link RemoteHttpRunner#CONTAINER_URL}.
	 *
	 * @return URL pointing to the container
	 * @throws IOException
	 */
	protected String getContainerUrl() throws IOException {
		return this.getProperties().getProperty("host");
	}

	public Properties getProperties() throws IOException {
		Properties props = new Properties();
		InputStream inputStream = this.getClass().getResourceAsStream("/properties/junitremotehttp.properties");
		props.load(inputStream);
		return props;
	}

}
