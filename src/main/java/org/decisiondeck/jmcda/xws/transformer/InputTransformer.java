package org.decisiondeck.jmcda.xws.transformer;

import static org.decision_deck.jmcda.utils.FunctionUtils.compose;
import static org.decision_deck.jmcda.utils.FunctionUtils.identity;
import static org.decision_deck.utils.ReflectUtils.isAssignableFrom;
import static org.decision_deck.utils.ReflectUtils.toClass;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.decision_deck.jmcda.utils.FunctionUtils;
import org.decision_deck.utils.ReflectUtils;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.exc.InvalidInvocationException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.xws.transformer.xml.ToAllAssignments;
import org.decisiondeck.jmcda.xws.transformer.xml.ToAlternatives;
import org.decisiondeck.jmcda.xws.transformer.xml.ToBoolean;
import org.decisiondeck.jmcda.xws.transformer.xml.ToCategories;
import org.decisiondeck.jmcda.xws.transformer.xml.ToCategoriesAndProfiles;
import org.decisiondeck.jmcda.xws.transformer.xml.ToCriteria;
import org.decisiondeck.jmcda.xws.transformer.xml.ToDecisionMakers;
import org.decisiondeck.jmcda.xws.transformer.xml.ToDouble;
import org.decisiondeck.jmcda.xws.transformer.xml.ToEvaluations;
import org.decisiondeck.jmcda.xws.transformer.xml.ToFuzzyMatrix;
import org.decisiondeck.jmcda.xws.transformer.xml.ToMatrix;
import org.decisiondeck.jmcda.xws.transformer.xml.ToMatrixesByCriteria;
import org.decisiondeck.jmcda.xws.transformer.xml.ToScales;
import org.decisiondeck.jmcda.xws.transformer.xml.ToString;
import org.decisiondeck.jmcda.xws.transformer.xml.ToThresholds;
import org.decisiondeck.jmcda.xws.transformer.xml.ToWeights;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.io.ByteSource;
import com.google.common.io.InputSupplier;

public class InputTransformer {
	public static FunctionWithInputCheck<XMCDADoc, XmlObject> functionDocToTag(String tagName) {
		return new InputTransformerDocToTag(tagName);
	}

	public static FunctionWithInputCheck<XMCDADoc, List<XmlObject>> functionDocToTags(String tagName,
			boolean optional) {
		return new InputTransformerDocToTags(tagName, optional);
	}

	public static FunctionWithInputCheck<File, ByteSource> functionFileToSource() {
		return FunctionUtils.functionWithInputCheck(new InputTransformerFileToSource());
	}

	/**
	 * Returns a function which, given a file name as a String, returns a
	 * {@link File} representing the given file, assuming the file is to be
	 * found in the given input directory. For example, if the given input
	 * directory refers to a directory "dir", the returned function, when given
	 * the string "name", with return a handle to a file named "name" in the
	 * directory "dir". The returned function checks that the file exists before
	 * returning, if it does not, the returned function returns
	 * <code>null</code> or throws an {@link InvalidInputException} depending on
	 * whether the return value is optional.
	 *
	 * @param inputDirectory
	 *            not <code>null</code>.
	 * @param optional
	 *            if <code>true</code>, the function will return
	 *            <code>null</code> when the referenced file does not exist, and
	 *            does not throw {@link InvalidInputException}; if
	 *            <code>false</code>, the function never returns
	 *            <code>null</code> but throws an exception if the file to be
	 *            returned does not exist.
	 * @return not <code>null</code>.
	 */
	static public FunctionWithInputCheck<String, File> functionNameToFile(File inputDirectory, boolean optional) {
		Preconditions.checkNotNull(inputDirectory);
		return new InputTransformerNameToFile(inputDirectory, optional);
	}

	public static FunctionWithInputCheck<ByteSource, XMCDADoc> functionSourceToDoc() {
		return new InputTransformerSourceToDoc();
	}

	private FunctionWithInputCheck<XMCDADoc, ? extends XmlObject> m_docToTag;
	private FunctionWithInputCheck<XMCDADoc, List<XmlObject>> m_docToTags;

	private FunctionWithInputCheck<File, ByteSource> m_fileToSource;

	private FunctionWithInputCheck<String, File> m_nameToFile;

	private FunctionWithInputCheck<String, ByteSource> m_nameToSource;

	private FunctionWithInputCheck<ByteSource, XMCDADoc> m_sourceToDoc;

