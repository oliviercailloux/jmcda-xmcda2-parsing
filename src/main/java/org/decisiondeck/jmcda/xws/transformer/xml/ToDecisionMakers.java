package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Set;

import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDADecisionMakers;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodParameters;

public class ToDecisionMakers implements FunctionWithInputCheck<XMethodParameters, Set<DecisionMaker>> {
    @Override
    public Set<DecisionMaker> apply(XMethodParameters input) throws InvalidInputException {
	return input == null ? null : new XMCDADecisionMakers().read(input);
    }

}
