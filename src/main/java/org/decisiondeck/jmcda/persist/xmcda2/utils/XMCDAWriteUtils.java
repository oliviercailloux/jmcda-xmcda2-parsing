package org.decisiondeck.jmcda.persist.xmcda2.utils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.decision_deck.utils.ByteArraysSupplier;
import org.decision_deck.utils.StringUtils;
import org.decision_deck.utils.persist.XmlWriteUtils;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;

public class XMCDAWriteUtils {

	private static final Logger s_logger = LoggerFactory.getLogger(XMCDAWriteUtils.class);

	static public void appendTo(Collection<? extends XmlObject> xFragments, XMCDA xmcda, boolean validate) {
		checkNotNull(xFragments);
		checkNotNull(xmcda);
		for (XmlObject fragment : xFragments) {
			appendTo(fragment, xmcda, validate);
		}
	}

	static public void appendTo(XmlObject fragment, XMCDA xmcda, boolean validate) {
		checkNotNull(fragment);
		checkNotNull(xmcda);
		final Document docNode = xmcda.getDomNode().getOwnerDocument();
		s_logger.info("Getting dom node.");
		final Node domNode = fragment.getDomNode();
		s_logger.info("Importing node.");
		final Node importedFragment = docNode.importNode(domNode, true);
		s_logger.info("Imported node.");
		xmcda.getDomNode().appendChild(importedFragment);
		s_logger.info("Appened child.");
		if (validate) {
			checkState(xmcda.validate(), "Resulting document does not validate after appending " + fragment + ".");
		}
	}

	/**
	 * Returns a new XMCDA document containing only the given fragment.
	 *
	 * @param fragment
	 *            not <code>null</code>, must conform to the relevant schema,
	 *            must be allowed as a direct child of the XMCDA tag.
	 * @param validate
	 *            if <code>true</code>, this method will assert that the
	 *            returned document is valid. This is recommanded.
	 * @return not <code>null</code>.
	 */
	static public XMCDADoc getDoc(XmlObject fragment, boolean validate) {
		checkNotNull(fragment);
		final XMCDADoc doc = XMCDADoc.Factory.newInstance();
		final XMCDA xmcda = doc.addNewXMCDA();
		appendTo(fragment, xmcda, validate);

		if (validate) {
			assert (doc.validate());
		}

		return doc;
	}

	/**
	 * Writes an XMCDA document containing only the given fragment to the given
	 * destination. If validate is <code>true</code>, the fragment must be valid
	 * and the written document is guaranteed to be valid.
	 *
	 * @param fragment
	 *            not <code>null</code>, must conform to the relevant schema,
	 *            must be allowed as a direct child of the XMCDA tag.
	 * @param destination
	 *            not <code>null</code>.
	 * @param validate
	 *            if <code>true</code>, this method will assert that the written
	 *            document is valid. This is recommanded.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             writer, or while writing to the destination.
	 */
	static public void write(XmlObject fragment, ByteSink destination, boolean validate) throws IOException {
		final XMCDAWriteUtils writer = new XMCDAWriteUtils();
		writer.setValidate(validate);
		s_logger.debug("Obtaining document for fragment: {}.", fragment);
		final XMCDADoc doc = writer.getDoc(fragment);
		s_logger.debug("Obtained document for fragment.");
		writer.write(doc, destination);
		s_logger.debug("Written document: {}.", doc);
	}

	private final XmlWriteUtils m_helper = new XmlWriteUtils();

	public XMCDAWriteUtils() {
		// nothing
	}

	public void appendTo(Collection<? extends XmlObject> xFragments, XMCDA xmcda) {
		appendTo(xFragments, xmcda, m_helper.doesValidate());
	}

	public void appendTo(XmlObject fragment, XMCDA xmcda) {
		appendTo(fragment, xmcda, m_helper.doesValidate());
	}

	/**
	 * Retrieves the information whether this object only accepts to write valid
	 * documents. The default is <code>true</code>.
	 *
	 * @return <code>true</code> if this object validates documents before
	 *         writing them.
	 */
	public boolean doesValidate() {
		return m_helper.doesValidate();
	}