	private final Transformers<FunctionWithInputCheck<?, ?>> m_transformers = new Transformers<FunctionWithInputCheck<?, ?>>(
			false, true);

	public InputTransformer() {
		m_nameToFile = null;
		m_nameToSource = null;
		m_fileToSource = null;
		m_sourceToDoc = null;
		m_docToTag = null;
		m_docToTags = null;

		m_transformers.add(new ToAlternatives());
		m_transformers.add(new ToCriteria());
		m_transformers.add(new ToDecisionMakers());
		m_transformers.add(new ToThresholds());
		m_transformers.add(new ToScales());
		m_transformers.add(new ToWeights());
		m_transformers.add(new ToEvaluations());
		m_transformers.add(new ToCategories());
		m_transformers.add(new ToCategoriesAndProfiles());
		m_transformers.add(new ToMatrix());
		m_transformers.add(new ToMatrixesByCriteria());
		m_transformers.add(new ToAllAssignments());
		m_transformers.add(new ToFuzzyMatrix());
		m_transformers.add(new ToDouble());
		m_transformers.add(new ToString());
		m_transformers.add(new ToBoolean());
	}

	/**
	 * Adds the given transformer to the set of transformers this object uses.
	 * The given function must have a return type that is not already associated
	 * with a transformer.
	 *
	 * @param function
	 *            not <code>null</code>.
	 */
	public void add(FunctionWithInputCheck<?, ?> function) {
		m_transformers.add(function);
	}

	public <B, C, D> D get(Type targetType,
			Class<? extends FunctionWithInputCheck<Object, Object>> intermediateTransform, String name,
			File inputDirectory, boolean optional) throws InvalidInputException, InvalidInvocationException {
		final Type fakeTarget;
		if (targetType.equals(double.class)) {
			fakeTarget = Double.class;
		} else {
			fakeTarget = targetType;
		}

		final FunctionWithInputCheck<String, ? extends D> transformer = getTransformer(fakeTarget,
				intermediateTransform, inputDirectory, optional);
		return transformer.apply(name);
	}

	public FunctionWithInputCheck<XMCDADoc, ? extends XmlObject> getDocToTag(String tagName) {
		return m_docToTag == null ? functionDocToTag(tagName) : m_docToTag;
	}

	public FunctionWithInputCheck<XMCDADoc, List<XmlObject>> getDocToTags(String tagName, boolean optional) {
		return m_docToTags == null ? functionDocToTags(tagName, optional) : m_docToTags;
	}

	public FunctionWithInputCheck<File, ByteSource> getFileToSource() {
		return m_fileToSource == null ? functionFileToSource() : m_fileToSource;
	}

	/**
	 * <p>
	 * Returns a function which, given a file name as a String, returns an
	 * {@link XMCDADoc} containing the contents of the file, assuming the file
	 * is to be found in the given input directory. For example, if the given
	 * input directory refers to a directory "dir", the returned function, when
	 * given the string "name", with return a document containing the contents
	 * found in the file named "name" in the directory "dir". The returned
	 * function checks that the file exists before returning, if it does not,
	 * the returned function returns <code>null</code> or throws an
	 * {@link InvalidInputException} depending on whether the return value is
	 * optional.
	 * </p>
	 * <p>
	 * Assuming that the file exists, the function retrieves the XMCDA document
	 * and ensures that it validates. The function throws an {@link IOException}
	 * if an exception happens while opening or closing the given reader, or
	 * while parsing the source, and throws an {@link XmlException} if an
	 * exception related to the contents of the source happens while parsing the
	 * source, including if the source document does not validate. Both are
	 * wrapped into an {@link InvalidInputException}.
	 * </p>
	 * <p>
	 * If the document contained in the given source appears, according to its
	 * namespace, to be an XMCDA document not matching the expected version
	 * {@link XMCDAReadUtils#DEFAULT_XMCDA_VERSION}, this method attemps to
	 * proceed as if the source version matched the expected one.
	 * </p>
	 *
	 * @param inputDirectory
	 *            not <code>null</code>.
	 * @param optional
	 *            if <code>true</code>, the function will return
	 *            <code>null</code> when the referenced file does not exist; if
	 *            <code>false</code>, the function never returns
	 *            <code>null</code> but throws an exception if the file to be
	 *            returned does not exist.
	 * @return not <code>null</code>.
	 * @throws InvalidInvocationException
	 *             if the default name to file transformer is to be used, thus
	 *             no alternative transformer is set, and the given input
	 *             directory is <code>null</code>.
	 */
	public FunctionWithInputCheck<String, XMCDADoc> getNameToDoc(File inputDirectory, boolean optional)
			throws InvalidInvocationException {
		return compose(getSourceToDoc(), getNameToSource(inputDirectory, optional));
	}

