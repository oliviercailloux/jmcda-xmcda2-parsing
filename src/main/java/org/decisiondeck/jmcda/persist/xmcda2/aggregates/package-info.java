/**
 *
 *  <p>High level classes to export aggregate structures to, or import from, <a href="http://www.decision-deck.org/xmcda/index.html">XMCDA documents</a>.</p>
 *  
 *  <p>This package contain reader classes designed to read complete structures from complete XMCDA documents,
 *   contrary to the lower level classes dedicated to reading and writing XMCDA fragments.</p> 
 * <p>
 * XMCDA reader objects read from so-called sources, which are InputSupplier of {@link java.io.Reader} objects. Such objects can
 * be obtained using Guava's classes, e.g. with {@link com.google.common.io.Files#newReaderSupplier}, {@link com.google.common.io.Resources#newReaderSupplier}.
 * The good point with using InputSuppliers instead of Readers is that the user does not have to manage the opening and
 * closing of the streams: the XMCDA reader takes care of this.
 * </p>
 * <p>
 * The problem may be defined in a single source, called the main source, representing a single XMCDA document from
 * which everything is read. Alternatively, dedicated sources may be used for each of for some type of object to read,
 * using the relevant setSource methods. XMCDA reader objects will default to the main source if asked to read a type
 * for which a dedicated source has not been defined.
 * </p>
 * As with classes reading XMCDA fragments, reader classes from this package expect the XMCDA documents to comply to some grammar.
 *  When not satisfied, for example, if the document contains a value that is not a number at some place where a number was expected,
 *   the reader class will 
 *  act according to one of several possible error management strategies. The default strategy is to throw an 
 *  {@link org.decisiondeck.jmcda.exc.InvalidInputException}. The exception comes with a message that will explain briefly which
 *   unexpected situation was met. 
 *  Throwing an exception stops the reading, thus other possible errors occuring in other places in the document are omitted
 *   from the message. The user may command the reader object to use other strategies to avoid stopping reading at the first
 *    error: log all errors, or collect all errors. In the latter case, the object may (and should) be queried to retrieve 
 *    all error messages after the read.</p>
 *  <p>Reader objects read only once from the relevant source for each type of requested object, if the reading does not throw an exception.
 *  Second calls to the same read method, supposing the previous call was successful, will return the same object, 
 *  or a new copy of the same object. Reading only once from the source permits to avoid stacking several times the same error message,
 *  supposing the error management strategy is {@link org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement#COLLECT}
 *   or {@link org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement#LOG}. Changing a dedicated source after the corresponding object type
 *  has been read is not allowed and will throw a runtime exception.</p>
 *  <p>If an exception happens during a read, the state of the reader is not modified by the read and the next read will try again from scratch.
 *  Changing the dedicated or the main source is allowed before re-trying a failed read.</p>
 *  <p>Unless indicated otherwise, read methods return immutable objects or defensive copies of underlying objects. 
 *  Even if they constitute new copies, this should be 
 *  considered an implementation detail and the objects be treated as immutable.</p><p>Reader classes will avoid reading twice the same information when possible
 *  by caching information the first time it is read. Asking for the same bit of information twice without changing the sources in between will result
 *  in only one read. The second time, the cached object will be returned.</p><p>The intended usage of these reader classes
 *  is to <em>first</em> set up all relevant sources, then start reading. Changing some source after some read has occurred will clear some part of
 *  the cache and possibly result in a re-read of some information, even of a non-related source. It is guaranteed that at least any cached information pertaining 
 *  to a given source is cleared when that source is changed, but more cached information than only these could be cleared. Exactly which part of the cache is cleared when
 *  some source is set is undocumented and subject to change.</p>
 *  
 *  @author Olivier Cailloux
 */
package org.decisiondeck.jmcda.persist.xmcda2.aggregates;

