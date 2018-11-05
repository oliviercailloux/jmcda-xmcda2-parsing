package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternatives;

import com.google.common.base.Function;

public class FromAlternatives implements Function<Set<Alternative>, XAlternatives> {
    @Override
    public XAlternatives apply(Set<Alternative> input) {
	final XMCDAAlternatives writer = new XMCDAAlternatives();
	writer.setMarkActiveAlternatives(true);
	return writer.writeAlternatives(input, null);
    }
}