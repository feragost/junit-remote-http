package br.ufsc.bridge.junit.remote.http.internal.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import br.ufsc.bridge.junit.remote.http.internal.model.TestDescription.Status;

public class ExceptionDescription {
	private String mMessage;
	private String mType;
	private String mStackTrace;
	private Status mStatus;
	private String mSerialThroable;

	public ExceptionDescription() {

	}

	public ExceptionDescription(String message, String type, String stackTrace, Status status) {
		super();
		this.mMessage = message;
		this.mType = type;
		this.mStackTrace = stackTrace;
		this.mStatus = status;
	}

	@XmlAttribute
	public String getMessage() {
		return this.mMessage;
	}

	public void setMessage(String message) {
		this.mMessage = message;
	}

	/**
	 * @return the name of the Exception/Error class
	 */
	@XmlAttribute
	public String getType() {
		return this.mType;
	}

	public void setType(String type) {
		this.mType = type;
	}

	@XmlValue
	public String getStackTrace() {
		return this.mStackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.mStackTrace = stackTrace;
	}

	@XmlAttribute(name = "status")
	public Status getStatus() {
		return this.mStatus;
	}

	public void setStatus(Status status) {
		this.mStatus = status;
	}

	@XmlAttribute(name = "throwable")
	public String getSerialThrowable() {
		return this.mSerialThroable;
	}

	public void setSerialThrowable(String t) {
		this.mSerialThroable = t;
	}

	@Override
	public String toString() {
		return "ExceptionResult [mMessage=" + this.mMessage + ", mType=" + this.mType + ", mStackTrace=" + this.mStackTrace + "]";
	}
}
