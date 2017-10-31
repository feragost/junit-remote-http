package br.ufsc.bridge.junit.remote.http.internal;

import static br.ufsc.bridge.junit.remote.http.internal.JicUnitServlet.TEST_CLASS_NAME_PARAM;
import static br.ufsc.bridge.junit.remote.http.internal.JicUnitServlet.TEST_NAME_PARAM;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import br.ufsc.bridge.junit.remote.http.Utils;
import br.ufsc.bridge.junit.remote.http.internal.model.TestDescription.Status;

/**
 * This class does the actual call to the servlet.
 *
 * @author lucas
 *
 */
public class JicUnitServletClient {

	private static final String ENCODING = "UTF-8";

	public JicUnitServletClient() {
	}

	/**
	 * Execute the test at the server
	 *
	 * @param containerUrl
	 * @param testClassName
	 * @param testDisplayName
	 * @return
	 */
	public Document runAtServer(String containerUrl, String testClassName, String testDisplayName) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			String url = containerUrl + String.format("?%s=%s&%s=%s", TEST_CLASS_NAME_PARAM, URLEncoder.encode(testClassName, ENCODING), TEST_NAME_PARAM,
					URLEncoder.encode(testDisplayName, ENCODING));
			builder = factory.newDocumentBuilder();
			Document document = builder.parse(url);

			return document;
		} catch (FileNotFoundException fe) {
			throw new RuntimeException("Could not find the test WAR at the server. Make sure jicunit.url points to the correct context root.", fe);
		} catch (ConnectException ce) {
			throw new RuntimeException("Could not connect to the server. Make sure jicunit.url points to the correct host and port.", ce);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException("Could not run the test. Check the jicunit.url so it is correct.", e);
		}
	}

	/**
	 * Processes the actual XML result and generate exceptions in case there was
	 * an issue on the server side.
	 *
	 * @param document
	 * @throws Throwable
	 */
	public List<Throwable> processResults(Document document) throws Throwable {
		Element root = document.getDocumentElement();
		root.normalize();

		// top element should testcase which has attributes for the outcome of the test
		// e.g <testcase classname="TheClass" name="theName(TheClass)" status="Error"><exception message="message" type="type">some text</exception></testcase>
		NamedNodeMap attributes = root.getAttributes();
		String statusAsStr = attributes.getNamedItem("status").getNodeValue();
		Status status = Status.valueOf(statusAsStr);

		if (status.equals(Status.Error) || status.equals(Status.Failure)) {
			return this.getExceptions(root);
		} else {
			return new LinkedList<>();
		}
	}

	private List<Throwable> getExceptions(Element root) {
		List<Throwable> exceptions = new LinkedList<>();
		NodeList nodes = root.getElementsByTagName("exceptions");
		for (int i = 0; i < nodes.getLength(); i++) {
			Node item = nodes.item(i);
			exceptions.add(this.getException(item));
		}
		return exceptions;
	}

	private Throwable getException(Node node) {

		NamedNodeMap attributes = node.getAttributes();
		String message = "";
		Node messageNode = attributes.getNamedItem("message");
		if (messageNode != null) {
			message = messageNode.getNodeValue();
		}
		String type = "";
		Node typeNode = attributes.getNamedItem("type");
		if (typeNode != null) {
			type = typeNode.getNodeValue();
		}

		String status = "";
		Node statusNode = attributes.getNamedItem("status");
		if (statusNode != null) {
			status = statusNode.getNodeValue();
		}

		String stackTrace = node.getTextContent();

		Throwable throwable = null;
		Node throwableNode = attributes.getNamedItem("throwable");
		if (throwableNode != null) {
			String stringSerialThrowable = throwableNode.getNodeValue();
			throwable = this.unserializeThrowable(stringSerialThrowable, type);
		}

		if (throwable != null) {
			return throwable;
		} else if (status.equals(Status.Error.toString())) {
			ExceptionWrapper e = new ExceptionWrapper(message, type, stackTrace);
			return e;
		} else {
			AssertionErrorWrapper e = new AssertionErrorWrapper(message, type, stackTrace);
			return e;
		}

	}

	private Throwable unserializeThrowable(String serialThrowable, String type) {
		try {
			Class<? extends Throwable> klazz = (Class<? extends Throwable>) Class.forName(type);
			return Utils.deserialize(serialThrowable, klazz);
		} catch (ClassNotFoundException e) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public String getString(Document doc) {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
			return output;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

}
