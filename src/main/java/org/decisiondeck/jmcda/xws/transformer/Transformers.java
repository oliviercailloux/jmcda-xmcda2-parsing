package org.decisiondeck.jmcda.xws.transformer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.decision_deck.utils.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Contains a set of functions, called transformers, accessible by return type. One transformer maximum for a given
 * return type.
 * 
 * @author Olivier Cailloux
 * @param <F>
 *            the function type.
 * 
 */
public class Transformers<F> {

    public Transformers(boolean uniqueInputType, boolean uniqueReturnType) {
	m_uniqueInputType = uniqueInputType;
	m_uniqueReturnType = uniqueReturnType;
    }

    /**
     * Creates a transformers object holding the given functions. No reference is kept to the given set.
     * 
     * @param functions
     *            not <code>null</code>.
     */
    public Transformers(Set<F> functions) {
	this(false, false);
	checkNotNull(functions);
	addAll(functions);
    }

    /**
     * Adds all the given functions to this set of functions.
     * 
     * @param functions
     *            not <code>null</code>.
     */
    public void addAll(Set<F> functions) {
	for (F function : functions) {
	    add(function);
	}
    }

    /**
     * Adds the given function to this set of functions.
     * 
     * @param function
     *            not <code>null</code>.
     */
    public void add(F function) {
	Preconditions.checkNotNull(function);

	@SuppressWarnings("unchecked")
	final Class<? extends F> fctClass = (Class<? extends F>) function.getClass();
	final Method applyMethod = getApplyMethod(fctClass);
	final Type returnType = applyMethod.getGenericReturnType();
	final Type parameterType = applyMethod.getGenericParameterTypes()[0];
	if (m_uniqueReturnType) {
	    checkArgument(!m_functions.containsColumn(returnType));
	}
	if (m_uniqueInputType) {
	    checkArgument(!m_functions.containsRow(parameterType));
	}
	m_functions.put(parameterType, returnType, function);
    }

    public Transformers() {
	this(false, false);
    }

    /**
     * Tests whether the given return type has an associated function.
     * 
     * @param returnType
     *            not <code>null</code>.
     * @return <code>true</code> iff at least one function in this object has the given return type.
     */
    public boolean containsReturnType(Type returnType) {
	checkNotNull(returnType);
	return m_functions.containsColumn(returnType);
    }

    /**
     * Tests whether the given from type has an associated function.
     * 
     * @param fromType
     *            not <code>null</code>.
     * @return <code>true</code> iff at least one function in this object has the given type as input type.
     */
    public boolean containsFromTypeExact(Type fromType) {
	checkNotNull(fromType);
	return m_functions.containsRow(fromType);
    }

    private final HashBasedTable<Type, Type, F> m_functions = HashBasedTable.create();
    private final boolean m_uniqueInputType;
    private final boolean m_uniqueReturnType;

    /**
     * Retrieves the apply method of the given class. Only one apply method must be declared, or one with object types
     * and one with non-object types, the later one being chosen in this case. The returned method is guaranteed to have
     * only one parameter.
     * 
     * @param function
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    static public Method getApplyMethodGeneric(Class<?> function) {
	final Method[] declaredMethods = function.getDeclaredMethods();
	Set<Method> applyMethods = Sets.newHashSet();
	for (Method method : declaredMethods) {
	    if (!"apply".equals(method.getName())) {
		continue;
	    }
	    final Class<?>[] parameterTypes = method.getParameterTypes();
	    if (parameterTypes.length != 1) {
		continue;
	    }
	    applyMethods.add(method);
	}

	if (applyMethods.size() >= 3) {
	    throw new IllegalArgumentException("Given function " + function + " has " + applyMethods.size()
		    + " apply methods, that's a bit too much.");
	}
	if (applyMethods.isEmpty()) {
	    throw new IllegalArgumentException("Given function " + function + " does not seem to implement apply.");
	}

	/**
	 * For an unknown reason, it seems that the function has two apply methods defined, one being with Object
	 * parameter and return type.
	 */
	if (applyMethods.size() == 2) {
	    final Iterator<Method> iterator = applyMethods.iterator();
	    final Method first = iterator.next();
	    final Method second = iterator.next();
	    final boolean firstObj = first.getReturnType().equals(Object.class);
	    final boolean secondObj = second.getReturnType().equals(Object.class);
	    if (firstObj && secondObj) {
		throw new IllegalArgumentException("Given function " + function + " has " + applyMethods.size()
			+ " apply methods, that's a bit too much.");
	    }
	    if (!firstObj && !secondObj) {
		throw new IllegalArgumentException("Given function " + function + " has " + applyMethods.size()
			+ " apply methods, that's a bit too much.");
	    }
	    if (firstObj) {
		applyMethods.remove(first);
	    } else {
		applyMethods.remove(second);
	    }
	}

