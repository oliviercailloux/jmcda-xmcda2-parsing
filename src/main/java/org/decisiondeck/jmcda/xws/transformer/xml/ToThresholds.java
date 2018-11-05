package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Collection;

import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;

public class ToThresholds implements FunctionWithInputCheck<Collection<XCriteria>, Thresholds> {
    @Override
    public Thresholds apply(Collection<XCriteria> input) throws InvalidInputException {
	if (input == null) {
	    return null;
	}
	final XMCDACriteria xmcdaCriteria = new XMCDACriteria();
	xmcdaCriteria.readAll(input);
	return xmcdaCriteria.getThresholds();
    }

}
