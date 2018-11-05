package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Map;

import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternativesMatrix;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesComparisons;

import com.google.common.base.Function;

public class FromMatrixesByCriteria implements Function<Map<Criterion, SparseAlternativesMatrixFuzzy>, XAlternativesComparisons> {
    @Override
    public XAlternativesComparisons apply(Map<Criterion, SparseAlternativesMatrixFuzzy> matrixes) {
	return new XMCDAAlternativesMatrix().write(matrixes);
    }
}