/*
 * Copyright (c) 2000 jPOS.org.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *    "This product includes software developed by the jPOS project
 *    (http://www.jpos.org/)". Alternately, this acknowledgment may
 *    appear in the software itself, if and wherever such third-party
 *    acknowledgments normally appear.
 *
 * 4. The names "jPOS" and "jPOS.org" must not be used to endorse
 *    or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    license@jpos.org.
 *
 * 5. Products derived from this software may not be called "jPOS",
 *    nor may "jPOS" appear in their name, without prior written
 *    permission of the jPOS project.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE JPOS PROJECT OR ITS CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the jPOS Project.  For more
 * information please see <http://www.jpos.org/>.
 */
package org.jpos.q2.qbean;

import org.jpos.q2.Q2;
import org.jpos.q2.QBean;
import org.jpos.q2.QBeanSupport;
import org.jpos.q2.QPersist;
import org.jdom.Element;
import org.jpos.util.Log;
import org.jpos.util.Logger;

import javax.management.*;

/**
 * @author <a href="mailto:taherkordy@dpi2.dpi.net.ir">Alireza Taherkordi</a>
 *
 * <http-adaptor class="org.jpos.q2.qbean.HttpAdaptor"
 *                       name="http-adaptor" >
 *  <attr name="host">localhost</attr>
 *  <attr name="port" type="java.lang.Integer">8082</attr>
 * </http-adaptor>
 *
 * set host property to "localhost" if you want to can't access the server
 * from another computer,This is good for security reasons.
 *
 * @version $Revision$ $Date$
 */
public class HttpAdaptor
            extends mx4j.adaptor.http.HttpAdaptor
        implements  HttpAdaptorMBean , QBean, QPersist
{
    Element persist;
    int state;
    Q2 server;
    String name, loggerName, user, password;
    boolean modified;
    ObjectName processorName;
    Log log;

    public HttpAdaptor () {
        super();
        state = -1;
    }
    public void setServer (Q2 server) {
        this.server = server;
    }
    public Q2 getServer () {
        return server;
    }
    public void setName (String name) {
        this.name = name;
        setModified (true);
    }
    public String getName () {
        return name;
    }
    public void setUser (String user) {
        this.user = user;
    }
    public String getUser () {
        return user;
    }
    public void setPassword (String password) {
        this.password = password;
    }
    public void setLogger (String loggerName) {
        this.loggerName = loggerName;
        log = new Log (Logger.getLogger (loggerName), getName ());
        setModified (true);
    }

    public String getLogger () 
    {
        return log.getLogger().getName();
    }

    public void init ()
    {
        if (state != -1)
            return;
        try {
            setModified (false);
            processorName = new ObjectName("MX4J:name=mx4j.adaptor.http.XSLTProcessor");
            mx4j.adaptor.http.XSLTProcessor processorMBean = new mx4j.adaptor.http.XSLTProcessor();
            getServer().getMBeanServer().registerMBean(processorMBean,processorName);
            setProcessorName(processorName);
            if (user != null && password != null) {
                addAuthorization(user, password);
                setAuthenticationMethod ("basic");
            }
            state = QBean.STOPPED;
        } catch (Exception e) {
            if (log != null)
                log.warn ("init", e);
        }

    }
    public void start()
    {
        if (state != QBean.DESTROYED &&
            state != QBean.STOPPED   &&
            state != QBean.FAILED)
           return;

        this.state = QBean.STARTING;

        try {
           super.start();
        } catch (Throwable t) {
           state = QBean.FAILED;
           t.printStackTrace();
           return;
        }
        state = QBean.STARTED;
    }
    public void stop ()
    {
        if (state != QBean.STARTED)
           return;
        state = QBean.STOPPING;
        try {
           super.stop();
        } catch (Throwable t) {
           state = QBean.FAILED;
           t.printStackTrace();
           return;
        }
        state = QBean.STOPPED;
    }
    public void destroy ()
    {
        if (state == QBean.DESTROYED)
           return;
        if (state != QBean.STOPPED)
           stop();

        try {
            getServer().getMBeanServer().unregisterMBean(processorName);
        } catch (Exception e) {
            if (log != null)
                log.warn ("destroy", e);
        }

        state = QBean.DESTROYED;
    }
    public int getState () {
        return state;
    }
    public String getStateAsString () {
        return QBeanSupport.stateString[state];
    }
    public void setState (int state) {
        this.state = state;
    }
    public void setPersist (Element persist) {
        this.persist = persist ;
    }

    private Element createAttr (String name, String value, String type) {
        Element e = new Element ("attr");
        e.setAttribute ("name", name);
        if (type != null)
            e.setAttribute ("type", type);
        e.setText (value);
        return e;
    }
    private Element createAttr (String name, String value) {
        return createAttr (name, value, null);
    }
    public synchronized Element getPersist () {
        setModified (false);
        Element e = new Element (persist.getName());
        Element classPath = persist.getChild ("classpath");
        if (classPath != null)
            e.addContent (classPath);
        e.setAttribute ("class", getClass().getName());
        if (!e.getName().equals (getName ()))
            e.setAttribute ("name", getName());
        if (loggerName != null)
            e.setAttribute ("logger", loggerName);
        e.addContent (
            createAttr ("host", getHost())
        );
        e.addContent (
            createAttr (
                "port", Integer.toString (getPort()), "java.lang.Integer")
        );
        if (user != null && password != null) {
            e.addContent (createAttr ("user", user));
            e.addContent (createAttr ("password", password));
        }
        return (persist = e);
    }
    public synchronized void setModified (boolean modified) {
        this.modified = modified;
    }
    public synchronized boolean isModified () {
        return modified;
    }
    protected boolean running () {
        return state == QBean.STARTING || state == QBean.STARTED;
    }
    public void setHost (String host) {
        setModified (true);
        super.setHost (host);
    }
    public void setPort (int port) {
        setModified (true);
        super.setPort (port);
    }
    public void shutdownQ2 () {
        getServer().shutdown ();
    }
}
