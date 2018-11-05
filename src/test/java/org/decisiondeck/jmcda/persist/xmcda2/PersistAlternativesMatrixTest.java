package org.decisiondeck.jmcda.persist.xmcda2;

import static org.junit.Assert.assertTrue;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.utils.matrix.SparseMatrixD;
import org.decision_deck.utils.matrix.SparseMatrixDRead;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesComparisons;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

public class PersistAlternativesMatrixTest {
	private static final Logger s_logger = LoggerFactory.getLogger(PersistAlternativesMatrixTest.class);

	@Test
	public void readTest() throws Exception {
		final SixRealCars data = SixRealCars.getInstance();
		final ByteSource supplier = Resources.asByteSource(getClass().getResource("SixRealCars - Concordance.xml"));
		final SparseMatrixD<Alternative, Alternative> read = new XMCDAAlternativesMatrix().readAlternativesFloatMatrix(
				Iterables.getOnlyElement(new XMCDAReadUtils().getXMCDA(supplier).getAlternativesComparisonsList()));

		assertTrue("Deserialized concordance does not match.", data.getConcordance().approxEquals(read, 0.00005f));
	}

	@Test
	public void writeTest() throws Exception {
		final SixRealCars data = SixRealCars.getInstance();
		final SparseMatrixDRead<Alternative, Alternative> conc = data.getConcordance();
		final XAlternativesComparisons written = new XMCDAAlternativesMatrix().write(conc);
		final SparseMatrixD<Alternative, Alternative> read = new XMCDAAlternativesMatrix()
				.readAlternativesFloatMatrix(written);
		assertTrue("Deserialized concordance does not match.", conc.approxEquals(read, 0.00005f));
	}

}
