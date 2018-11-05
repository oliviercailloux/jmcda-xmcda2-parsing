package org.decisiondeck.jmcda.xws.transformer.xml;

import org.decision_deck.jmcda.structure.scores.AlternativesScores;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternativesScores;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesValues;

import com.google.common.base.Function;

public class FromScores implements Function<AlternativesScores, XAlternativesValues> {
    @Override
    public XAlternativesValues apply(AlternativesScores scores) {
	return new XMCDAAlternativesScores().writeScoredAlternatives(scores);
    }
}