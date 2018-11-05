package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.List;
import java.util.Map;

import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaSet;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteriaSets;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;

import com.google.common.base.Function;

public class FromCoalitionsByDecisionMaker implements Function<Map<DecisionMaker, Coalitions>, XCriteriaSets> {
    @Override
    public XCriteriaSets apply(Map<DecisionMaker, Coalitions> coalitionsByDecisionMaker) {
	final List<XCriteriaSet> sets = new XMCDACriteria().write(coalitionsByDecisionMaker);
	final XCriteriaSets xSets = XMCDA.Factory.newInstance().addNewCriteriaSets();
	for (XCriteriaSet set : sets) {
	    xSets.addNewCriteriaSet().set(set);
	}
	return xSets;
    }
}