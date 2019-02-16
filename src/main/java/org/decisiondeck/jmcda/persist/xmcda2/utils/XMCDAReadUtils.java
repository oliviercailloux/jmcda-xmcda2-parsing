package org.decisiondeck.jmcda.persist.xmcda2.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlTokenSource;
import org.decision_deck.utils.persist.XmlReadUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

/**
 * Helper class containing methods useful for parsing various XMCDA fragments.
 *
 * @author Olivier Cailloux
 *
 */
public class XMCDAReadUtils extends XMCDAErrorsManagerForwarder {

	static public String DEFAULT_XMCDA_VERSION = "2.1.0";
	public static final String SAMPLES_PACKAGE = "/org/decision_deck/xmcda2/samples/";
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(XMCDAReadUtils.class);

	private static final String XMCDA_NAMESPACE_PREFIX = "http://www.decision-deck.org/2009/XMCDA-";

	/**
	 * <p>
	 * Blindly transforms an XMCDA document corresponding to any version to a new
	 * XMCDA document corresponding to a different version. This method only changes
	 * the namespace associated with the document and performs no validation
	 * whatsoever, except that the source document must have a namespace which has
	 * the appropriate prefix for an XMCDA document. The resulting document is
	 * therefore not necessarily a valid XMCDA document.
	 * </p>
	 * <p>
	 * This method may typically be useful in two situations: transforming an old
	 * version of an XMCDA document into the version supported by this library (see
	 * {@link #DEFAULT_XMCDA_VERSION}) in order to be able to read data using the
	 * reader classes this library proposes; or transforming a version of an XMCDA
	 * document obtained by writer classes in this library into an older version
	 * possibly expected by some user.
	 * </p>
	 *
	 * @param source  not <code>null</code>, must contain an XMCDA document.
	 * @param version not <code>null</code>, e.g. "2.0.0" or
	 *                {@link #DEFAULT_XMCDA_VERSION}.
	 * @return not <code>null</code>.
	 * @throws IOException  if an IO error occurs.
	 * @throws XmlException if a parse error occurs.
	 */
	static public ByteSource getAsVersion(ByteSource source, String version) throws IOException, XmlException {
		checkNotNull(source);
		checkNotNull(version);

		final String xsltBase = Resources.toString(XMCDAReadUtils.class.getResource("Change namespace.xslt"),
				Charsets.UTF_8);

		final String sourceNamespace = XmlReadUtils.getNamespace(source);
		if (sourceNamespace == null || !sourceNamespace.startsWith(XMCDA_NAMESPACE_PREFIX)) {
			throw new XmlException("Given source namespace: '" + sourceNamespace + "' should start with "
					+ XMCDA_NAMESPACE_PREFIX + ".");
		}

		// final DOMImplementation domImpl = db.getDOMImplementation();
		// Document document1 =
		// domImpl.createDocument("http://www.decision-deck.org/2009/XMCDA-2.1.0",
		// "xmcda:XMCDA",
		// null);
		// final NodeList childNodes = doc.getDocumentElement().getChildNodes();
		// for (int i = 0; i < childNodes.getLength(); ++i) {
		// final Node node = childNodes.item(0);
		// final Node imported = document1.adoptNode(node);
		// document1.getDocumentElement().appendChild(imported);
		// }
		// DOMImplementationLS ls = (DOMImplementationLS) domImpl;
		// LSSerializer lss = ls.createLSSerializer();
		// LSOutput lso = ls.createLSOutput();
		// lso.setByteStream(System.out);
		// lss.write(document1, lso);

		final String xslt = xsltBase.replace("FROM_NAMESPACE", sourceNamespace).replace("TO_NAMESPACE",
				XMCDA_NAMESPACE_PREFIX + version);
		Transformer tr;
		final ByteArrayOutputStream writer = new ByteArrayOutputStream();
		try (InputStream input = source.openBufferedStream()) {
			tr = TransformerFactory.newInstance().newTransformer(new StreamSource(new StringReader(xslt)));
			tr.transform(new StreamSource(input), new StreamResult(writer));
		} catch (TransformerException exc) {
			throw new XmlException(exc);
		}

		return ByteSource.wrap(writer.toByteArray());
	}

	static public String getTagName(Class<? extends XmlObject> targetType) {
		String resTagName;
		try {
			final Field xmlTypeField = targetType.getDeclaredField("type");
			final SchemaType xmlType = (SchemaType) xmlTypeField.get(null);
			final QName name = xmlType.getName();
			resTagName = name.getLocalPart();
		} catch (SecurityException exc) {
			throw new IllegalStateException(exc);
		} catch (NoSuchFieldException exc) {
			throw new IllegalStateException(exc);
		} catch (IllegalArgumentException exc) {
			throw new IllegalStateException(exc);
		} catch (IllegalAccessException exc) {
			throw new IllegalStateException(exc);
		}
		return resTagName;
	}

	private String m_lastVersionRead;

