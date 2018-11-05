package org.decisiondeck.jmcda.xws.ws;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAEvaluations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeType.Enum;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;

public class ReadPerformancesUsingConcept implements FunctionWithInputCheck<List<XPerformanceTable>, Evaluations> {
    private final Enum m_concept;

    public ReadPerformancesUsingConcept(Enum concept) {
	checkNotNull(concept);
	m_concept = concept;
    }

    @Override
    public Evaluations apply(List<XPerformanceTable> input) throws InvalidInputException {
	if (input == null) {
	    return null;
	}
	if (input.size() == 1) {
	    final XMCDAEvaluations reader = new XMCDAEvaluations();
	    return reader.read(input);
	}
	final XMCDAEvaluations reader = new XMCDAEvaluations();
	reader.setConceptToRead(m_concept);
	return reader.read(input);
    }

}