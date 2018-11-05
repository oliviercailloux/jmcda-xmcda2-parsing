package org.decisiondeck.jmcda.persist.xmcda2;

import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.decision_deck.jmcda.structure.scores.AlternativesScores;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesValues;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.junit.Test;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

public class XMCDAAlternativesScoresTest {
	@Test
	public void serializeAndDeserializeSixRealCarsFlowsTest() throws Exception {
		final SixRealCars data = SixRealCars.getInstance();

		final AlternativesScores netFlows = data.getNetFlows();
		final XAlternativesValues written = new XMCDAAlternativesScores().writeScoredAlternatives(netFlows);
		final AlternativesScores readScores = new XMCDAAlternativesScores()
				.readScoredAlternatives(Collections.singleton(written));

		assertTrue("Situations after ser and deser do not match.", netFlows.approxEquals(readScores, 5e-5d));
	}

	@Test
	public void testRead() throws Exception {
		final SixRealCars data = SixRealCars.getInstance();
		final ByteSource supplier = Resources
				.asByteSource(getClass().getResource("SixRealCars - Promethee net flows.xml"));
		final AlternativesScores readScores = new XMCDAAlternativesScores()
				.readScoredAlternatives(new XMCDAReadUtils().getXMCDA(supplier).getAlternativesValuesList());
		assertTrue("Scores read do not match expected.", readScores.approxEquals(data.getNetFlows(), 0.00005f));
	}
}