	/**
	 * Retrieves the alternative transformer from name to file set in this
	 * object, or the default one if none is set.
	 *
	 * @param inputDirectory
	 *            non <code>null</code> if the default transformer is to be
	 *            used.
	 * @param optional
	 *            <code>true</code> if the transformer is allowed to return
	 *            <code>null</code>, used only for the default transformer.
	 * @return not <code>null</code>.
	 * @throws InvalidInvocationException
	 *             if the default transformer is to be used, thus no alternative
	 *             transformer is set, and the given input directory is
	 *             <code>null</code>.
	 */
	public FunctionWithInputCheck<String, File> getNameToFile(File inputDirectory, boolean optional)
			throws InvalidInvocationException {
		if (m_nameToFile == null) {
			if (inputDirectory == null) {
				throw new InvalidInvocationException("Unknown input directory.");
			}
			return functionNameToFile(inputDirectory, optional);
		}
		return m_nameToFile;
	}

	/**
	 * <p>
	 * Retrieves the alternative transformer from name to source set in this
	 * object, or the default one if none is set.
	 * </p>
	 * <p>
	 * The default is to return a function which, given a file name as a String,
	 * returns a source referencing the given file, assuming the file is to be
	 * found in the given input directory. For example, if the given input
	 * directory refers to a directory "dir", the returned function, when given
	 * the string "name", with return a stream supplier reading from a file
	 * named "name" in the directory "dir". The returned function checks that
	 * the file exists before returning, if it does not, the returned function
	 * returns <code>null</code> or throws an {@link InvalidInputException}
	 * depending on whether the return value is optional.
	 * </p>
	 * <p>
	 * if optional is <code>true</code>, the function will return
	 * <code>null</code> when the referenced file does not exist, and does not
	 * throw {@link InvalidInputException}; if <code>false</code>, the function
	 * never returns <code>null</code> but throws an exception if the file to be
	 * returned does not exist.
	 * </p>
	 *
	 * @param inputDirectory
	 *            non <code>null</code> if the default transformer is to be
	 *            used.
	 * @param optional
	 *            <code>true</code> if the transformer is allowed to return
	 *            <code>null</code>, used only for the default transformer.
	 * @return not <code>null</code>.
	 * @throws InvalidInvocationException
	 *             if the default name to file transformer is to be used, thus
	 *             no alternative transformer is set, and the given input
	 *             directory is <code>null</code>.
	 */
	public FunctionWithInputCheck<String, ByteSource> getNameToSource(File inputDirectory, boolean optional)
			throws InvalidInvocationException {
		return m_nameToSource == null
				? FunctionUtils.compose(getFileToSource(), getNameToFile(inputDirectory, optional)) : m_nameToSource;
	}

	/**
	 * <p>
	 * Returns a function which, given a file name as a String, returns an
	 * {@link XmlObject} containing the fragment of the file corresponding to
	 * the given tagname, assuming the file is to be found in the given input
	 * directory. For example, if the given input directory refers to a
	 * directory "dir", and tagname is "alternatives", the returned function,
	 * when given the string "name", with return a xml fragment representing the
	 * tag "alternatives" and its content, found in the file named "name" in the
	 * directory "dir". If the file does not exist, the returned function
	 * returns <code>null</code> or throws an {@link InvalidInputException}
	 * depending on whether the return value is optional.
	 * </p>
	 * <p>
	 * Assuming that the file exists, the function retrieves the XMCDA document
	 * and ensures that it validates. The function throws an {@link IOException}
	 * if an exception happens while opening or closing the given reader, or
	 * while parsing the source, and throws an {@link XmlException} if an
	 * exception related to the contents of the source happens while parsing the
	 * source, including if the source document does not validate. Both are
	 * wrapped into an {@link InvalidInputException}. Then, the function reads
	 * the content of the document. If the given tag is not found or more than
	 * one is found, an {@link InvalidInputException} is thrown. Otherwize, an
	 * xml object representing the tag contents is returned.
	 * </p>
	 * <p>
	 * If the document contained in the given source appears, according to its
	 * namespace, to be an XMCDA document not matching the expected version
	 * {@link XMCDAReadUtils#DEFAULT_XMCDA_VERSION}, this method attemps to
	 * proceed as if the source version matched the expected one.
	 * </p>
	 *
	 * @param inputDirectory
	 *            not <code>null</code>.
	 * @param tagName
	 *            not <code>null</code>.
	 * @param optional
	 *            if <code>true</code>, the function will return
	 *            <code>null</code> when the referenced file does not exist; if
	 *            <code>false</code>, the function never returns
	 *            <code>null</code> but throws an exception if the file to be
	 *            returned does not exist.
	 * @return not <code>null</code>.
	 * @throws InvalidInvocationException
	 *             if the default name to file transformer is to be used, thus
	 *             no alternative transformer is set, and the given input
	 *             directory is <code>null</code>.
	 */
	public FunctionWithInputCheck<String, ? extends XmlObject> getNameToTag(File inputDirectory, String tagName,
			boolean optional) throws InvalidInvocationException {
		Preconditions.checkNotNull(tagName);
		return compose(getDocToTag(tagName), getNameToDoc(inputDirectory, optional));
	}

