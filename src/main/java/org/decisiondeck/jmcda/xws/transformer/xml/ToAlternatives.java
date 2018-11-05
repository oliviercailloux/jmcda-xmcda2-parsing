package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Collection;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternatives;

public class ToAlternatives implements FunctionWithInputCheck<Collection<XAlternatives>, Set<Alternative>> {
    @Override
    public Set<Alternative> apply(Collection<XAlternatives> input) throws InvalidInputException {
	return input == null ? null : new XMCDAAlternatives().readAll(input);
    }

}
