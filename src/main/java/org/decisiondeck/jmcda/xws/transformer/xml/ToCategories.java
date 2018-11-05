package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.NavigableSet;

import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACategories;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategories;

public class ToCategories implements FunctionWithInputCheck<XCategories, NavigableSet<Category>> {
    @Override
    public NavigableSet<Category> apply(XCategories input) throws InvalidInputException {
	return input == null ? null : new XMCDACategories().read(input);
    }

}
