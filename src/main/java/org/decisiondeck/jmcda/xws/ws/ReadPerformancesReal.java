package org.decisiondeck.jmcda.xws.ws;

import java.util.List;

import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeType;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;

public class ReadPerformancesReal extends ReadPerformancesUsingConcept {

    public ReadPerformancesReal() {
	super(XAlternativeType.REAL);
    }

    @Override
    public Evaluations apply(List<XPerformanceTable> input) throws InvalidInputException {
	return super.apply(input);
    }

}