	/**
	 * Returns a new XMCDA document containing only the given fragment. The
	 * fragment must be valid, and the document is guaranteed to be valid,
	 * except if this object is not set to validate xml.
	 *
	 * @param fragment
	 *            not <code>null</code>, must conform to the relevant schema,
	 *            must be allowed as a direct child of the XMCDA tag.
	 * @return not <code>null</code>.
	 * @see #setValidate(boolean)
	 */
	public XMCDADoc getDoc(XmlObject fragment) {
		return getDoc(fragment, m_helper.doesValidate());
	}

	/**
	 * Retrieves a writable view of the options used to save XML streams.
	 * Default options are to use pretty print and to use the UTF-8 encoding.
	 *
	 * @return not <code>null</code>.
	 */
	public XmlOptions getSaveOptions() {
		return m_helper.getSaveOptions();
	}

	/**
	 * Enables or disables the check for validation before writing any document.
	 * The default is <code>true</code>, thus this object validates each
	 * document before returning or writing them. It is not recommanded to
	 * disable validation but it can be useful for debug.
	 *
	 * @param validate
	 *            <code>false</code> to allow invalid documents.
	 */
	public void setValidate(boolean validate) {
		m_helper.setValidate(validate);
	}

	/**
	 * Writes the given XMCDA document to the given destination. The document
	 * must be valid, except if this object is specifically set to not validate
	 * documents.
	 *
	 * @param doc
	 *            not <code>null</code>, must conform to the XMCDA schema.
	 * @param destination
	 *            not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             writer, or while writing to the destination.
	 */
	public void write(XMCDADoc doc, ByteSink destination) throws IOException {
		checkNotNull(destination);
		checkNotNull(doc);
		m_helper.write(doc, destination);
	}

	/**
	 * Writes the given XMCDA document to the given destination. The document
	 * must be valid, except if this object is specifically set to not validate
	 * documents.
	 *
	 * @param doc
	 *            not <code>null</code>, must conform to the XMCDA schema.
	 * @param destination
	 *            not <code>null</code>.
	 * @param versionToWrite
	 *            <code>null</code> for
	 *            {@value XMCDAReadUtils#DEFAULT_XMCDA_VERSION}.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             writer, or while writing to the destination.
	 * @see #setValidate(boolean)
	 */
	public void write(XMCDADoc doc, final ByteSink destination, String versionToWrite) throws IOException {
		checkNotNull(doc);
		checkNotNull(destination);

		if (versionToWrite == null || versionToWrite.equals(XMCDAReadUtils.DEFAULT_XMCDA_VERSION)) {
			write(doc, destination);
		} else {
			final ByteArraysSupplier supplier = StringUtils.newByteArraysSupplier();
			write(doc, supplier);
			final ByteArrayOutputStream writtenDefaultVersion = Iterables.getOnlyElement(supplier.getArrays());

			final ByteSource outVersion;
			try {
				outVersion = XMCDAReadUtils.getAsVersion(ByteSource.wrap(writtenDefaultVersion.toByteArray()),
						versionToWrite);
			} catch (XmlException exc) {
				throw new IllegalStateException("Couldn't parse again document just written.");
			}
			outVersion.copyTo(destination);
		}
	}

	/**
	 * Writes an XMCDA document containing only the given fragment to the given
	 * destination. The fragment must be valid, except if this object is
	 * specifically set to not validate documents.
	 *
	 * @param fragment
	 *            not <code>null</code>, must conform to the relevant schema,
	 *            must be allowed as a direct child of the XMCDA tag.
	 * @param destination
	 *            not <code>null</code>.
	 * @throws IOException
	 *             if an exception happens while opening or closing the given
	 *             writer, or while writing to the destination.
	 */
	public void write(XmlObject fragment, ByteSink destination) throws IOException {
		final XMCDADoc doc = getDoc(fragment);
		write(doc, destination);
	}

}
