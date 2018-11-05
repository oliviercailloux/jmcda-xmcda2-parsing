package org.decisiondeck.jmcda.xws.transformer;

import java.util.Arrays;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;

import com.google.common.base.Preconditions;

class InputTransformerDocToTag implements FunctionWithInputCheck<XMCDADoc, XmlObject> {
    final private String m_tagName;
    private final XMCDAReadUtils m_utils = new XMCDAReadUtils();

    /**
     * @param tagName
     *            not <code>null</code>.
     */
    public InputTransformerDocToTag(String tagName) {
	Preconditions.checkNotNull(tagName);
	m_tagName = tagName;
    }

    @Override
    public XmlObject apply(XMCDADoc doc) throws InvalidInputException {
	if (doc == null) {
	    return null;
	}
	final List<XmlObject> tags = Arrays.asList(doc.getXMCDA().selectPath("$this/child::" + m_tagName));
	return m_utils.getUnique(tags, m_tagName);
    }
}