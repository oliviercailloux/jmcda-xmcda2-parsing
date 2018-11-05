package org.decisiondeck.jmcda.xws.transformer;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;

import com.google.common.io.ByteSource;

class InputTransformerSourceToDoc implements
	FunctionWithInputCheck<ByteSource, XMCDADoc> {
    private final XMCDAReadUtils m_utils = new XMCDAReadUtils();

    @Override
    public XMCDADoc apply(ByteSource source) throws InvalidInputException {
	if (source == null) {
	    return null;
	}
	try {
	    return m_utils.getXMCDADoc(source);
	} catch (IOException exc) {
	    throw new InvalidInputException(exc);
	} catch (XmlException exc) {
	    throw new InvalidInputException(exc);
	}
    }
}