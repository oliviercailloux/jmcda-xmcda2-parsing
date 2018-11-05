package org.decisiondeck.jmcda.xws.transformer.xml;

import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAVarious;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodParameters;

public class ToString implements FunctionWithInputCheck<XMethodParameters, String> {
    @Override
    public String apply(XMethodParameters input) throws InvalidInputException {
	return input == null ? null : new XMCDAVarious().readLabel(input, null);
    }

}
