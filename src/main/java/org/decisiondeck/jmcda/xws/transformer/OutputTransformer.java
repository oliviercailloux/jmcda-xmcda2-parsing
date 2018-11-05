package org.decisiondeck.jmcda.xws.transformer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Type;

import org.apache.xmlbeans.XmlObject;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAWriteUtils;
import org.decisiondeck.jmcda.xws.transformer.xml.FromAffectations;
import org.decisiondeck.jmcda.xws.transformer.xml.FromAlternatives;
import org.decisiondeck.jmcda.xws.transformer.xml.FromCoalitionsByDecisionMaker;
import org.decisiondeck.jmcda.xws.transformer.xml.FromCriteria;
import org.decisiondeck.jmcda.xws.transformer.xml.FromEvaluations;
import org.decisiondeck.jmcda.xws.transformer.xml.FromExceptions;
import org.decisiondeck.jmcda.xws.transformer.xml.FromMatrix;
import org.decisiondeck.jmcda.xws.transformer.xml.FromMatrixesByCriteria;
import org.decisiondeck.jmcda.xws.transformer.xml.FromScores;
import org.decisiondeck.jmcda.xws.transformer.xml.FromString;
import org.decisiondeck.jmcda.xws.transformer.xml.FromThresholds;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

public class OutputTransformer {

    private final Transformers<Function<?, ?>> m_transformers = new Transformers<Function<?, ?>>(true, false);
    private boolean m_validate;

    public OutputTransformer() {
	m_transformers.add(new FromExceptions());
	m_transformers.add(new FromMatrix());
	m_transformers.add(new FromMatrixesByCriteria());
	m_transformers.add(new FromScores());
	m_transformers.add(new FromEvaluations());
	m_transformers.add(new FromAffectations());
	m_transformers.add(new FromAlternatives());
	m_transformers.add(new FromThresholds());
	m_transformers.add(new FromCriteria());
	m_transformers.add(new FromString());
	m_transformers.add(new FromCoalitionsByDecisionMaker());
	m_validate = true;
    }

    /**
     * 
     * Retrieves the given object, transformed to the given target type, if possible. Otherwise, an exception is thrown.
     * 
     * @param <T>
     *            the target type.
     * @param object
     *            not <code>null</code>.
     * @param targetType
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public <T> T getAs(Object object, Class<T> targetType) {
	checkNotNull(object);
	checkNotNull(targetType);

	if (targetType.isAssignableFrom(object.getClass())) {
	    @SuppressWarnings("unchecked")
	    final T objectTransformed = (T) object;
	    return objectTransformed;
	}
	final Object transformed = autoTransform(object);
	if (targetType.isAssignableFrom(transformed.getClass())) {
	    @SuppressWarnings("unchecked")
	    final T objectTransformed = (T) transformed;
	    return objectTransformed;
	}
	if (targetType.equals(XMCDADoc.class)) {
	    final XMCDADoc doc = transformToDocument(transformed);
	    @SuppressWarnings("unchecked")
	    final T doc2 = (T) doc;
	    return doc2;
	}

	throw new IllegalArgumentException("Unexpected target type: " + targetType + " for object " + object
		+ " of type " + object.getClass() + ".");
    }

    /**
     * Writes the given object in an XMCDA Document, if a transform is known for that object type. The given object is
     * returned if it is a document, or transformed automatically to a document if a transform is known for this object.
     * 
     * @param source
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XMCDADoc getAsDoc(Object source) {
	Preconditions.checkNotNull(source);
	final Object transformed = autoTransform(source);
	return transformToDocument(transformed);
    }

    private XMCDADoc transformToDocument(Object source) {
	if (source instanceof XmlObject) {
	    final XmlObject xmlSource = (XmlObject) source;
	    return XMCDAWriteUtils.getDoc(xmlSource, m_validate);
	}
	if (source instanceof XMCDADoc) {
	    final XMCDADoc doc = (XMCDADoc) source;
	    if (m_validate) {
		if (!doc.validate()) {
		    throw new IllegalArgumentException("Given document does not validate: " + source + ".");
		}
	    }
	    return doc;
	}

	throw new IllegalArgumentException("Don't know how to transform from " + source + " to document.");
    }

    /**
     * Transforms the given object using a transformer in this object, if one exists. Otherwise, returns the object
     * itself.
     * 
     * @param <B>
     *            the type of the object to transform.
     * @param objectFrom
     *            not <code>null</code>.
     * @return not <code>null</code>, because transform functions are supposed not to return <code>null</code> values.
     */
    public <B> Object autoTransform(B objectFrom) {
	return autoTransform(objectFrom, objectFrom.getClass());
    }

    public boolean validates() {
	return m_validate;
    }

    /**
     * @param validate
     *            if <code>true</code>, this object will assert that the XMCDA documents it returns are valid. This is
     *            the default, and this is recommanded.
     */
    public void setValidate(boolean validate) {
	m_validate = validate;
    }

    public XMCDADoc getAsDoc(Object source, Type sourceType) {
	Preconditions.checkNotNull(source);
	final Object transformed = autoTransform(source, sourceType);
	return transformToDocument(transformed);
    }

    /**
     * Transforms the given object using a transformer in this object, if one exists. Otherwise, returns the object
     * itself.
     * 
     * @param <B>
     *            the type of the object to transform.
     * @param objectFrom
     *            not <code>null</code>.
     * @param objectType
     *            not <code>null</code>.
     * @return not <code>null</code>, because transform functions are supposed not to return <code>null</code> values.
     */
    public <B> Object autoTransform(B objectFrom, Type objectType) {
	Preconditions.checkNotNull(objectFrom);
	Preconditions.checkNotNull(objectType);

	@SuppressWarnings("unchecked")
	final Function<B, ?> transformer = (Function<B, ?>) m_transformers.getTransformerFromLaxist(objectType);
	if (transformer == null) {
	    return objectFrom;
	}
	final Object transformed = transformer.apply(objectFrom);
	Preconditions.checkState(transformed != null, "Transform function from " + objectType + " returned null for "
		+ objectFrom + ".");
	return transformed;
    }
}
