package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Set;

import org.decision_deck.jmcda.structure.Criterion;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;

import com.google.common.base.Function;

public class FromCriteria implements Function<Set<Criterion>, XCriteria> {
    @Override
    public XCriteria apply(Set<Criterion> input) {
	final XMCDACriteria writer = new XMCDACriteria();
	writer.setMarkActive(true);
	return writer.write(input);
    }
}