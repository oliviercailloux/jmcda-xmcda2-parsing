package org.decisiondeck.jmcda.xws.transformer.xml;

import org.decisiondeck.jmcda.persist.xmcda2.XMCDAVarious;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodMessages;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

public class FromString implements Function<String, XMethodMessages> {
    @Override
    public XMethodMessages apply(String input) {
	return new XMCDAVarious().writeMessages(ImmutableSet.of(input));
    }
}