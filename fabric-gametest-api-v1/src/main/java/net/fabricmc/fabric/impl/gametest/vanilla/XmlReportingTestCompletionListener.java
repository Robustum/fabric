package net.fabricmc.fabric.impl.gametest.vanilla;


import com.google.common.base.Stopwatch;

import net.fabricmc.fabric.impl.gametest.GameTestExtensions;

import net.minecraft.test.GameTest;

import net.minecraft.test.TestCompletionListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class XmlReportingTestCompletionListener implements TestCompletionListener {
	private final Document document;
	private final Element testSuiteElement;
	private final Stopwatch stopwatch;
	private final File file;

	public XmlReportingTestCompletionListener(File file) throws ParserConfigurationException {
		this.file = file;
		this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		this.testSuiteElement = this.document.createElement("testsuite");
		Element element = this.document.createElement("testsuite");
		element.appendChild(this.testSuiteElement);
		this.document.appendChild(element);
		this.testSuiteElement.setAttribute("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
		this.stopwatch = Stopwatch.createStarted();
	}

	private Element addTestCase(GameTest test, String name) {
		Element element = this.document.createElement("testcase");
		element.setAttribute("name", name);
		element.setAttribute("classname", test.getStructureName());
		element.setAttribute("time", String.valueOf((double) ((GameTestExtensions) test).fabric_getElapsedMilliseconds() / 1000.0));
		this.testSuiteElement.appendChild(element);
		return element;
	}

	@Override
	public void onTestFailed(GameTest test) {
		String string = test.getStructurePath();
		Throwable throwable = test.getThrowable();
		String string2 = throwable != null ? throwable.getMessage() : "Unknown error";
		Element element = this.document.createElement(test.isRequired() ? "failure" : "skipped");
		element.setAttribute("message", "(" + test.getPos().toShortString() + ") " + string2);
		Element element2 = this.addTestCase(test, string);
		element2.appendChild(element);
	}

	//	@Override
	public void onTestPassed(GameTest test) {
		String string = test.getStructurePath();
		this.addTestCase(test, string);
	}

	//	@Override
	public void onStopped() {
		this.stopwatch.stop();
		this.testSuiteElement.setAttribute("time", String.valueOf((double) this.stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0));

		try {
			this.saveReport(this.file);
		} catch (TransformerException var2) {
			throw new Error("Couldn't save test report", var2);
		}
	}

	public void saveReport(File file) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource dOMSource = new DOMSource(this.document);
		StreamResult streamResult = new StreamResult(file);
		transformer.transform(dOMSource, streamResult);
	}
}
