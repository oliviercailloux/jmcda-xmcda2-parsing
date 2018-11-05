package org.decisiondeck.jmcda.xws.transformer.xml;

import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;

import com.google.common.base.Function;

public class FromThresholds implements Function<Thresholds, XCriteria> {
    @Override
    public XCriteria apply(Thresholds input) {
	final XMCDACriteria xmcdaCriteria = new XMCDACriteria();
	xmcdaCriteria.setPreferenceThresholds(input.getPreferenceThresholds());
	xmcdaCriteria.setIndifferenceThresholds(input.getIndifferenceThresholds());
	xmcdaCriteria.setVetoThresholds(input.getVetoThresholds());
	return xmcdaCriteria.write(input.getCriteria());
    }

}
