package org.decisiondeck.jmcda.persist.xmcda2;

import org.apache.xmlbeans.XmlException;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.junit.Test;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

public class XMCDAUtilsTest {

    @Test(expected = XmlException.class)
    public void testInvalidXMCDA() throws Exception {
	final ByteSource supplier = Resources.asByteSource(getClass().getResource(
		"Invalid xmcda.txt"));
	new XMCDAReadUtils().getXMCDA(supplier);
    }

    @Test(expected = XmlException.class)
    public void testInvalidXML() throws Exception {
	final ByteSource supplier = Resources.asByteSource(getClass().getResource(
		"Invalid XML.txt"));
	new XMCDAReadUtils().getXMCDA(supplier);
    }

}
