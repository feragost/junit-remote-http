package br.ufsc.bridge.junit.remote.http.internal.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import br.ufsc.bridge.junit.remote.http.internal.InContainerTestRunner;

/**
 * Describes a test and it status or a collection of tests.
 * Used in the GUI and in the {@link InContainerTestRunner}
 *
 *
 * @author lucas
 *
 */
@XmlRootElement(name = "testcase")
public class TestDescription {

	public enum Status {
		OK,
		Ignored,
		Error,
		Failure
	};

	/**
	 * Display name of the test like "testSome(com.example.SomeTest)" or "com.example.SomeTest"
	 */
	private String mDisplayName;
	private String mClassName;

	private Status mStatus;

	private long mStartTime;
	/**
	 * Execution time in ms
	 */
	private Long mTime;

	private List<ExceptionDescription> mExceptionDescriptions = new ArrayList<>();
	// private ExceptionDescription mExceptionDescription;
	private List<TestDescription> mTestDescriptions = new ArrayList<>();

	/**
	 * Needed by JAXB, not supposed to be used.
	 */
	public TestDescription() {
	}

	public TestDescription(String displayName, String className) {
		this.mDisplayName = displayName;
		this.mClassName = className;
	}

	public void clearResult() {
		this.mStatus = null;
		this.mStartTime = 0;
		this.mTime = null;
		this.mTestDescriptions.clear();
		// mExceptionDescription = null;

		if (this.isSuite()) {
			for (TestDescription testDescription : this.mTestDescriptions) {
				testDescription.clearResult();
			}
		}

	}

	/**
	 *
	 * @return the display name
	 */
	@XmlAttribute(name = "name")
	public String getDisplayName() {
		return this.mDisplayName;
	}

	public void setDisplayName(String displayName) {
		this.mDisplayName = displayName;
	}

	/**
	 * Like DisplayName but omitting the class name part
	 */
	@XmlTransient
	public String getShortName() {
		if (this.mDisplayName.indexOf('(') != -1) {
			return this.mDisplayName.substring(0, this.mDisplayName.lastIndexOf('('));
		} else {
			return this.mDisplayName;
		}
	}

	/**
	 * @return name of the test class
	 */
	@XmlAttribute(name = "classname")
	public String getClassName() {
		return this.mClassName;
	}

	@XmlAttribute
	public Status getStatus() {
		if (this.isSuite()) {
			// aggregate status, Error and Failure have precedence
			Status status = null;
			for (TestDescription testDescription : this.mTestDescriptions) {
				Status childStatus = testDescription.getStatus();
				if (childStatus != null) {
					if (childStatus.equals(Status.Error) || childStatus.equals(Status.Failure)) {
						status = childStatus;
					} else {
						// only set to childStatus if aggregate status has not ever been set
						if (status == null) {
							status = childStatus;
						}
					}
				}
			}
			return status;
		} else {
			return this.mStatus;
		}
	}

	public void setStatus(Status status) {
		this.mStatus = status;
	}

	@XmlTransient
	public long getStartTime() {
		return this.mStartTime;
	}

	public void setStartTime(long startTime) {
		this.mStartTime = startTime;
	}

	/**
	 *
	 * @return execution time in ms
	 */
	@XmlAttribute
	public Long getTime() {
		return this.mTime;
	}

	public void setTime(Long time) {
		this.mTime = time;
	}

	/**
	 *
	 * @return execution time in seconds
	 */
	@XmlTransient
	public Double getTimeAsSeconds() {
		if (this.isSuite()) {
			// aggregate time
			Double time = null;
			for (TestDescription testDescription : this.mTestDescriptions) {
				Double childTime = testDescription.getTimeAsSeconds();
				if (childTime != null) {
					if (time == null) {
						time = new Double(0);
					}
					time = time + childTime;
				}
			}
			return time;
		} else {
			if (this.mTime != null) {
				return (double) this.mTime / 1000;
			} else {
				return null;
			}
		}
	}

	@XmlElement(name = "exceptions")
	public List<ExceptionDescription> getExceptionDescription() {
		return this.mExceptionDescriptions;
	}

	public void addExceptionDescription(ExceptionDescription exceptionDescription) {
		this.mExceptionDescriptions.add(exceptionDescription);
	}

	// @XmlElement(name = "exception")
	// public ExceptionDescription getExceptionDescription() {
	// return mExceptionDescription;
	// }
	//
	// public void setExceptionDescription(ExceptionDescription exceptionDescription) {
	// mExceptionDescription = exceptionDescription;
	// }

	@XmlTransient
	public List<TestDescription> getTestDescriptions() {
		return this.mTestDescriptions;
	}

	public void setTestDescriptions(List<TestDescription> testDescriptions) {
		this.mTestDescriptions = testDescriptions;
	}

	public void addTestDescription(TestDescription testDescriptionChild) {
		this.mTestDescriptions.add(testDescriptionChild);
	}

	@XmlTransient
	public boolean isSuite() {
		return this.mTestDescriptions.size() > 0;
	}

	@Override
	public String toString() {
		return this.mDisplayName;
	}

}
