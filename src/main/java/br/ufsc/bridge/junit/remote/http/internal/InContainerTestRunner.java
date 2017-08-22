package br.ufsc.bridge.junit.remote.http.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.JUnit4;

import br.ufsc.bridge.junit.remote.http.RunInContainerWith;
import br.ufsc.bridge.junit.remote.http.Utils;
import br.ufsc.bridge.junit.remote.http.internal.model.ExceptionDescription;
import br.ufsc.bridge.junit.remote.http.internal.model.TestDescription;
import br.ufsc.bridge.junit.remote.http.internal.model.TestDescription.Status;

/**
 * Runs the actual test in the container and format the result. It uses
 * <code>org.junit.runner.JUnitCore</code> to actually execute the tests that
 * handles both JUnit3 and JUnit4 type of tests.
 *
 * @author lucas
 *
 */
public class InContainerTestRunner extends RunListener {

	private JUnitCore mJUnitCore;
	private TestDescription mTestDescription;

	public InContainerTestRunner() {
		this.mJUnitCore = new JUnitCore();
		this.mJUnitCore.addListener(this);
	}

	/**
	 *
	 * @param classLoader
	 * @param testDescription description of the test that shall be run.
	 * @return testDescription with update result
	 * @throws ClassNotFoundException
	 */
	public TestDescription runTest(ClassLoader classLoader, TestDescription testDescription) throws ReflectiveOperationException {
		this.mTestDescription = testDescription;
		this.mTestDescription.clearResult();
		String testClassName = this.mTestDescription.getClassName();
		String testDisplayName = this.mTestDescription.getDisplayName();
		Filter filter = this.createMethodFilter(testClassName, testDisplayName);
		return this.runTest(classLoader, testClassName, filter);
	}

	private Filter createMethodFilter(final String testClassName, final String testDisplayName) {
		String testMethod = testDisplayName.substring(0, testDisplayName.lastIndexOf('('));
		return Filter.matchMethodDescription(Description.createTestDescription(testClassName, testMethod));
	}

	private TestDescription runTest(ClassLoader classLoader, String testClassName, Filter filter)
			throws ReflectiveOperationException,
			InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?> clazz = classLoader.loadClass(testClassName);

		Class<? extends Runner> runnerClazz = JUnit4.class;
		RunInContainerWith runInContainerWith = Utils.findAnnotation(clazz, RunInContainerWith.class);
		if (runInContainerWith != null) {
			runnerClazz = runInContainerWith.value();
		}
		Constructor<? extends Runner> constructor = runnerClazz.getConstructor(Class.class);
		Runner runnerInstance = constructor.newInstance(clazz);

		Request request = Request.runner(runnerInstance);
		request = request.filterWith(filter);

		this.mJUnitCore.run(request);

		return this.mTestDescription;
	}

	@Override
	public void testRunStarted(Description description) throws Exception {
	}

	@Override
	public void testRunFinished(Result result) throws Exception {
	}

	@Override
	public void testStarted(Description description) throws Exception {
		this.mTestDescription.setStartTime(System.currentTimeMillis());
	}

	// testFinished will be called last. I.e after testFailure
	@Override
	public void testFinished(Description description) throws Exception {
		long stopTime = System.currentTimeMillis();
		this.mTestDescription.setTime(stopTime - this.mTestDescription.getStartTime());
		if (this.mTestDescription.getStatus() == null) {
			this.mTestDescription.setStatus(Status.OK);
		}
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		Throwable exception = failure.getException();
		ExceptionDescription exceptionDescription = new ExceptionDescription(failure.getMessage(), exception.getClass().getName(),
				ExceptionUtil.filterdStackTrace(failure.getTrace()));
		this.mTestDescription.setExceptionDescription(exceptionDescription);

		if (exception instanceof AssertionError) {
			// -> this is a failure
			this.mTestDescription.setStatus(Status.Failure);
		} else {
			// -> this is an error
			this.mTestDescription.setStatus(Status.Error);
		}

	}

	@Override
	public void testIgnored(Description description) throws Exception {
		this.mTestDescription.setStatus(Status.Ignored);
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		try {
			this.testFailure(failure);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the TestDescription as XML
	 *
	 * @param testDescription
	 * @return XML as String
	 */
	public String resultAsXml(TestDescription testDescription) {
		XmlUtil xmlUtil = new XmlUtil();
		String xml = xmlUtil.convertToXml(testDescription, testDescription.getClass());
		return xml;

	}

}
