package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Map;

import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternativesMatrix;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesComparisons;

public class ToMatrixesByCriteria implements
	FunctionWithInputCheck<XAlternativesComparisons, Map<Criterion, SparseAlternativesMatrixFuzzy>> {
    @Override
    public Map<Criterion, SparseAlternativesMatrixFuzzy> apply(XAlternativesComparisons matrixes) throws InvalidInputException {
	return matrixes == null ? null : new XMCDAAlternativesMatrix()
		.readAlternativesFuzzyMatrixesByCriteria(matrixes);
    }
}