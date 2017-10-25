package br.ufsc.bridge.junit.remote.http.internal;

import java.util.List;

import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.w3c.dom.Document;

/**
 * This runner will forward all execution of the tests to a container
 *
 * @author lucas
 *
 */
public class BasicProxyRunner extends BlockJUnit4ClassRunner {

	private String mContainerUrl;
	private JicUnitServletClient mClient;
	private Description mDescription;

	public BasicProxyRunner(Class<?> testClass, String containerUrl) throws InitializationError {
		super(testClass);
		this.mContainerUrl = containerUrl;

		this.mClient = new JicUnitServletClient();
	}

	public BasicProxyRunner(Class<?> testClass, String containerUrl, Description description) throws InitializationError {
		this(testClass, containerUrl);
		this.mDescription = description;
	}

	@Override
	protected void collectInitializationErrors(List<Throwable> errors) {
		// bypass all validation of the test class since that will anyway be done
		// when the real runner is executing in the container
	}

	@Override
	public Description getDescription() {
		if (this.mDescription != null) {
			return this.mDescription;
		} else {
			return super.getDescription();
		}
	}

	/**
	 * This method will be called for the set of methods annotated with
	 * Before/Test/After.
	 */
	@Override
	protected void runChild(FrameworkMethod method, RunNotifier notifier) {

		Description description = this.describeChild(method);
		if (method.getAnnotation(Ignore.class) != null) {
			notifier.fireTestIgnored(description);
			return;
		}

		notifier.fireTestStarted(description);
		String testClassName = this.getTestClass().getJavaClass().getName();
		String testName = description.getDisplayName();

		try {
			Document result = this.mClient.runAtServer(this.mContainerUrl, testClassName, testName);
			List<Throwable> processResults = this.mClient.processResults(result);
			for (Throwable failure : processResults) {
				notifier.fireTestFailure(new Failure(description, failure));
				failure.printStackTrace();
				System.err.println();
				System.err.println();
			}
		} catch (Throwable e) {
			e.printStackTrace();
			notifier.fireTestFailure(new Failure(description, e));
		} finally {
			notifier.fireTestFinished(description);
		}
	}

	/**
	 * Suppress any BeforeClass annotation since it shall not be run locally.
	 */
	@Override
	protected Statement withBeforeClasses(Statement statement) {
		return statement;
	}

	/**
	 * Suppress any AfterClass annotation since it shall not be run locally.
	 */
	@Override
	protected Statement withAfterClasses(Statement statement) {
		return statement;
	}
}
