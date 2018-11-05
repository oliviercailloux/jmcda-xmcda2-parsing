package org.decisiondeck.jmcda.xws.transformer;

import java.util.List;

import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;

public interface FunctionWithErrorManagement<F, V> extends FunctionWithInputCheck<F, V> {
    @Override
    public V apply(F input) throws InvalidInputException;

    public List<String> getErrors();
}