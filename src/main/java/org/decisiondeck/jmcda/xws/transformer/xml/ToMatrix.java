package org.decisiondeck.jmcda.xws.transformer.xml;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.utils.matrix.SparseMatrixD;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternativesMatrix;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesComparisons;

public class ToMatrix implements
	FunctionWithInputCheck<XAlternativesComparisons, SparseMatrixD<Alternative, Alternative>> {
    @Override
    public SparseMatrixD<Alternative, Alternative> apply(XAlternativesComparisons input) throws InvalidInputException {
	return input == null ? null : new XMCDAAlternativesMatrix().readAlternativesFloatMatrix(input);
    }
}