	/**
	 * Creates a new object which will use the default error management strategy
	 * {@link ErrorManagement#THROW}.
	 */
	public XMCDAReadUtils() {
		super();
		m_lastVersionRead = null;
	}

	/**
	 * Creates a new object delegating error management to the given error manager
	 * in case of unexpected data read.
	 *
	 * @param errorsManager not <code>null</code>.
	 */
	public XMCDAReadUtils(XMCDAErrorsManager errorsManager) {
		super(errorsManager);
		m_lastVersionRead = null;
	}

	/**
	 * Retrieves the version number of the last XMCDA document read by this object,
	 * as assessed by its namespace. This does not necessarily correspond to an
	 * effective version number as the document could contain an exotic namespace.
	 *
	 * @return <code>null</code> iff no document has been read or the version number
	 *         could not be retrieved.
	 */
	public String getLastVersionRead() {
		return m_lastVersionRead;
	}

	/**
	 * <p>
	 * Retrieves the XMCDA document representing the requested sample. Ensures that
	 * it validates.
	 * </p>
	 * <p>
	 * The sample is searched for by requesting the given name prepended with
	 * {@value #SAMPLES_PACKAGE}. This requires that the sample files are reachable
	 * by the class loader for this class.
	 * </p>
	 *
	 * @param name not <code>null</code>.
	 * @return not <code>null</code>.
	 * @throws IOException  if an exception happens while opening or closing the
	 *                      given reader, or while parsing the source.
	 * @throws XmlException if an exception related to the contents of the source
	 *                      happens while parsing the source, including if the
	 *                      source document does not validate.
	 */
	public XMCDADoc getSample(String name) throws IOException, XmlException {
		checkNotNull(name);
		final ByteSource source = getSampleAsInputSupplier(name);
		try (InputStream input = source.openBufferedStream()) {
			final XMCDADoc doc = XMCDADoc.Factory.parse(input);
			if (!doc.validate()) {
				throw new XmlException("Input does not validate.");
			}
			return doc;
		}
	}

	/**
	 * <p>
	 * Retrieves an input supplier reading the requested sample.
	 * </p>
	 * <p>
	 * The sample is searched for by requesting the given name prepended with
	 * {@value #SAMPLES_PACKAGE}. This requires that the sample files are reachable
	 * by the class loader for this class.
	 * </p>
	 *
	 * @param name not <code>null</code>.
	 * @return not <code>null</code>.
	 */
	public ByteSource getSampleAsInputSupplier(String name) {
		checkNotNull(name);
		final String fullName = SAMPLES_PACKAGE + name;
		final URL url = getClass().getResource(fullName);
		if (url == null) {
			throw new IllegalArgumentException("Couldn't find resource " + fullName + ".");
		}
		final ByteSource source = Resources.asByteSource(url);
		return source;
	}

	/**
	 * <p>
	 * Retrieves the only element from the given collection of elements. This method
	 * expects the collection of elements to contain exactly one element.
	 * </p>
	 * <p>
	 * In case of unexpected contents, this method returns <code>null</code> or
	 * throws an exception depending on the strategy this object follows.
	 * </p>
	 *
	 * @param                       <T> the type of content of the collection.
	 * @param collection            not <code>null</code>.
	 * @param contextMessageIfEmpty if not <code>null</code>, is used to give a more
	 *                              useful error message about the context when the
	 *                              given collection is empty. If parsing an
	 *                              {@link XmlObject}, one possibility is to use the
	 *                              qualified name of the parsed type, see e.g.
	 *                              {@link XPerformanceTable#type} and
	 *                              {@link SchemaType#getName()}.
	 * @return <code>null</code> iff the collection contains more or less than one
	 *         element and this object follows a permissive strategy.
	 * @throws InvalidInputException iff the collection contains more or less than
	 *                               one element and this object follows the
	 *                               {@link ErrorManagement#THROW} strategy.
	 */
	public <T extends XmlTokenSource> T getUnique(Collection<T> collection, String contextMessageIfEmpty)
			throws InvalidInputException {
		return getUnique(collection, false, contextMessageIfEmpty);
	}

	/**
	 * <p>
	 * Retrieves the only element from the given collection of elements, or
	 * <code>null</code> if the collection is empty. This method expects the
	 * collection of elements to contain zero or one elements, thus maximum one
	 * element.
	 * </p>
	 * <p>
	 * If the given collection contains more than one element, this method returns
	 * <code>null</code> or throws an exception depending on the strategy this
	 * object follows.
	 * </p>
	 *
	 * @param            <T> the type of content of the collection.
	 * @param collection not <code>null</code>.
	 * @return <code>null</code> if the collection contains zero elements, or if the
	 *         given collection contains more than one element and this object
	 *         follows a permissive strategy.
	 * @throws InvalidInputException iff the given collection contains more than one
	 *                               element and this object follows the
	 *                               {@link ErrorManagement#THROW} strategy.
	 */
	public <T> T getUniqueOrZero(Collection<T> collection) throws InvalidInputException {
		return getUnique(collection, true, null);
	}

