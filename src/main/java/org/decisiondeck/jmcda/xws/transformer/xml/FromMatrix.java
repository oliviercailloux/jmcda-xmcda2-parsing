package org.decisiondeck.jmcda.xws.transformer.xml;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.utils.matrix.SparseMatrixFuzzyRead;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternativesMatrix;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesComparisons;

import com.google.common.base.Function;

public class FromMatrix implements Function<SparseMatrixFuzzyRead<Alternative, Alternative>, XAlternativesComparisons> {
    @Override
    public XAlternativesComparisons apply(SparseMatrixFuzzyRead<Alternative, Alternative> matrix) {
	return new XMCDAAlternativesMatrix().write(matrix);
    }
}