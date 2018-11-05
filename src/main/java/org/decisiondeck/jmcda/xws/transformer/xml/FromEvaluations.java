package org.decisiondeck.jmcda.xws.transformer.xml;

import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAEvaluations;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;

import com.google.common.base.Function;

public class FromEvaluations implements Function<EvaluationsRead, XPerformanceTable> {
    @Override
    public XPerformanceTable apply(EvaluationsRead input) {
	return new XMCDAEvaluations().write(input);
    }
}