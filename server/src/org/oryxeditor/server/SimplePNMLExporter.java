package org.oryxeditor.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import de.hpi.bpmn.BPMNDiagram;
import de.hpi.bpmn.rdf.BPMNRDFImporter;
import de.hpi.bpmn2pn.converter.StandardConverter;
import de.hpi.ibpmn.IBPMNDiagram;
import de.hpi.ibpmn.converter.IBPMNConverter;
import de.hpi.ibpmn.rdf.IBPMNRDFImporter;
import de.hpi.interactionnet.InteractionNet;
import de.hpi.interactionnet.pnml.InteractionNetPNMLExporter;
import de.hpi.interactionnet.rdf.InteractionNetRDFImporter;
import de.hpi.petrinet.PetriNet;
import de.hpi.petrinet.pnml.PetriNetPNMLExporter;

/**
 * Copyright (c) 2008 Lutz Gericke, Gero Decker
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class SimplePNMLExporter extends HttpServlet {
	private static final long serialVersionUID = -8374877061121257562L;
	private static Configuration config = null;
	
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		try {
			res.setContentType("text/pnml+xml");

			String rdf = req.getParameter("data");

			DocumentBuilder builder;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			Document document = builder.parse(new ByteArrayInputStream(rdf.getBytes()));
			Document pnmlDoc = builder.newDocument();
			
			processDocument(document, pnmlDoc);
			
			OutputFormat format = new OutputFormat(pnmlDoc);

			StringWriter stringOut = new StringWriter();
			XMLSerializer serial2 = new XMLSerializer(stringOut, format);
			serial2.asDOMSerializer();

			serial2.serialize(pnmlDoc.getDocumentElement());

			res.getWriter().print(stringOut.toString());
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	protected void processDocument(Document document, Document pnmlDoc) {
		String type = getDocumentType(document);
		if (type.equals("ibpmn.json"))
			processIBPMN(document, pnmlDoc);
		else if (type.equals("bpmn.json"))
			processBPMN(document, pnmlDoc);
		else if (type.equals("interactionpetrinets.json"))
			processIPN(document, pnmlDoc);
	}
	
	protected void processBPMN(Document document, Document pnmlDoc) {
		BPMNRDFImporter importer = new BPMNRDFImporter(document);
		BPMNDiagram diagram = (BPMNDiagram) importer.loadBPMN();

		PetriNet net = new StandardConverter(diagram).convert();

		PetriNetPNMLExporter exp = new PetriNetPNMLExporter();
		exp.savePetriNet(pnmlDoc, net);
	}

	protected void processIBPMN(Document document, Document pnmlDoc) {
		IBPMNRDFImporter importer = new IBPMNRDFImporter(document);
		BPMNDiagram diagram = (IBPMNDiagram) importer.loadIBPMN();

		PetriNet net = new IBPMNConverter(diagram).convert();

		InteractionNetPNMLExporter exp = new InteractionNetPNMLExporter();
		exp.savePetriNet(pnmlDoc, net);
	}

	protected void processIPN(Document document, Document pnmlDoc) {
		InteractionNetRDFImporter importer = new InteractionNetRDFImporter(document);
		InteractionNet net = (InteractionNet) importer.loadInteractionNet();

		InteractionNetPNMLExporter exp = new InteractionNetPNMLExporter();
		exp.savePetriNet(pnmlDoc, net);
	}


	
	protected String getDocumentType(Document doc) {
		Node node = doc.getDocumentElement();
		if (node == null || !node.getNodeName().equals("rdf:RDF"))
			return null;
		
		node = node.getFirstChild();
		while (node != null) {
			 String about = getAttributeValue(node, "rdf:about");
			 if (about != null && about.contains("oryxcanvas")) break;
			 node = node.getNextSibling();
		}
		String type = getAttributeValue(getChild(node, "stencilset"), "rdf:resource");
		if (type != null)
			return type.substring(type.lastIndexOf('/')+1);
		
		return null;
	}

//	protected String getContent(Node node) {
//		if (node != null && node.hasChildNodes())
//			return node.getFirstChild().getNodeValue();
//		return null;
//	}
	
	private String getAttributeValue(Node node, String attribute) {
		Node item = node.getAttributes().getNamedItem(attribute);
		if (item != null)
			return item.getNodeValue();
		else
			return null;
	}

	private Node getChild(Node n, String name) {
		if (n == null)
			return null;
		for (Node node=n.getFirstChild(); node != null; node=node.getNextSibling())
			if (node.getNodeName().indexOf(name) >= 0) 
				return node;
		return null;
	}

}
