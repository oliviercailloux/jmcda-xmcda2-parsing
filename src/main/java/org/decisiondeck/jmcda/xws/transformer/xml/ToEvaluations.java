package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Collection;

import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAEvaluations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;

public class ToEvaluations implements FunctionWithInputCheck<Collection<XPerformanceTable>, Evaluations> {
    @Override
    public Evaluations apply(Collection<XPerformanceTable> input) throws InvalidInputException {
	return input == null ? null : new XMCDAEvaluations().read(input);
    }

}