	/**
	 * <p>
	 * Returns a function which, given a file name as a String, returns alist of
	 * {@link XmlObject}s containing the fragments of the file corresponding to
	 * the given tagname, assuming the file is to be found in the given input
	 * directory. For example, if the given input directory refers to a
	 * directory "dir", and tagname is "alternatives", the returned function,
	 * when given the string "name", with return xml fragments representing the
	 * tags "alternatives" and their content, found in the file named "name" in
	 * the directory "dir". Assuming the given file would contain two tags named
	 * "alternatives", the returned list would contain two xml objects, each one
	 * corresponding to one of the tags, listed in document order.
	 * </p>
	 * <p>
	 * If the file does not exist, the returned function returns
	 * <code>null</code> or throws an {@link InvalidInputException} depending on
	 * whether the return value is optional.
	 * </p>
	 * <p>
	 * Assuming that the file exists, the function retrieves the XMCDA document
	 * and ensures that it validates. The function throws an {@link IOException}
	 * if an exception happens while opening or closing the given reader, or
	 * while parsing the source, and throws an {@link XmlException} if an
	 * exception related to the contents of the source happens while parsing the
	 * source, including if the source document does not validate. Both are
	 * wrapped into an {@link InvalidInputException}. Then, the function reads
	 * the content of the document. If the given tag is not found, an
	 * {@link InvalidInputException} is thrown. Otherwize, xml objects
	 * representing the tags contents are returned, one object per tag matching
	 * the given tag name.
	 * </p>
	 * <p>
	 * If the document contained in the given source appears, according to its
	 * namespace, to be an XMCDA document not matching the expected version
	 * {@link XMCDAReadUtils#DEFAULT_XMCDA_VERSION}, this method attemps to
	 * proceed as if the source version matched the expected one.
	 * </p>
	 *
	 * @param inputDirectory
	 *            not <code>null</code>.
	 * @param tagName
	 *            not <code>null</code>.
	 * @param optional
	 *            if <code>true</code>, the function will return
	 *            <code>null</code> when the referenced file does not exist; if
	 *            <code>false</code>, the function never returns
	 *            <code>null</code> but throws an exception if the file to be
	 *            returned does not exist.
	 * @return not <code>null</code>.
	 * @throws InvalidInvocationException
	 *             if the default name to file transformer is to be used, thus
	 *             no alternative transformer is set, and the given input
	 *             directory is <code>null</code>.
	 */
	public FunctionWithInputCheck<String, List<XmlObject>> getNameToTags(File inputDirectory, String tagName,
			boolean optional) throws InvalidInvocationException {
		return compose(getDocToTags(tagName, optional), getNameToDoc(inputDirectory, optional));
	}

	public FunctionWithInputCheck<ByteSource, XMCDADoc> getSourceToDoc() {
		return m_sourceToDoc == null ? functionSourceToDoc() : m_sourceToDoc;
	}

