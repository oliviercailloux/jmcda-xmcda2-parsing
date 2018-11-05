package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Collection;

import org.decision_deck.jmcda.structure.weights.Weights;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaValues;

import com.google.common.collect.Iterables;

public class ToWeights implements FunctionWithInputCheck<Collection<XCriteriaValues>, Weights> {
    @Override
    public Weights apply(Collection<XCriteriaValues> input) throws InvalidInputException {
	if (input == null) {
	    return null;
	}
	final XMCDACriteria reader = new XMCDACriteria();
	if (input.size() == 1) {
	    return reader.readWeights(Iterables.getOnlyElement(input));
	}
	return reader.readWeights(input);
    }

}
