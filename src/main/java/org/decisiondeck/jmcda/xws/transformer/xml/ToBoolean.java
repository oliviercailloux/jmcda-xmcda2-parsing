package org.decisiondeck.jmcda.xws.transformer.xml;

import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAVarious;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodParameters;

public class ToBoolean implements FunctionWithInputCheck<XMethodParameters, Boolean> {
    @Override
    public Boolean apply(XMethodParameters input) throws InvalidInputException {
	return input == null ? null : new XMCDAVarious().readBoolean(input, null);
    }

}
