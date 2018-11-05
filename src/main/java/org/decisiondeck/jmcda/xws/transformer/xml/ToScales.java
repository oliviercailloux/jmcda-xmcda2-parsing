package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Collection;
import java.util.Map;

import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;

public class ToScales implements FunctionWithInputCheck<Collection<XCriteria>, Map<Criterion, Interval>> {
    @Override
    public Map<Criterion, Interval> apply(Collection<XCriteria> input) throws InvalidInputException {
	if (input == null) {
	    return null;
	}
	final XMCDACriteria xmcdaCriteria = new XMCDACriteria();
	xmcdaCriteria.readAll(input);
	return xmcdaCriteria.getScales();
    }

}