	/**
	 * Returns a transformer that chains transformation in order to pass from a
	 * String (supposedly containing a file name) to the given target type, and
	 * going through the given intermediate transformer if it is not
	 * <code>null</code>. The returned transformer implements a default strategy
	 * to transform the String to type B, then the intermediate transformer is
	 * instantiated and used to transform the object of type B to an object of
	 * type C, then again the default strategy is used to get a final object of
	 * type D. If some of these types are equal, for example C and D, no
	 * transformation occurs for that step.
	 *
	 * @param <B>
	 *            the input type of the intermediate transformer, if given. If
	 *            it is not String, a string will be transformed to a type B by
	 *            using the default transform strategy.
	 * @param <C>
	 *            the output type of the intermediate transformer, if given.
	 * @param <D>
	 *            the type that the returned function outputs.
	 * @param targetType
	 *            the type that the returned function outputs.
	 * @param intermediateTransform
	 *            may be <code>null</code>.
	 * @param inputDirectory
	 *            may be <code>null</code> if unused.
	 * @param optional
	 *            if <code>true</code>, the returned function will return
	 *            <code>null</code> when the file does not exist instead of
	 *            throwing an exception.
	 * @return not <code>null</code>.
	 * @throws InvalidInvocationException
	 *             if the default name to file transformer is to be used and the
	 *             given input directory is <code>null</code>.
	 */
	public <B, C, D> FunctionWithInputCheck<String, ? extends D> getTransformer(Type targetType,
			Class<? extends FunctionWithInputCheck<B, C>> intermediateTransform, File inputDirectory, boolean optional)
			throws InvalidInvocationException {
		Preconditions.checkNotNull(targetType);
		final boolean toManaged = isManagedType(targetType)
				|| (intermediateTransform != null && TransformersWithInputCheck.getApplyMethod(intermediateTransform)
						.getGenericReturnType().equals(targetType));

		if (toManaged) {
			return getTransformerToManagedType(targetType, intermediateTransform, inputDirectory, optional);
		}
		@SuppressWarnings("unchecked")
		final FunctionWithInputCheck<Object, ?> reader = (FunctionWithInputCheck<Object, ?>) m_transformers
				.getTransformerToLaxist(targetType);
		Preconditions.checkArgument(reader != null, "Undefined transform to given type " + targetType + ".");

		final Type requiredType = m_transformers.getRequired(targetType);
		@SuppressWarnings("unchecked")
		final FunctionWithInputCheck<String, Object> transformerToManagedType = (FunctionWithInputCheck<String, Object>) getTransformerToManagedType(
				requiredType, intermediateTransform, inputDirectory, optional);
		final FunctionWithInputCheck<String, ?> composed = compose(reader, transformerToManagedType);
		@SuppressWarnings("unchecked")
		final FunctionWithInputCheck<String, ? extends D> typed = (FunctionWithInputCheck<String, ? extends D>) composed;
		return typed;
	}

	/**
	 * <p>
	 * Retrieves a function that transforms from a String supposedly containing
	 * a file name to the given toType using a default strategy, assuming such a
	 * default transform is specified. For example, if the to type is File, the
	 * returned function, when given a String, returns a File referencing the
	 * file named according to the String given to the function, and found in
	 * the input directory given as parameter to this method, according to the
	 * description of {@link #functionNameToFile(File, boolean)}.
	 * </p>
	 * <p>
	 * If toType is String, the identity function is returned.
	 * </p>
	 * <p>
	 * The toType must be chosen in the following list: String, File,
	 * {@link InputSupplier} (of InputStream extended streams), {@link XMCDADoc}
	 * , {@link XmlObject} or list of xml objects. TODO doc not up to date
	 * because InputSupplier changed to ByteSource.
	 * </p>
	 *
	 * @param <V>
	 *            the codomain of the function: the type of value it returns,
	 *            which is also the to type.
	 *
	 * @param targetType
	 *            not <code>null</code>.
	 * @param inputDirectory
	 *            may be <code>null</code>.
	 * @param optional
	 *            <code>true</code> to allow the function to return a
	 *            <code>null</code> value if the file does not exist.
	 * @return not <code>null</code>.
	 * @throws InvalidInvocationException
	 *             if the default name to file transformer is to be used, thus
	 *             no alternative transformer is set and the toType is not
	 *             String, and the given input directory is <code>null</code>.
	 */
	public <V> FunctionWithInputCheck<String, V> getTransformer(Type targetType, File inputDirectory, boolean optional)
			throws InvalidInvocationException {
		Preconditions.checkNotNull(targetType);
		final FunctionWithInputCheck<String, V> transformerFrom = getTransformerFromExact(String.class, targetType,
				inputDirectory, optional);
		return transformerFrom;
	}