	return Iterables.getOnlyElement(applyMethods);
    }

    /**
     * Retrieves the apply method of the given class implementing a function. Only one apply method must be declared, or
     * one with object types and one with non-object types, the later one being chosen in this case. The returned method
     * is guaranteed to have only one parameter.
     * 
     * @param function
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public Method getApplyMethod(Class<? extends F> function) {
	return getApplyMethodGeneric(function);
    }

    /**
     * Retrieves the type of the required parameter to use the function associated to the given return type, or the
     * function compatible with the given return type. The return type must have a unique associated function.
     * 
     * @param returnType
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * @see #containsReturnType(Type)
     */
    public Type getRequired(Type returnType) {
	checkNotNull(returnType);
	final Type compatibleReturnType = getCompatibleReturnType(returnType);
	checkArgument(containsReturnType(compatibleReturnType), "Undefined transformer to return " + returnType + ".");
	final Map<Type, F> byReturnType = m_functions.column(compatibleReturnType);
	checkArgument(byReturnType.size() == 1);
	final Type inputType = Iterables.getOnlyElement(byReturnType.keySet());
	return inputType;
    }

    /**
     * Retrieves the transformer function associated with the given input type. The input type must be associated with a
     * unique function.
     * 
     * @param fromType
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public F getTransformerFromExact(Type fromType) {
	checkNotNull(fromType);
	checkArgument(containsFromTypeExact(fromType));
	final Map<Type, F> byFromType = m_functions.row(fromType);
	checkArgument(byFromType.size() == 1);
	final F fct = Iterables.getOnlyElement(byFromType.values());
	return fct;
    }

    /**
     * Retrieves the transformer function associated with the given input type, or that is able to transform the given
     * input type. There must be a unique function satisfying that definition, if inexact. For example, if a transformer
     * function is defined from List, and the given from type is LinkedList, that function is returned.
     * 
     * @param fromType
     *            not <code>null</code>.
     * @return <code>null</code> iff not found.
     */
    public F getTransformerFromLaxist(Type fromType) {
	checkNotNull(fromType);
	if (containsFromTypeExact(fromType)) {
	    return getTransformerFromExact(fromType);
	}
	F suitable = null;
	s_logger.debug("Searching for inexact transformer from {}.", fromType);
	for (Type inputType : m_functions.rowKeySet()) {
	    if (ReflectUtils.isAssignableFrom(inputType, fromType, false)) {
		s_logger.debug("Found transformer with input type {}, compatible with searched {}.", inputType,
			fromType);
		if (suitable != null) {
		    throw new IllegalArgumentException("More than one function compatible with requested " + fromType
			    + ".");
		}
		final Map<Type, F> row = m_functions.row(inputType);
		if (row.size() > 1) {
		    throw new IllegalArgumentException("More than one function for type " + inputType
			    + " compatible with requested " + fromType + ".");
		}
		suitable = Iterables.getOnlyElement(row.values());
	    }
	}
	return suitable;
    }

    /**
     * Retrieves the transformer function associated with the given return type. The return type must be contained in
     * this object.
     * 
     * @param returnType
     *            not <code>null</code>, in this object.
     * @return not <code>null</code>.
     */
    public F getTransformerTo(Type returnType) {
	checkNotNull(returnType);
	checkArgument(containsReturnType(returnType));
	final Map<Type, F> byReturnType = m_functions.column(returnType);
	checkArgument(byReturnType.size() == 1);
	final F fct = Iterables.getOnlyElement(byReturnType.values());
	return fct;
    }

    private static final Logger s_logger = LoggerFactory.getLogger(Transformers.class);

    /**
     * Retrieves the transformer function that returns the given type, or if no exact transformer is found, the
     * transformer that is able to return the given type. There must be at most one exact function. If inexact, there
     * must be at most one compatible function.
     * 
     * @param returnType
     *            not <code>null</code>.
     * @return <code>null</code> if not found.
     */
    public F getTransformerToLaxist(Type returnType) {
	Type compatibleOutputType = getCompatibleReturnType(returnType);
	if (compatibleOutputType == null) {
	    return null;
	}
	return getTransformerTo(compatibleOutputType);
    }

    private Type getCompatibleReturnType(Type returnType) {
	checkNotNull(returnType);
	Type compatibleOutputType = null;
	if (containsReturnType(returnType)) {
	    compatibleOutputType = returnType;
	} else {
	    s_logger.debug("Searching for inexact transformer to {}.", returnType);

	    for (Type outputType : m_functions.columnKeySet()) {
		if (ReflectUtils.isAssignableFrom(returnType, outputType, false)) {
		    s_logger.debug("Found transformer with return type {}, compatible with searched {}.", outputType,
			    returnType);
		    if (compatibleOutputType != null) {
			throw new IllegalArgumentException("More than one function compatible with requested "
				+ returnType + ".");
		    }
		    final Map<Type, F> column = m_functions.column(outputType);
		    if (column.size() > 1) {
			throw new IllegalArgumentException("More than one function for type " + outputType
				+ " compatible with requested " + returnType + ".");
		    }
		    compatibleOutputType = outputType;
		}
	    }
	}
	return compatibleOutputType;
    }
}
