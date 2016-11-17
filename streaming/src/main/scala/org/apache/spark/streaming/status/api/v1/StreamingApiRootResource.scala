/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.streaming.status.api.v1

import javax.servlet.ServletContext
import javax.ws.rs.Path
import javax.ws.rs.core.Context

import com.sun.jersey.spi.container.servlet.ServletContainer
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder

import org.apache.spark.status.api.v1.UIRoot
import org.apache.spark.streaming.ui.StreamingJobProgressListener

@Path("/v1")
private[v1] class StreamingApiRootResource extends UIRootFromServletContext{

  @Path("streaminginfo")
  def getStreamingInfo(): StreamingInfoResource = {
    new StreamingInfoResource(uiRoot, listener)
  }

  @Path("statistics")
  def getStreamingStatistics(): StreamingStatisticsResource = {
    new StreamingStatisticsResource(uiRoot, listener, startTimeMillis)
  }

  @Path("receivers")
  def getReceivers(): AllReceiversResource = {
    new AllReceiversResource(listener)
  }

}

private[spark] object StreamingApiRootResource {

  def getServletHandler(
    uiRoot: UIRoot,
    listener: StreamingJobProgressListener,
    startTimeMillis: Long
  ): ServletContextHandler = {

    val jerseyContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS)
    jerseyContext.setContextPath("/streaming/api")
    val holder: ServletHolder = new ServletHolder(classOf[ServletContainer])
    holder.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
      "com.sun.jersey.api.core.PackagesResourceConfig")
    holder.setInitParameter("com.sun.jersey.config.property.packages",
      "org.apache.spark.streaming.status.api.v1")
    // holder.setInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS,
    // classOf[SecurityFilter].getCanonicalName)
    UIRootFromServletContext.setUiRoot(jerseyContext, uiRoot)
    UIRootFromServletContext.setListener(jerseyContext, listener)
    UIRootFromServletContext.setStartTimeMillis(jerseyContext, startTimeMillis)
    jerseyContext.addServlet(holder, "/*")
    jerseyContext
  }
}

private[v1] object UIRootFromServletContext {

  private val attribute = getClass.getCanonicalName

  def setListener(contextHandler: ContextHandler, listener: StreamingJobProgressListener): Unit = {
    contextHandler.setAttribute(attribute + "_listener", listener)
  }
  
  def getListener(context: ServletContext): StreamingJobProgressListener = {
    context.getAttribute(attribute + "_listener").asInstanceOf[StreamingJobProgressListener]
  }

  def setStartTimeMillis(contextHandler: ContextHandler, time: Long): Unit = {
    contextHandler.setAttribute(attribute + "_startTimeMillis", time)
  }

  def getStartTimeMillis(context: ServletContext): Long = {
    context.getAttribute(attribute + "_startTimeMillis").asInstanceOf[Long]
  }
  
  def setUiRoot(contextHandler: ContextHandler, uiRoot: UIRoot): Unit = {
    contextHandler.setAttribute(attribute, uiRoot)
  }

  def getUiRoot(context: ServletContext): UIRoot = {
    context.getAttribute(attribute).asInstanceOf[UIRoot]
  }
}

private[v1] trait UIRootFromServletContext {
  @Context
  var servletContext: ServletContext = _

  def uiRoot: UIRoot = UIRootFromServletContext.getUiRoot(servletContext)
  def listener: StreamingJobProgressListener = UIRootFromServletContext.getListener(servletContext)
  def startTimeMillis: Long = UIRootFromServletContext.getStartTimeMillis(servletContext)
}