	/**
	 * <p>
	 * Retrieves a function that transforms from the given fromType to the given
	 * toType using a default strategy, assuming such a default transform is
	 * specified. For example, if fromType is String and the to type is File,
	 * the returned function, when given a String, returns a File referencing
	 * the file named according to the String given to the function, and found
	 * in the input directory given as parameter to this method, according to
	 * the description of {@link #functionNameToFile(File, boolean)}.
	 * </p>
	 * <p>
	 * If fromType equals toType, the identity function is returned.
	 * </p>
	 * <p>
	 * The fromType and toType must be chosen in the following list: String,
	 * File, {@link InputSupplier} (of InputStream extended streams),
	 * {@link XMCDADoc}, {@link XmlObject} or subclasses, or parameterized list
	 * (or superclass) of xml objects or subclasses. The to type must come later
	 * down the list compared to the from type. TODO doc not up to date because
	 * InputSupplier changed to ByteSource.
	 * </p>
	 * <p>
	 * Note that the optional parameter is only used to transform from a String
	 * to a File. If e.g. the from type is File and the to type is
	 * InputSupplier, the returned function, when given a <code>null</code>
	 * argument, returns <code>null</code> irrespective of the optional
	 * parameter value, whereas it should throw an exception when optional is
	 * <code>false</code>. This works for as long as caution is exercised not to
	 * give the returned function a <code>null</code> argument when optional is
	 * <code>false</code>. TODO doc not up to date because InputSupplier changed
	 * to ByteSource.
	 * </p>
	 *
	 * @param <F>
	 *            the domain of the function: the type of value it expects,
	 *            which is also the from type.
	 * @param <V>
	 *            the codomain of the function: the type of value it returns,
	 *            which is also the to type.
	 *
	 * @param fromType
	 *            not <code>null</code>.
	 * @param toType
	 *            not <code>null</code>.
	 * @param inputDirectory
	 *            may be <code>null</code>.
	 * @param optional
	 *            <code>true</code> to allow the function to return a
	 *            <code>null</code> value if the file does not exist.
	 * @param requireExactReturnType
	 *            if <code>true</code>, the returned function has a codomain of
	 *            exactly V instead of something extending V, the drawback being
	 *            that the toType must also be exactly V, not some subtype of an
	 *            allowed entry. For example, if the argument is
	 *            <code>false</code>, a Collection is allowed to play the role
	 *            of a List as a valid toType. Note that the from type is not
	 *            checked exactly, irrespective of the value of this parameter.
	 * @return not <code>null</code>.
	 * @throws InvalidInvocationException
	 *             if the default name to file transformer is to be used, thus
	 *             no alternative transformer is set and the fromType is String
	 *             and the toType is not String, and the given input directory
	 *             is <code>null</code>.
	 */
	public <F, V> FunctionWithInputCheck<F, ? extends V> getTransformerFrom(Class<? extends F> fromType, Type toType,
			File inputDirectory, boolean optional, boolean requireExactReturnType) throws InvalidInvocationException {
		Preconditions.checkNotNull(fromType);
		Preconditions.checkNotNull(toType);

		if (isAssignableFrom(toType, fromType, requireExactReturnType)) {
			final FunctionWithInputCheck<F, ? extends V> identity = (FunctionWithInputCheck<F, ? extends V>) identity();
			return identity;
		}
		if (String.class.isAssignableFrom(fromType)) {
			if (isAssignableFrom(toType, File.class, requireExactReturnType)) {
				final FunctionWithInputCheck<String, File> function = getNameToFile(inputDirectory, optional);
				return ((FunctionWithInputCheck<F, ? extends V>) function);
			}
			if (isAssignableFrom(toType, ByteSource.class, requireExactReturnType)) {
				final FunctionWithInputCheck<String, ByteSource> function = getNameToSource(inputDirectory, optional);
				return (FunctionWithInputCheck<F, ? extends V>) function;
			}
			if (isAssignableFrom(toType, XMCDADoc.class, requireExactReturnType)) {
				final FunctionWithInputCheck<String, XMCDADoc> function = getNameToDoc(inputDirectory, optional);
				return (FunctionWithInputCheck<F, ? extends V>) function;
			}

			if (isAssignableFrom(XmlObject.class, toType, false)) {
				final Class<? extends XmlObject> toType2 = (Class<? extends XmlObject>) toType;
				final String resTagName = XMCDAReadUtils.getTagName(toType2);
				final FunctionWithInputCheck<String, ? extends XmlObject> function = getNameToTag(inputDirectory,
						resTagName, optional);
				/**
				 * Note here we do not know if the function really returns a V
				 * type. We assume it for now, maybe would be better to check.
				 */
				return (FunctionWithInputCheck<F, ? extends V>) function;
			}

			if (isAssignableFrom(toType, List.class, requireExactReturnType)) {
				final Type parameterType = ReflectUtils.getParameterType(toType);
				final Class<? extends XmlObject> parameterClass = (Class<? extends XmlObject>) toClass(parameterType);
				final String tagName = XMCDAReadUtils.getTagName(parameterClass);
				final FunctionWithInputCheck<String, List<XmlObject>> function = getNameToTags(inputDirectory, tagName,
						optional);
				return (FunctionWithInputCheck<F, ? extends V>) function;
			}

			throw new IllegalArgumentException("Illegal to type: " + toType + " with from type " + fromType
					+ " (exact? " + requireExactReturnType + ").");
		}

		if (File.class.isAssignableFrom(fromType)) {
			if (isAssignableFrom(toType, ByteSource.class, requireExactReturnType)) {
				final FunctionWithInputCheck<File, ByteSource> function = getFileToSource();
				return (FunctionWithInputCheck<F, ? extends V>) function;
			}
			if (isAssignableFrom(toType, XMCDADoc.class, requireExactReturnType)) {
				final FunctionWithInputCheck<File, XMCDADoc> function = compose(getSourceToDoc(), getFileToSource());
				return (FunctionWithInputCheck<F, ? extends V>) function;
			}

			if (isAssignableFrom(XmlObject.class, toType, false)) {
				final FunctionWithInputCheck<File, XMCDADoc> function1 = compose(getSourceToDoc(), getFileToSource());
				final Class<? extends XmlObject> toType2 = (Class<? extends XmlObject>) toType;
				final String resTagName = XMCDAReadUtils.getTagName(toType2);
				final FunctionWithInputCheck<File, ? extends XmlObject> function = compose(getDocToTag(resTagName),
						function1);
				return (FunctionWithInputCheck<F, V>) function;
			}

			if (isAssignableFrom(toType, List.class, requireExactReturnType)) {
				final Type parameterType = ReflectUtils.getParameterType(toType);
				final Class<? extends XmlObject> parameterClass = (Class<? extends XmlObject>) toClass(parameterType);
				final String tagName = XMCDAReadUtils.getTagName(parameterClass);
				final FunctionWithInputCheck<File, XMCDADoc> function1 = compose(getSourceToDoc(), getFileToSource());
				final FunctionWithInputCheck<File, List<XmlObject>> function = compose(getDocToTags(tagName, optional),
						function1);
				return (FunctionWithInputCheck<F, ? extends V>) function;
			}

			throw new IllegalArgumentException("Illegal to type: " + toType + " with from type " + fromType + ".");
		}

		if (ByteSource.class.isAssignableFrom(fromType)) {
			if (isAssignableFrom(toType, XMCDADoc.class, requireExactReturnType)) {
				final FunctionWithInputCheck<ByteSource, XMCDADoc> function = getSourceToDoc();
				return (FunctionWithInputCheck<F, ? extends V>) function;
			}

			if (isAssignableFrom(XmlObject.class, toType, false)) {
				final FunctionWithInputCheck<ByteSource, XMCDADoc> function1 = getSourceToDoc();
				final Class<? extends XmlObject> toType2 = (Class<? extends XmlObject>) toType;
				final String resTagName = XMCDAReadUtils.getTagName(toType2);
				final FunctionWithInputCheck<ByteSource, ? extends XmlObject> function = compose(
						getDocToTag(resTagName), function1);
				return (FunctionWithInputCheck<F, V>) function;
			}

			if (isAssignableFrom(toType, List.class, requireExactReturnType)) {
				final Type parameterType = ReflectUtils.getParameterType(toType);
				final Class<? extends XmlObject> parameterClass = (Class<? extends XmlObject>) toClass(parameterType);
				final String tagName = XMCDAReadUtils.getTagName(parameterClass);
				final FunctionWithInputCheck<ByteSource, XMCDADoc> function1 = getSourceToDoc();
				final FunctionWithInputCheck<ByteSource, List<XmlObject>> function = compose(
						getDocToTags(tagName, optional), function1);
				return (FunctionWithInputCheck<F, ? extends V>) function;
			}

			throw new IllegalArgumentException("Illegal to type: " + toType + " with from type " + fromType + ".");
		}

		throw new IllegalArgumentException("Illegal from type: " + fromType + ".");
	}