	/**
	 * <p>
	 * Retrieves the XMCDA document from the given source. Ensures that it
	 * validates.
	 * </p>
	 * <p>
	 * If the document contained in the given source appears, according to its
	 * namespace, to be an XMCDA document not matching the expected version
	 * {@link #DEFAULT_XMCDA_VERSION}, this method attemps to proceed as if the
	 * source version matched the expected one.
	 * </p>
	 * <p>
	 * The underlying reader is closed when this method returns.
	 * </p>
	 *
	 * @param source not <code>null</code>, with a non <code>null</code> reader.
	 * @return not <code>null</code>.
	 * @throws IOException  if an exception happens while opening or closing the
	 *                      given reader, or while parsing the source.
	 * @throws XmlException if an exception related to the contents of the source
	 *                      happens while parsing the source, including if the
	 *                      source document does not validate.
	 * @see #getLastVersionRead
	 */
	public XMCDA getXMCDA(ByteSource source) throws IOException, XmlException {
		return getXMCDADoc(source).getXMCDA();
	}

	/**
	 * <p>
	 * Retrieves the XMCDA document from the given source. Ensures that it
	 * validates.
	 * </p>
	 * <p>
	 * If the document contained in the given source appears, according to its
	 * namespace, to be an XMCDA document not matching the expected version
	 * {@link #DEFAULT_XMCDA_VERSION}, this method attemps to proceed as if the
	 * source version matched the expected one.
	 * </p>
	 * <p>
	 * The underlying reader is closed when this method returns.
	 * </p>
	 *
	 * @param source not <code>null</code>, with a non <code>null</code> reader.
	 * @return not <code>null</code>.
	 * @throws IOException  if an exception happens while opening or closing the
	 *                      given reader, or while parsing the source.
	 * @throws XmlException if an exception related to the contents of the source
	 *                      happens while parsing the source, including if the
	 *                      source document does not validate.
	 * @see #getLastVersionRead
	 */
	public XMCDADoc getXMCDADoc(ByteSource source) throws IOException, XmlException {
		checkNotNull(source);
		final ByteSource effectiveSource;
		final String sourceNamespace = XmlReadUtils.getNamespace(source);
		if (sourceNamespace != null && sourceNamespace.startsWith(XMCDA_NAMESPACE_PREFIX)) {
			m_lastVersionRead = sourceNamespace.substring(XMCDA_NAMESPACE_PREFIX.length());
			LOGGER.info("Version read: " + m_lastVersionRead + ".");
			if (!sourceNamespace.equals(XMCDA_NAMESPACE_PREFIX + DEFAULT_XMCDA_VERSION)) {
				effectiveSource = getAsVersion(source, DEFAULT_XMCDA_VERSION);
			} else {
				effectiveSource = source;
			}
		} else {
			effectiveSource = source;
		}
		try (InputStream input = effectiveSource.openBufferedStream()) {
			final XMCDADoc doc = XMCDADoc.Factory.parse(input);
			if (!doc.validate()) {
				throw new XmlException("Input does not validate.");
			}
			return doc;
		}
	}

	/**
	 * <p>
	 * Retrieves the only element from the given collection of elements, or
	 * <code>null</code> if the collection is empty. This method expects the
	 * collection of elements to contain zero or one elements, thus maximum one
	 * element, if zero is accepted; otherwise it expects the given collection to
	 * contain exactly one element.
	 * </p>
	 * <p>
	 * In case of unexpected contents, this method returns <code>null</code> or
	 * throws an exception depending on the strategy this object follows.
	 * </p>
	 *
	 * @param                      <T> the type of content of the collection.
	 * @param collection           not <code>null</code>.
	 * @param acceptZero           <code>true</code> to accept an empty collection
	 *                             as a non erroneous case.
	 * @param contextMessageIfZero may be <code>null</code>. If zero is not allowed,
	 *                             this will be used to give a more useful error
	 *                             message about the context where the empty
	 *                             collection was found.
	 * @return <code>null</code> if the collection contains zero elements and this
	 *         is an acceptable situation according to the relevant parameter, or in
	 *         case of unexpected contents if this object follows a permissive
	 *         strategy.
	 * @throws InvalidInputException if unexpected data is read and this object
	 *                               follows the {@link ErrorManagement#THROW}
	 *                               strategy.
	 */
	private <T> T getUnique(Collection<T> collection, boolean acceptZero, final String contextMessageIfZero)
			throws InvalidInputException {
		Preconditions.checkNotNull(collection);
		final String expected = acceptZero ? "zero or one" : "one";
		final T unique;
		if (collection.size() > 1) {
			error("Found more than one element at " + collection + ", expected " + expected + ".");
			unique = null;
		} else if (collection.size() == 1) {
			unique = Iterables.getOnlyElement(collection);
		} else {
			if (!acceptZero) {
				final String printContext = contextMessageIfZero == null ? "in collection"
						: "at " + contextMessageIfZero;
				error("Found zero elements " + printContext + ", expected " + expected + ".");
			}
			unique = null;
		}
		return unique;
	}
}
