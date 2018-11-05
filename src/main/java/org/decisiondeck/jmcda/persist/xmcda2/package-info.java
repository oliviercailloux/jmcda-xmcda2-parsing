/**
 *
 *  <p>Classes to export to and import from <a href="http://www.decision-deck.org/xmcda/index.html">XMCDA documents</a>.</p>
 *  
 *  <p>Vocabulary notes: a part of an XMCDA document is called a fragment. Methods reading data from XMCDA fragments 
 *  may specify that they expect the fragment to conform to some specific structure. This is necessarily compatible with, and 
 *  more restrictive than, the XMCDA standard. For example, a method may say that a fragment is 
 *  expected to contain only numbers as values (thus not labels or anything else). When such expectation is not 
 *  satisfied, i.e. in our example, if the fragment contains a value that is not a number, the reader class will 
 *  act according to one of several possible error management strategies. The default strategy is to throw an 
 *  {@link org.decisiondeck.jmcda.exc.InvalidInputException}. The exception comes with a message that will explain briefly which
 *   unexpected situation was met. 
 *  Throwing an exception stops the reading, thus other possible errors occuring in other places in the fragment are omitted
 *   from the message. The user may command the reader object to use other strategies to avoid stopping reading at the first
 *    error: log all errors, or collect all errors. In the latter case, the object may (and should) be queried to retrieve 
 *    all error messages after the read.</p>
 *    <p>When the classes documentation refers to alternatives, it generally designates alternatives and profiles,
 *     as profiles are represented in XMCDA
 * as fictive alternatives. The class {@link org.decisiondeck.jmcda.persist.xmcda2.XMCDAAlternatives} contains
 *  specific methods to discriminate real alternatives and profiles.</p>
 *  
 *  @author Olivier Cailloux
 */
package org.decisiondeck.jmcda.persist.xmcda2;