	public void setDocToTag(FunctionWithInputCheck<XMCDADoc, ? extends XmlObject> docToTag) {
		m_docToTag = docToTag;
	}

	public void setDocToTags(FunctionWithInputCheck<XMCDADoc, List<XmlObject>> docToTags) {
		m_docToTags = docToTags;
	}

	public void setFileToSource(FunctionWithInputCheck<File, ByteSource> fileToSource) {
		m_fileToSource = fileToSource;
	}

	public void setNameToFile(FunctionWithInputCheck<String, File> nameToFile) {
		m_nameToFile = nameToFile;
	}

	public void setNameToSource(FunctionWithInputCheck<String, ByteSource> nameToSource) {
		m_nameToSource = nameToSource;
	}

	public void setSourceToDoc(FunctionWithInputCheck<ByteSource, XMCDADoc> sourceToDoc) {
		m_sourceToDoc = sourceToDoc;
	}

	private <F, V> FunctionWithInputCheck<F, V> getTransformerFromExact(Class<? extends F> fromType, Type toType,
			File inputDirectory, boolean optional) throws InvalidInvocationException {
		@SuppressWarnings("unchecked")
		final FunctionWithInputCheck<F, V> transformer = (FunctionWithInputCheck<F, V>) getTransformerFrom(fromType,
				toType, inputDirectory, optional, true);
		return transformer;
	}

