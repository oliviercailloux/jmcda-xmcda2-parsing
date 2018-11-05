package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Collection;
import java.util.Set;

import org.decision_deck.jmcda.structure.Criterion;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;

public class ToCriteria implements FunctionWithInputCheck<Collection<XCriteria>, Set<Criterion>> {
    @Override
    public Set<Criterion> apply(Collection<XCriteria> input) throws InvalidInputException {
	return input == null ? null : new XMCDACriteria().readAll(input);
    }

}
