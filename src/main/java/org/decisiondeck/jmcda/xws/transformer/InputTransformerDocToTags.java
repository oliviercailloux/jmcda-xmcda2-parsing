package org.decisiondeck.jmcda.xws.transformer;

import java.util.Arrays;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;

import com.google.common.base.Preconditions;

class InputTransformerDocToTags implements FunctionWithInputCheck<XMCDADoc, List<XmlObject>> {
    final private String m_tagName;
    private boolean m_optional;

    /**
     * @param tagName
     *            not <code>null</code>.
     * @param optional
     *            <code>true</code> to not require that at least one such tag is found.
     */
    public InputTransformerDocToTags(String tagName, boolean optional) {
	Preconditions.checkNotNull(tagName);
	m_tagName = tagName;
	m_optional = optional;
    }

    @Override
    public List<XmlObject> apply(XMCDADoc doc) throws InvalidInputException {
	final List<XmlObject> list = doc == null ? null : Arrays.asList(doc.getXMCDA().selectPath(
		"$this/child::" + m_tagName));
	if (doc == null && !m_optional) {
	    throw new InvalidInputException("Didn't find the required tag " + m_tagName + ": document is missing.");
	}
	if (list == null || list.isEmpty()) {
	    if (m_optional) {
		return null;
	    }
	    throw new InvalidInputException("Didn't find the required tag " + m_tagName + " in given document.");
	}
	return list;
    }
}