	private <B, D, C> FunctionWithInputCheck<String, ? extends D> getTransformerToManagedType(final Type targetType,
			Class<? extends FunctionWithInputCheck<B, C>> intermediateTransform, File inputDirectory, boolean optional)
			throws InvalidInvocationException {
		Preconditions.checkNotNull(targetType);

		if (intermediateTransform == null) {
			return getTransformerFrom(String.class, targetType, inputDirectory, optional, false);
		}
		// @SuppressWarnings("unchecked")
		// final Class<? extends FunctionWithInputCheck<B, C>>
		// intermediateClass2 = (Class<? extends
		// FunctionWithInputCheck<B, C>>) intermediateClass;

		final Method applyMethod = TransformersWithInputCheck.getApplyMethod(intermediateTransform);
		final Type[] parameterTypes = applyMethod.getGenericParameterTypes();
		// this is class of type B
		final Type parameterType = Iterators.getOnlyElement(Iterators.forArray(parameterTypes));
		@SuppressWarnings("unchecked")
		final Class<C> returnType = (Class<C>) applyMethod.getReturnType();

		/** From String to parameterType. */
		final FunctionWithInputCheck<String, B> transformer1 = getTransformer(parameterType, inputDirectory, optional);

		/** From parameterType to returnType. */
		final FunctionWithInputCheck<B, C> transformer2;
		try {
			// @SuppressWarnings("unchecked")
			// final FunctionWithInputCheck<B, C> transformerChecked =
			// (FunctionWithInputCheck<B, C>) intermediateClass
			// .newInstance();
			// transformer2 = transformerChecked;
			transformer2 = intermediateTransform.newInstance();
		} catch (InstantiationException exc) {
			throw new IllegalStateException("Exception while attempting to transform input " + ".", exc);
		} catch (IllegalAccessException exc) {
			throw new IllegalStateException("Exception while attempting to transform input " + ".", exc);
		}

		/** From returnType to targetType. */
		final FunctionWithInputCheck<C, ? extends D> transformer3 = getTransformerFrom(returnType, targetType,
				inputDirectory, optional, false);

		/** From String to returnType. */
		final FunctionWithInputCheck<String, C> transformer1Then2 = compose(transformer2, transformer1);
		// final FunctionWithInputCheck<String, C> transformer1Then2 = new
		// TransformerCombine(transformer1, applyMethod,
		// transformer2);

		/** From String to targetType. */
		final FunctionWithInputCheck<String, D> transformer1To3 = compose(transformer3, transformer1Then2);
		return transformer1To3;
	}

	private boolean isManagedType(Type toType) {
		if (isAssignableFrom(toType, File.class, false)) {
			return true;
		}
		if (isAssignableFrom(toType, ByteSource.class, false)) {
			return true;
		}
		if (isAssignableFrom(toType, XMCDADoc.class, false)) {
			return true;
		}

		if (isAssignableFrom(XmlObject.class, toType, false)) {
			return true;
		}

		if (isAssignableFrom(toType, List.class, false)) {
			final Type parameterType = ReflectUtils.getParameterType(toType);
			if (isAssignableFrom(XmlObject.class, parameterType, false)) {
				return true;
			}
		}

		return false;
	}
}
